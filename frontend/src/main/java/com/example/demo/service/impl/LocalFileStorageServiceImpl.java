package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalFileStorageServiceImpl implements FileStorageService {

    // WebMvcConfigのルーティングに合わせたプレフィックス
    private static final String DB_PATH_PREFIX = "/images/";
    private static final String RECEIPTS_PATH_PREFIX = "/images/receipts/";
    // Dockerコンテナ内のマウント先（NASの実体）
    private static final String LOCAL_BASE_DIR = "/app/uploads";
    // ファイル名の最大長（長すぎる元ファイル名は、拡張子が残るよう末尾を優先して切り詰める）
    private static final int MAX_FILE_NAME_LENGTH = 100;

    // すべて、正規化（normalize）済みの絶対パスで持つ。「この配下か？」の判定に使うため。
    private final Path baseDir;
    private final Path tempDir;
    private final Path receiptsDir; // ここがNASにマウントされている想定

    public LocalFileStorageServiceImpl() {
        this(Paths.get(LOCAL_BASE_DIR));
    }

    /** 保存先のルートを指定する（テスト用）。 */
    public LocalFileStorageServiceImpl(Path baseDir) {
        this.baseDir = baseDir.toAbsolutePath().normalize();
        this.tempDir = this.baseDir.resolve("temp");
        this.receiptsDir = this.baseDir.resolve("receipts");
    }

    @Override
    public String saveToTemp(MultipartFile file) {
        try {
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            // ファイル名の重複を防ぐためUUIDを付与。元のファイル名はユーザー入力なので、無害化してから使う
            String fileName = UUID.randomUUID() + "_" + sanitizeFileName(file.getOriginalFilename());
            Path targetPath = resolveDirectChild(tempDir, fileName);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath);
            }

            // 画面表示やDB格納用の「Webから見たパス」を返す
            return TEMP_PATH_PREFIX + fileName;
        } catch (IOException e) {
            throw new RuntimeException("一時フォルダへの保存に失敗しました", e);
        }
    }

    /**
     * userId / date は S3 版でキー（receipts/{userId}/{yyyy}/{MM}/...）を作るために使う引数。
     * ローカル（NAS）版は従来どおり receipts 直下に保存するため、使用しない。
     */
    @Override
    public String moveToReceipts(String tempImagePath, String userId, LocalDate date) {
        // 一時フォルダ（/images/temp/）のファイル以外は移動させない
        if (tempImagePath == null || !tempImagePath.startsWith(TEMP_PATH_PREFIX)) {
            throw new IllegalArgumentException("一時フォルダ以外のパスは移動できません: " + tempImagePath);
        }
        try {
            // Web用のパス（/images/temp/xxx.jpg）から、実際のファイル名（xxx.jpg）を抽出
            String fileName = tempImagePath.substring(TEMP_PATH_PREFIX.length());

            Path sourcePath = resolveDirectChild(tempDir, fileName);
            Path targetPath = resolveDirectChild(receiptsDir, fileName);

            if (!Files.exists(receiptsDir)) {
                Files.createDirectories(receiptsDir);
            }

            // temp から receipts(NAS) へ移動
            Files.move(sourcePath, targetPath);

            // 本保存後のWeb用パスを返す
            return RECEIPTS_PATH_PREFIX + fileName;
        } catch (IOException e) {
            throw new RuntimeException("NASへのファイル転送に失敗しました", e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }

        // DBのパス "/images/xxx.jpg" をコンテナの物理パス "/app/uploads/xxx.jpg" に変換
        Path physicalPath = toPhysicalPath(filePath);
        if (physicalPath == null) {
            // DBに不正な値（"../" を含むなど）が入っていても、保存先の外のファイルは絶対に消さない
            log.warn("保存先の外を指すパスのため、ファイルの削除をスキップしました: {}", filePath);
            return;
        }

        try {
            // ファイルが存在する場合のみ削除（存在しない場合は何もしない安全なメソッド）
            boolean deleted = Files.deleteIfExists(physicalPath);
            if (deleted) {
                log.info("ローカルファイルを削除しました: {}", physicalPath);
            }
        } catch (IOException e) {
            // 権限エラーや使用中ロックなどで消せない場合は例外を投げるか、ログのみ残す
            log.error("ローカルファイルの削除に失敗しました: {}", physicalPath, e);
            throw new RuntimeException("ファイルの削除に失敗しました", e);
        }
    }

    /**
     * Web用パス（/images/...）を物理パスに変換する。
     * 変換後のパスが保存先のルート配下に収まらない場合（"../" や絶対パスを使った抜け道）は null を返す。
     */
    private Path toPhysicalPath(String webPath) {
        if (!webPath.startsWith(DB_PATH_PREFIX)) {
            return null;
        }
        Path resolved = baseDir.resolve(webPath.substring(DB_PATH_PREFIX.length())).normalize();
        if (!resolved.startsWith(baseDir) || resolved.equals(baseDir)) {
            return null;
        }
        return resolved;
    }

    /**
     * dir 直下のファイルとして解決したパスを返す。
     * ファイル名にディレクトリ区切りや ".." が含まれて、dir の外や下位フォルダを指す場合は例外にする。
     */
    private static Path resolveDirectChild(Path dir, String fileName) {
        if (fileName == null || fileName.isEmpty()
                || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("不正なファイル名です: " + fileName);
        }
        Path resolved = dir.resolve(fileName).normalize();
        if (!dir.equals(resolved.getParent())) {
            throw new IllegalArgumentException("不正なファイル名です: " + fileName);
        }
        return resolved;
    }

    /**
     * アップロードされた元のファイル名から、パスとして危険な部分を取り除く。
     * "../../x.jpg" や "C:\dir\x.jpg" のようにフォルダ指定が付いていても、最後のファイル名部分だけを使う。
     */
    static String sanitizeFileName(String originalFileName) {
        if (originalFileName == null) {
            return "file";
        }
        String name = originalFileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        // 制御文字と、ファイルシステムやURLで問題になる文字を置き換える
        name = name.replaceAll("[\\p{Cntrl}:*?\"<>|#%]", "_").trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            return "file";
        }
        if (name.length() > MAX_FILE_NAME_LENGTH) {
            name = name.substring(name.length() - MAX_FILE_NAME_LENGTH);
        }
        return name;
    }
}
