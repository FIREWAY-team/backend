package com.fireway.backend.shared.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * app.storage.* 바인딩.
 *
 * bucket 만 기본값이 없다 — prod 에서 비어 있으면 S3StorageAdapter 가 부팅에서 끊는다.
 * 나머지는 기본값을 코드에도 두어, yml 에서 줄이 사라져도 조용히 0 이 되지 않게 한다
 * (record 생성자 바인딩에서 primitive 는 값이 없으면 0 이 된다).
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String bucket,
        @DefaultValue("ap-northeast-2") String region,
        @DefaultValue("5m") Duration presignPutTtl,
        @DefaultValue("10m") Duration presignGetTtl,
        @DefaultValue("10485760") long maxUploadBytes,
        @DefaultValue("build/local-storage") String localDir,
        @DefaultValue("http://localhost:8080") String localBaseUrl) {
}
