package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * コンテナ内ローカルディスク上の一時保存領域（OCR 〜 登録確定までの間だけ使う）。
 * NAS マウント(/app/uploads)とは別の場所（既定 /tmp/receipt-temp）に置く。
 * コンテナ再作成で消えてよい前提。放置された一時ファイルは、アップロード時に maxAge 超過分を掃除する。
 */
public class TempFileStoreService {

    public static final String WEB_PREFIX = "/images/temp/";
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private final Path dir;
    private final Duration maxAge;

    public TempFileStoreService(Path dir, Duration maxAge) {
        this.dir = dir.toAbsolutePath().normalize();
        this.maxAge = maxAge;
    }

    /** 保存して "/images/temp/{uuid}.ext" を返す。元のファイル名は使わない。 */
    public String save(MultipartFile file) {
        try {
            Files.createDirectories(dir);
            purgeExpired();
            String name = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(name));
            }
            return WEB_PREFIX + name;
        } catch (IOException e) {
            throw new RuntimeException("一時フォルダへの保存に失敗しました", e);
        }
    }

    /** Web パス → 実ファイルパス。不正なパス（../ など）は IllegalArgumentException。 */
    public Path resolve(String webPath) {
        if (webPath == null || !webPath.startsWith(WEB_PREFIX)) {
            throw new IllegalArgumentException("不正な一時ファイルパスです: " + webPath);
        }
        String name = webPath.substring(WEB_PREFIX.length());
        if (!NAME.matcher(name).matches() || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("不正な一時ファイルパスです: " + webPath);
        }
        Path p = dir.resolve(name).normalize();
        if (!p.startsWith(dir)) {
            throw new IllegalArgumentException("不正な一時ファイルパスです: " + webPath);
        }
        return p;
    }

    public void deleteQuietly(String webPath) {
        try {
            Files.deleteIfExists(resolve(webPath));
        } catch (IOException | IllegalArgumentException ignored) {
            // 一時ファイルなので失敗しても致命的ではない（次回の purge で消える）
        }
    }

    private void purgeExpired() {
        Instant limit = Instant.now().minus(maxAge);
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(p -> {
                try {
                    if (Files.getLastModifiedTime(p).toInstant().isBefore(limit)) {
                        Files.deleteIfExists(p);
                    }
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    public static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String ext = filename.substring(dot).toLowerCase(Locale.ROOT);
        return ext.matches("\\.[a-z0-9]{1,5}") ? ext : "";
    }

    public static String contentTypeOf(String filename) {
        String ext = extensionOf(filename);
        switch (ext) {
            case ".jpg":
            case ".jpeg": return "image/jpeg";
            case ".png":  return "image/png";
            case ".gif":  return "image/gif";
            case ".webp": return "image/webp";
            case ".heic": return "image/heic";
            case ".pdf":  return "application/pdf";
            default:      return "application/octet-stream";
        }
    }
}
