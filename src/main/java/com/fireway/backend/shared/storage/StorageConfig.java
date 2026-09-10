package com.fireway.backend.shared.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 저장소 설정. 프로퍼티 등록은 프로파일과 무관하게(로컬 어댑터도 값을 읽는다),
 * AWS 클라이언트 빈은 prod 에서만 만든다.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    /**
     * S3 클라이언트/프리사이너. prod 에서만 뜬다 — 로컬은 LocalStorageAdapter 가 대신하므로
     * 팀원 노트북에 AWS 자격증명이 아예 없어도 부팅된다.
     *
     * 자격증명은 DefaultCredentialsProvider 체인으로 받는다. 서버에서는 이 체인의 마지막 단계인
     * EC2 인스턴스 메타데이터(IMDS)에서 인스턴스에 붙인 IAM 역할을 줍는다 — 액세스키를
     * backend.env 에 넣지 않는다는 뜻이다.
     *
     * ⚠️ 이 앱은 EC2 위 '도커 컨테이너' 안에서 돈다. IMDSv2 의 응답 홉 제한 기본값이 1 이면
     *    컨테이너(브리지 네트워크로 홉이 하나 더 늘어남)에서는 토큰을 못 받아 체인이 통째로
     *    실패한다. 인스턴스 메타데이터 옵션의 HttpPutResponseHopLimit 을 2 로 올려야 한다.
     */
    @Configuration
    @Profile("prod")
    static class S3ClientConfig {

        // presign 은 네트워크를 타지 않지만(로컬 서명 계산), head/delete 는 실제 호출이다.
        // 시간 제한이 없으면 S3 가 느려질 때 SDK 기본 재시도까지 겹쳐 요청 스레드를 오래 붙든다.
        private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(10);
        private static final Duration API_CALL_ATTEMPT_TIMEOUT = Duration.ofSeconds(5);

        @Bean
        S3Client s3Client(StorageProperties properties) {
            return S3Client.builder()
                    .region(Region.of(properties.region()))
                    .credentialsProvider(DefaultCredentialsProvider.builder().build())
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .apiCallTimeout(API_CALL_TIMEOUT)
                            .apiCallAttemptTimeout(API_CALL_ATTEMPT_TIMEOUT)
                            .build())
                    .build();
        }

        @Bean
        S3Presigner s3Presigner(StorageProperties properties) {
            return S3Presigner.builder()
                    .region(Region.of(properties.region()))
                    .credentialsProvider(DefaultCredentialsProvider.builder().build())
                    .build();
        }
    }
}
