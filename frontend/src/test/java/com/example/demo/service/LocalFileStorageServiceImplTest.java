package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageServiceImplTest {

    @TempDir
    Path root;

    private Path baseDir;
    private LocalFileStorageServiceImpl storage;

    // ローカル版は userId / date を使わないが、インターフェースの引数として渡す
    private static final String USER = "alice";
    private static final LocalDate DATE = LocalDate.of(2026, 10, 10);

    @BeforeEach
    void setUp() throws IOException {
        // baseDir（= /app/uploads の代わり）の外側に root を置き、「外へ出られないこと」を確認できるようにする
        baseDir = root.resolve("uploads");
        Files.createDirectories(baseDir);
        storage = new LocalFileStorageServiceImpl(baseDir);
    }

    private static MockMultipartFile image(String originalName) {
        return new MockMultipartFile("file", originalName, "image/jpeg", "dummy".getBytes(StandardCharsets.UTF_8));
    }

    // ---- saveToTemp ----

    @Test
    void saveToTemp_fileNameWithTraversal_isSavedInsideTempOnly() throws IOException {
        String webPath = storage.saveToTemp(image("../../evil.jpg"));

        assertTrue(webPath.startsWith(FileStorageService.TEMP_PATH_PREFIX));
        assertTrue(webPath.endsWith("_evil.jpg"));
        assertFalse(webPath.contains(".."));

        String fileName = webPath.substring(FileStorageService.TEMP_PATH_PREFIX.length());
        assertTrue(Files.exists(baseDir.resolve("temp").resolve(fileName)));
        assertFalse(Files.exists(root.resolve("evil.jpg")));
        assertFalse(Files.exists(baseDir.resolve("evil.jpg")));
    }

    @Test
    void sanitizeFileName_removesDirectoriesAndUnsafeCharacters() {
        assertEquals("file", LocalFileStorageServiceImpl.sanitizeFileName(null));
        assertEquals("file", LocalFileStorageServiceImpl.sanitizeFileName(".."));
        assertEquals("file", LocalFileStorageServiceImpl.sanitizeFileName(""));
        assertEquals("a.png", LocalFileStorageServiceImpl.sanitizeFileName("..\\..\\a.png"));
        assertEquals("a.png", LocalFileStorageServiceImpl.sanitizeFileName("C:\\dir\\a.png"));
        assertEquals("a_b_c.jpg", LocalFileStorageServiceImpl.sanitizeFileName("a#b%c.jpg"));
        assertEquals("レシート.jpg", LocalFileStorageServiceImpl.sanitizeFileName("レシート.jpg"));
    }

    @Test
    void sanitizeFileName_longName_keepsExtension() {
        String name = "a".repeat(300) + ".jpg";
        String sanitized = LocalFileStorageServiceImpl.sanitizeFileName(name);

        assertEquals(100, sanitized.length());
        assertTrue(sanitized.endsWith(".jpg"));
    }

    // ---- moveToReceipts ----

    @Test
    void moveToReceipts_movesTempFileToReceipts() {
        String tempPath = storage.saveToTemp(image("receipt.jpg"));

        String receiptPath = storage.moveToReceipts(tempPath, USER, DATE);

        assertTrue(receiptPath.startsWith("/images/receipts/"));
        String fileName = receiptPath.substring("/images/receipts/".length());
        assertTrue(Files.exists(baseDir.resolve("receipts").resolve(fileName)));
        assertFalse(Files.exists(baseDir.resolve("temp").resolve(fileName)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/images/receipts/a.jpg",            // temp 以外
            "/images/temp/../receipts/a.jpg",    // ".." で temp の外へ
            "/images/temp/sub/a.jpg",            // 下位フォルダ
            "/images/temp/..\\a.jpg",            // バックスラッシュ区切り
            "/images/temp/",                     // ファイル名なし
            "/etc/passwd"                        // /images/ 以外
    })
    void moveToReceipts_invalidPath_isRejected(String invalidPath) {
        assertThrows(IllegalArgumentException.class, () -> storage.moveToReceipts(invalidPath, USER, DATE));
    }

    @Test
    void moveToReceipts_null_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> storage.moveToReceipts(null, USER, DATE));
    }

    // ---- deleteFile ----

    @Test
    void deleteFile_deletesReceiptFile() {
        String receiptPath = storage.moveToReceipts(storage.saveToTemp(image("receipt.jpg")), USER, DATE);
        Path physical = baseDir.resolve("receipts")
                .resolve(receiptPath.substring("/images/receipts/".length()));
        assertTrue(Files.exists(physical));

        storage.deleteFile(receiptPath);

        assertFalse(Files.exists(physical));
    }

    @Test
    void deleteFile_pathOutsideBaseDir_doesNotDeleteAnything() throws IOException {
        Path outside = root.resolve("outside.txt");
        Files.writeString(outside, "must not be deleted");

        storage.deleteFile("/images/../outside.txt");        // ".." で保存先の外へ
        storage.deleteFile("/images//" + outside);            // 絶対パスで保存先の外へ
        storage.deleteFile("/images/receipts/../../outside.txt");
        storage.deleteFile(outside.toString());               // /images/ で始まらない
        storage.deleteFile("/images/");                       // 保存先のルートそのもの

        assertTrue(Files.exists(outside));
        assertTrue(Files.exists(baseDir));
    }

    @Test
    void deleteFile_blankOrNull_isIgnored() {
        storage.deleteFile(null);
        storage.deleteFile("  ");
    }
}
