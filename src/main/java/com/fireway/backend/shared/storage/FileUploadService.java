package com.fireway.backend.shared.storage;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fireway.backend.shared.exception.ValidationException;

/**
 * 업로드 정책을 한곳에 모은다 — 허용 타입, key 규칙, 만료, 크기 검증.
 * 어느 저장소(S3·로컬)를 쓰든 정책은 같아야 하므로 어댑터가 아니라 여기에 둔다.
 */
@Service
public class FileUploadService {

    /**
     * 허용 Content-Type. 서명에 들어가므로 클라이언트가 다른 타입으로는 올리지 못한다.
     *
     * ⚠️ 이건 '선언한 타입'만 고정할 뿐 '내용'을 검사하지 않는다. jpg 확장자를 단 실행 파일은
     * 여전히 통과한다. 브라우저가 이 파일을 원본 그대로 실행할 수 없게, 조회는 반드시
     * presigned GET 으로만 내보낸다(버킷 퍼블릭 차단 유지).
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StoragePort storage;
    private final StorageProperties properties;

    public FileUploadService(StoragePort storage, StorageProperties properties) {
        this.storage = storage;
        this.properties = properties;
    }

    public StoragePort.PresignedUpload issueUploadUrl(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ValidationException("허용되지 않는 파일 형식입니다: " + contentType);
        }
        // 저장 파일명에 사용자 입력을 쓰지 않는다. 날짜 접두사는 S3 라이프사이클 규칙을 걸거나
        // 사고 난 날의 업로드만 골라내야 할 때 쓴다.
        String key = "uploads/%s/%s".formatted(LocalDate.now(KST), UUID.randomUUID());
        return storage.presignUpload(key, contentType, properties.presignPutTtl());
    }

    public String issueDownloadUrl(String key) {
        return storage.presignDownload(key, properties.presignGetTtl());
    }

    /**
     * 프론트가 "다 올렸고 key 는 이거예요"라고 보고한 것을 실제로 확인한다.
     * 도메인이 key 를 DB 에 저장하기 전에 반드시 이걸 통과시킬 것 — 그냥 믿으면 실제로는
     * 올라오지 않았는데 key 만 박힌 유령 레코드가 생긴다.
     *
     * 크기 검사가 여기 있는 이유: presigned PUT 은 크기를 막지 못한다. 만료 전에 1GB 를
     * 밀어넣을 수 있으므로, 올라온 뒤에 재보고 초과분은 지우고 거절한다.
     */
    public StoragePort.StoredObject confirmUpload(String key) {
        StoragePort.StoredObject object = storage.find(key)
                .orElseThrow(() -> new ValidationException("업로드되지 않은 파일입니다: " + key));

        if (object.sizeBytes() > properties.maxUploadBytes()) {
            storage.delete(key);
            throw new ValidationException(
                    "파일이 너무 큽니다: %d bytes (최대 %d)".formatted(object.sizeBytes(), properties.maxUploadBytes()));
        }
        return object;
    }

    public void delete(String key) {
        storage.delete(key);
    }
}
