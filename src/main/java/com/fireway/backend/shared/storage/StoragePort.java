package com.fireway.backend.shared.storage;

import java.time.Duration;
import java.util.Optional;

/**
 * 파일 저장소에 대한 아웃바운드 포트.
 *
 * key 생성 규칙과 허용 Content-Type 같은 '정책'은 여기 두지 않는다 — 그건 FileUploadService 의
 * 몫이고, 어댑터마다 복사되면 S3 와 로컬의 규칙이 갈린다. 어댑터는 시키는 대로만 한다.
 */
public interface StoragePort {

    /** 업로드용 서명 URL. 서명에 contentType 이 포함되므로 클라이언트가 다른 타입으로 못 올린다. */
    PresignedUpload presignUpload(String key, String contentType, Duration ttl);

    /** 조회용 서명 URL. 버킷이 퍼블릭 차단이라 이걸 거치지 않으면 403 이다. */
    String presignDownload(String key, Duration ttl);

    /** 실제로 올라와 있는지, 크기가 얼마인지. 없으면 empty — 예외가 아니다. */
    Optional<StoredObject> find(String key);

    void delete(String key);

    record PresignedUpload(String uploadUrl, String key, long expiresInSeconds) { }

    record StoredObject(String key, long sizeBytes, String contentType) { }
}
