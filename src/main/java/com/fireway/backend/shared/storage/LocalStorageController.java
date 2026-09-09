package com.fireway.backend.shared.storage;

import java.nio.file.NoSuchFileException;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fireway.backend.shared.exception.NotFoundException;
import com.fireway.backend.shared.exception.ValidationException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 로컬에서 S3 역할을 대신하는 엔드포인트. LocalStorageAdapter 가 발급한 URL 이 여기로 온다.
 * prod 에서는 뜨지 않는다 — 서버에서 이 경로가 살아 있으면 그 자체가 무인증 업로드 구멍이다.
 */
@RestController
@Profile("!prod")
public class LocalStorageController {

    private final LocalStorageAdapter storage;
    private final long maxUploadBytes;

    public LocalStorageController(LocalStorageAdapter storage, StorageProperties properties) {
        this.storage = storage;
        this.maxUploadBytes = properties.maxUploadBytes();
    }

    @PutMapping(LocalStorageAdapter.URL_PREFIX + "**")
    ResponseEntity<Void> put(HttpServletRequest request) throws Exception {
        byte[] body = request.getInputStream().readAllBytes();
        // 로컬 디스크를 채우지 않기 위한 방어일 뿐, prod 의 크기 정책이 아니다.
        // prod 는 presigned PUT 을 막을 수 없어서 올라온 뒤 confirmUpload() 가 거른다.
        if (body.length > maxUploadBytes) {
            throw new ValidationException("파일이 너무 큽니다: " + body.length + " bytes");
        }
        storage.write(keyOf(request), body);
        return ResponseEntity.ok().build();
    }

    @GetMapping(LocalStorageAdapter.URL_PREFIX + "**")
    ResponseEntity<byte[]> get(HttpServletRequest request) {
        String key = keyOf(request);
        try {
            return ResponseEntity.ok(storage.read(key));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof NoSuchFileException) {
                throw new NotFoundException("파일이 없습니다: " + key);
            }
            throw e;
        }
    }

    private String keyOf(HttpServletRequest request) {
        return request.getRequestURI().substring(LocalStorageAdapter.URL_PREFIX.length());
    }
}
