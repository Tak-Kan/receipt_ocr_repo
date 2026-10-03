package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.regex.Pattern;
import java.util.UUID;

/**
 * S3 版の FileStorageService。
 * DB / 画面には従来どおり「/images/temp/xxx.jpg」「/images/receipts/xxx.jpg」を保持し、
 * S3 のキーは先頭の "/images/" を除いた "temp/xxx.jpg" "receipts/xxx.jpg" とする。
 */
@Slf4j
public class S3FileStorageServiceImpl implements FileStorageService {

    private final Path tempDir = Paths.get("/app/uploads/temp");
    private final Path receiptsDir = Paths.get("/app/uploads/receipts"); // ここがNASにマウントされている想定

    private static final String WEB_PREFIX = "/images/";
    private static final String RECEIPTS_PREFIX = "receipts/";
    private static final Pattern USER_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    // WebMvcConfigのルーティングに合わせたプレフィックス
    private static final String DB_PATH_PREFIX = "/images/";
    // Dockerコンテナ内のマウント先（NASの実体）
    private static final String LOCAL_BASE_DIR = "/app/uploads/";

    private final S3Client s3;
    private final String bucket;
    private final TempFileStoreService tempStore;
    private boolean _init = false;

    public S3FileStorageServiceImpl(S3Client s3, String bucket, TempFileStoreService tempStore) {
        this.s3 = s3;
        this.bucket = bucket;
        this.tempStore = tempStore;
    }

    /**
     * S3上に指定したキーのオブジェクトが存在するかチェックするメソッド
     */
    private boolean existsFileInS3(String keyName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(keyName)
                    .build();
            
            // オブジェクトのメタデータを取得（存在すれば例外が発生しない）
            s3.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            // ファイルが存在しない場合はこの例外がスローされる
            return false;
        }
    }

    @Override
    public String saveToTemp(MultipartFile file) {
        return tempStore.save(file);
    }

    @Override
    public String moveToReceipts(String tempImagePath, String userId, LocalDate date) {
        try {
            if (userId == null || !USER_ID.matcher(userId).matches()) {
                throw new IllegalArgumentException("S3 キーに使えないユーザー識別子です: " + userId);
            }
            Path src = tempStore.resolve(tempImagePath);
            if (!Files.isRegularFile(src)) {
                throw new IllegalStateException("一時ファイルが見つかりません（時間切れの可能性があります。再度アップロードしてください）");
            }
            String filename = src.getFileName().toString();
            String key = String.format("%s%s/%04d/%02d/%s",
                RECEIPTS_PREFIX, userId, date.getYear(), date.getMonthValue(), filename);

            // 1. 同一キーのファイルが既にS3に存在するかチェック
            boolean isFileExist = existsFileInS3(key);

            if (isFileExist) {
                // 既に存在する場合は上書きを防止するため処理をスキップ
                // System.out.println("【スキップ】S3上の「" + keyName + "」は既に存在するため、上書きを防止しました。");
                throw new IOException("【スキップ】S3上の「" + key + "」は既に存在するため、上書きを防止しました。");
            } else {
                // 存在しない場合のみ安全にアップロード
                //System.out.println("S3上に同名ファイルがありません。アップロードを開始します...");

                // S3 へアップロード（失敗した場合は例外 → 呼び出し側で DB 登録に進まない）
                s3.putObject(
                    PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(TempFileStoreService.contentTypeOf(filename))
                        .build(),
                    RequestBody.fromFile(src));

                // temp から receipts(AWS) へ移動
                //PutObjectResponse response = s3.putObject(putObjectRequest, Paths.get(sourcePath.toString()).toString());
                //System.out.println("【成功】アップロードが完了しました。 ETag: " + response.eTag());
            }
            tempStore.deleteQuietly(tempImagePath);

            // 本保存後のWeb用パスを返す
            return WEB_PREFIX + key;
        } catch (IOException e) {
            throw new RuntimeException("NASへのファイル転送に失敗しました", e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }
        if (filePath.startsWith(TempFileStoreService.WEB_PREFIX)) {
            tempStore.deleteQuietly(filePath);
            return;
        }
        String key = toKey(filePath);
        // S3 の deleteObject は存在しないキーでも成功扱い
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        log.info("S3オブジェクトを削除しました: {}", key);
    }

    /** 画像配信用（ImageController から使用）。 */
    public ResponseInputStream<GetObjectResponse> open(String webPath) {
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(toKey(webPath)).build());
    }

    /**
     * "/images/receipts/u1/2026/10/a.jpg" → "receipts/u1/2026/10/a.jpg"
     * receipts/ 配下以外・空セグメント・"." ".." ・使用不可文字は IllegalArgumentException。
     */
    public static String toKey(String webPath) {
        if (webPath == null || !webPath.startsWith(WEB_PREFIX + RECEIPTS_PREFIX)) {
            throw new IllegalArgumentException("不正な画像パスです: " + webPath);
        }
        String key = webPath.substring(WEB_PREFIX.length());
        for (String seg : key.split("/", -1)) {
            if (!SEGMENT.matcher(seg).matches() || seg.equals(".") || seg.equals("..")) {
                throw new IllegalArgumentException("不正な画像パスです: " + webPath);
            }
        }
        return key;
    }
}