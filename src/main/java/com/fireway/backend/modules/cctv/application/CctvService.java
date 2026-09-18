package com.fireway.backend.modules.cctv.application;

import com.fireway.backend.modules.cctv.application.port.CctvReadingRepository;
import com.fireway.backend.modules.cctv.domain.CctvReading;
import com.fireway.backend.shared.storage.StoragePort;
import com.fireway.backend.shared.storage.StorageProperties;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * CCTV 판독 조회 서비스. Repository 로 DB 행을 읽고, s3_media.key 또는 still_public_url 의 객체 key 에
 * 대해 StoragePort 로 프리사인드 GET 을 발급한다.
 *
 * StoragePort 는 prod(S3StorageAdapter) 프로파일에서만 빈이 뜬다 — local/test 는 없으므로
 * ObjectProvider 로 optional 주입하고, 없으면 원본 URL(fixture http url 또는 raw key) 을 그대로 준다.
 */
@Service
public class CctvService {

    private static final java.time.Duration DEFAULT_TTL = java.time.Duration.ofMinutes(10);

    private final CctvReadingRepository readings;
    private final ObjectProvider<StoragePort> storageProvider;
    private final StorageProperties storageProperties;

    public CctvService(CctvReadingRepository readings,
            ObjectProvider<StoragePort> storageProvider,
            StorageProperties storageProperties) {
        this.readings = readings;
        this.storageProvider = storageProvider;
        this.storageProperties = storageProperties;
    }

    public Optional<CctvReading> get(String id) {
        return readings.findById(id);
    }

    public List<CctvReading> list() {
        return readings.findAll();
    }

    /** 재생용 GET URL. http(s) 원본이면 그대로 · S3 key 면 프리사인드 · 없으면 null. */
    public String resolveMediaUrl(CctvReading reading) {
        String key = pickKey(reading);
        if (key == null) return null;
        if (key.startsWith("http://") || key.startsWith("https://")) return key;
        StoragePort storage = storageProvider.getIfAvailable();
        if (storage == null) {
            // local/test — 프리사이너 없음. 원본 key 를 그대로 노출하지 않고 null.
            return null;
        }
        java.time.Duration ttl = storageProperties.presignGetTtl();
        return storage.presignDownload(key, ttl == null ? DEFAULT_TTL : ttl);
    }

    public long mediaUrlTtlSeconds() {
        java.time.Duration ttl = storageProperties.presignGetTtl();
        return (ttl == null ? DEFAULT_TTL : ttl).toSeconds();
    }

    private static String pickKey(CctvReading r) {
        if (r.s3Key() != null && !r.s3Key().isBlank()) return r.s3Key();
        // still_public_url 컬럼은 실제 uploads/... key 또는 fixture 의 http URL.
        return r.stillPublicUrl();
    }
}
