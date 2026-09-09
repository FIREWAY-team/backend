package com.fireway.backend.shared.storage;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 업로드용 presigned URL 발급.
 *
 * ⚠️ 인증 계층은 두지 않는다(팀 결정). 다만 조회 API 와 성질이 다르다는 점은 알고 쓸 것 —
 *    경로·CCTV 조회는 남이 많이 불러도 CPU 를 좀 쓸 뿐이지만, 이 엔드포인트는 호출될 때마다
 *    '우리 버킷에 쓸 수 있는 티켓'이 하나씩 나간다. 그래서 상한을 세 겹으로 나눠 둔다:
 *
 *      1. Nginx rate limit — 이 경로에 IP 당 분당 5회 (인프라 쪽에서 설정)
 *      2. S3 수명주기 규칙 — uploads/ 접두사 30일 만료 (버킷 쪽에서 설정)
 *      3. FileUploadService.confirmUpload() — 올라온 뒤 크기 재검사, 초과분 삭제 (이 코드)
 *
 *    presigned PUT 은 크기를 막지 못하므로 3번만으로는 상한이 되지 않는다. 세 겹이 다 있어야 한다.
 */
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileUploadService service;

    public FileUploadController(FileUploadService service) {
        this.service = service;
    }

    @PostMapping("/upload-url")
    UploadUrlResponse issueUploadUrl(@RequestBody @Valid UploadUrlRequest request) {
        StoragePort.PresignedUpload upload = service.issueUploadUrl(request.contentType());
        return new UploadUrlResponse(upload.uploadUrl(), upload.key(), upload.expiresInSeconds());
    }

    // JSON 은 snake_case 다(spring.jackson.property-naming-strategy) — 요청은 content_type,
    // 응답은 upload_url · key · expires_in_seconds 로 나간다.
    public record UploadUrlRequest(@NotBlank String contentType) { }

    public record UploadUrlResponse(String uploadUrl, String key, long expiresInSeconds) { }
}
