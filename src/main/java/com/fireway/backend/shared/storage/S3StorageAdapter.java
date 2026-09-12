package com.fireway.backend.shared.storage;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * StoragePort 의 S3 구현. 파일 바이트는 이 서버를 거치지 않는다 — 서버는 서명만 발급하고
 * 실제 전송은 클라이언트와 S3 가 직접 한다(t3.micro 램 1GB 에서 이게 중요하다).
 */
@Component
@Profile("prod")
public class S3StorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3StorageAdapter.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3StorageAdapter(S3Client s3Client, S3Presigner s3Presigner, StorageProperties properties) {
        // prod 인데 버킷이 비어 있으면 업로드·조회·삭제가 전부 실패한다.
        // 그건 첫 요청이 아니라 부팅에서 드러나는 편이 낫다.
        if (properties.bucket() == null || properties.bucket().isBlank()) {
            throw new IllegalStateException(
                    "app.storage.bucket 이 비어 있습니다. 서버의 ~/deploy/backend.env 에 S3_BUCKET 을 넣으세요.");
        }
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = properties.bucket();
    }

    @Override
    public PresignedUpload presignUpload(String key, String contentType, Duration ttl) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)   // 서명 대상 → 클라이언트가 다른 타입으로 못 올린다
                .build();

        String url = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(putObjectRequest)
                .build()).url().toString();

        return new PresignedUpload(url, key, ttl.toSeconds());
    }

    @Override
    public String presignDownload(String key, Duration ttl) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getObjectRequest)
                .build()).url().toString();
    }

    /**
     * HEAD 로 존재와 크기를 확인한다.
     *
     * ⚠️ '없는 키'의 응답이 IAM 정책에 따라 갈린다. s3:ListBucket 권한이 있으면 404 로 오지만,
     * 최소권한 정책(리스트 권한 없음)에서는 S3 가 존재 여부 자체를 숨기려고 403 AccessDenied 로
     * 답한다. 게다가 HEAD 는 응답 본문이 없어서(HTTP 명세) SDK 가 에러 코드를 본문에서 못 읽고
     * 상태 코드만으로 예외를 만드는 탓에, 404 조차 NoSuchKeyException 이 아니라 일반
     * S3Exception 으로 떨어질 수 있다.
     *
     * NoSuchKeyException 만 잡으면 '아직 안 올렸다'는 정상 시나리오가 500 으로 둔갑한다.
     * 그래서 403·404 를 둘 다 '없음'으로 본다. 진짜 권한 문제라면 이 로그가 대량으로 반복되므로
     * 빈도로 구분할 수 있게 WARN 을 남긴다.
     *
     * 네트워크 타임아웃(SdkClientException 계열)은 여기서 잡지 않는다 — S3 가 답한 게 아니라
     * 우리가 확인 자체를 못 한 것이라, '없음'으로 단정하면 안 되고 재시도 가능한 500 으로 흘린다.
     */
    @Override
    public Optional<StoredObject> find(String key) {
        HeadObjectResponse response;
        try {
            response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 403 || e.statusCode() == 404) {
                log.warn("S3 HEAD {} — 없는 키로 간주한다(403=ListBucket 없는 최소권한 IAM, 404=진짜 없음). "
                        + "대량 반복되면 IAM 정책 자체를 의심할 것. key={}", e.statusCode(), key);
                return Optional.empty();
            }
            throw e;
        }
        long size = response.contentLength() == null ? 0L : response.contentLength();
        return Optional.of(new StoredObject(key, size, response.contentType()));
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}
