package com.fireway.backend.shared.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fireway.backend.shared.exception.InfrastructureException;
import com.fireway.backend.shared.exception.ValidationException;

/**
 * 로컬 개발용 저장소. 팀원 노트북에는 IAM 역할이 없어서 DefaultCredentialsProvider 체인이
 * 5단계 모두 실패한다 — 그래서 로컬은 AWS 를 아예 안 쓴다.
 *
 * 대안으로 '로컬 전용 IAM 사용자 키를 각자 ~/.aws/credentials 에 넣기'가 있지만, 팀원 수만큼
 * 유출 지점이 늘어난다. 파일시스템으로 가는 편이 낫다.
 *
 * ⚠️ 여기서 돌려주는 URL 에는 서명이 없다. 로컬에서만 뜨는 어댑터이므로 의도된 것이고,
 *    presigned URL 의 '만료·위조 불가' 성질은 prod 에서만 검증된다.
 */
@Component
@Profile("!prod")
public class LocalStorageAdapter implements StoragePort {

    static final String URL_PREFIX = "/local-storage/";

    private final Path root;
    private final String baseUrl;

    public LocalStorageAdapter(StorageProperties properties) {
        this.root = Path.of(properties.localDir()).toAbsolutePath().normalize();
        this.baseUrl = properties.localBaseUrl();
    }

    @Override
    public PresignedUpload presignUpload(String key, String contentType, Duration ttl) {
        return new PresignedUpload(baseUrl + URL_PREFIX + key, key, ttl.toSeconds());
    }

    @Override
    public String presignDownload(String key, Duration ttl) {
        return baseUrl + URL_PREFIX + key;
    }

    @Override
    public Optional<StoredObject> find(String key) {
        Path file = resolve(key);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new StoredObject(key, Files.size(file), Files.probeContentType(file)));
        } catch (IOException e) {
            throw new InfrastructureException("로컬 저장소를 읽지 못했습니다: " + key);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new InfrastructureException("로컬 저장소에서 지우지 못했습니다: " + key);
        }
    }

    byte[] read(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void write(String key, byte[] bytes) {
        Path file = resolve(key);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, bytes);
        } catch (IOException e) {
            throw new InfrastructureException("로컬 저장소에 쓰지 못했습니다: " + key);
        }
    }

    /**
     * key 를 실제 경로로 바꾸면서 루트 밖으로 나가는 것을 막는다.
     *
     * 우리가 만드는 key 는 UUID 라 안전하지만, LocalStorageController 는 인증이 없는 열린
     * 엔드포인트라 "../../.ssh/authorized_keys" 같은 key 가 들어올 수 있다. 저장 경로에
     * 사용자 입력을 그대로 쓰지 않는다는 규칙은 로컬에서도 지킨다.
     */
    Path resolve(String key) {
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) {
            throw new ValidationException("허용되지 않는 경로입니다: " + key);
        }
        return file;
    }
}
