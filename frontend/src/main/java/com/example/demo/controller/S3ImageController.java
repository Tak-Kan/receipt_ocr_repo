package com.example.demo.controller;

import com.example.demo.service.S3FileStorageServiceImpl;
import com.example.demo.service.TempFileStoreService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.stereotype.Controller;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * S3 モード時のみ有効。従来 WebMvcConfig が /images/** を /app/uploads に割り当てていた代わりに、
 *  - /images/temp/**     … コンテナ内の一時ファイル（登録前の確認画面用）
 *  - /images/receipts/** … S3 から取得（バケットは非公開のまま）
 * をアプリ経由で配信する。
 */
@Controller
@ConditionalOnProperty(name = "storage.type", havingValue = "aws")
public class S3ImageController {

    private final S3FileStorageServiceImpl storage;
    private final TempFileStoreService tempStore;

    public S3ImageController(S3FileStorageServiceImpl storage, TempFileStoreService tempStore) {
        this.storage = storage;
        this.tempStore = tempStore;
    }

    /** 登録前の確認画面で表示する一時画像（コンテナ内）。 */
    @GetMapping("/images/temp/{filename:[A-Za-z0-9._-]+}")
    public ResponseEntity<Resource> temp(@PathVariable String filename) throws IOException {
        Path p;
        try {
            p = tempStore.resolve(TempFileStoreService.WEB_PREFIX + filename);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        if (!Files.isRegularFile(p)) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        // 修正前: headers.add(HttpHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff");
        // ⭕ 修正後: 文字列で直接指定
        headers.add("X-Content-Type-Options", "nosniff"); 

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(TempFileStoreService.contentTypeOf(filename)))
            .contentLength(Files.size(p))
            .header("X-Content-Type-Options", "nosniff")
            .cacheControl(CacheControl.noStore())
            .body(new FileSystemResource(p));
    }

    /** 登録済みの画像（S3）。 */
    @GetMapping("/images/receipts/**")
    public ResponseEntity<Resource> receipt(HttpServletRequest request, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String webPath = request.getRequestURI().substring(request.getContextPath().length());

        String key;
        try {
            key = S3FileStorageServiceImpl.toKey(webPath);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }

        // key = receipts/{userId}/{yyyy}/{MM}/{file}（5セグメント）→ 本人のみ（管理者は例外）
        // key = receipts/{file}（2セグメント）は NAS から移行した旧形式 → 所有者を判別できないためログイン済みなら許可
        String[] seg = key.split("/");
        if (seg.length == 5) {
            boolean owner = seg[1].equals(auth.getName());
            boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())); // 実際のロール名に合わせて変更
            if (!owner && !admin) {
                return ResponseEntity.notFound().build();
            }
        } else if (seg.length != 2) {
            return ResponseEntity.notFound().build();
        }

        try {
            ResponseInputStream<GetObjectResponse> in = storage.open(webPath);
            GetObjectResponse meta = in.response();
            MediaType type = meta.contentType() != null
                ? MediaType.parseMediaType(meta.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                .contentType(type)
                .contentLength(meta.contentLength())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
                .body(new InputStreamResource(in));
        } catch (NoSuchKeyException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
