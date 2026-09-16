package com.fireway.backend.modules.incidents.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fireway.backend.modules.incidents.application.port.IncidentAttachmentRepository;
import com.fireway.backend.modules.incidents.domain.IncidentAttachment;
import com.fireway.backend.shared.exception.ConflictException;
import com.fireway.backend.shared.exception.NotFoundException;
import com.fireway.backend.shared.exception.ValidationException;
import com.fireway.backend.shared.storage.FileUploadService;
import com.fireway.backend.shared.storage.StoragePort;
import com.fireway.backend.shared.storage.StorageProperties;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class IncidentAttachmentServiceTest {
    private static final Clock 고정시계 =
            Clock.fixed(Instant.parse("2026-09-16T05:30:00Z"), ZoneId.of("Asia/Seoul"));
    private static final double LAT = 37.4381, LON = 127.1422;
    // 상한을 작게 잡아 경계를 바이트 단위로 본다. 사진 1,000 / 동영상 10,000.
    private static final StorageProperties PROPERTIES = new StorageProperties(
            "fire-dispatch-uploads", "ap-northeast-2", Duration.ofMinutes(5), Duration.ofMinutes(10),
            1_000L, 10_000L, "build/local-storage", "http://localhost:8080");

    /** 메모리 S3. HEAD 를 불렀는지까지 센다. */
    private static final class Storage implements StoragePort {
        final Map<String, StoredObject> objects = new HashMap<>();
        int finds = 0;
        public PresignedUpload presignUpload(String key, String type, Duration ttl) {
            return new PresignedUpload("https://example.invalid/" + key, key, ttl.toSeconds());
        }
        public String presignDownload(String key, Duration ttl) { return "https://example.invalid/get/" + key; }
        public Optional<StoredObject> find(String key) { finds++; return Optional.ofNullable(objects.get(key)); }
        public void delete(String key) { objects.remove(key); }
    }

    /** 메모리 저장소. file_key 유니크 키를 실제 테이블과 같게 흉내낸다. */
    private static final class Attachments implements IncidentAttachmentRepository {
        final List<IncidentAttachment> rows = new ArrayList<>();
        public long insert(IncidentAttachment a) {
            if (rows.stream().anyMatch(r -> r.fileKey().equals(a.fileKey()))) {
                throw new DuplicateKeyException("uk_incident_attachments_file_key");
            }
            long id = rows.size() + 1;
            rows.add(a.withId(id));
            return id;
        }
        public List<IncidentAttachment> findByIncidentId(long incidentId) {
            return rows.stream().filter(r -> r.incidentId() == incidentId)
                    .sorted(Comparator.comparingLong(IncidentAttachment::id)).toList();
        }
    }

    private Storage storage;
    private Attachments attachments;
    private FileUploadService files;
    private IncidentAttachmentService service;
    private String no;

    @BeforeEach void setUp() {
        storage = new Storage();
        attachments = new Attachments();
        files = new FileUploadService(storage, PROPERTIES);
        IncidentService incidents = new IncidentService(new FakeIncidentRepository(), 고정시계);
        service = new IncidentAttachmentService(incidents, files, attachments, 고정시계);
        no = incidents.receive("성남시 중원구 은행로 12-3", LAT, LON, "주택 화재").incidentNo();
    }

    /** 프론트가 upload-url 을 받고 S3 에 PUT 까지 끝낸 상태를 만든다. */
    private String uploaded(String contentType, long sizeBytes) {
        String key = files.issueUploadUrl(contentType).key();
        storage.objects.put(key, new StoragePort.StoredObject(key, sizeBytes, contentType));
        return key;
    }

    @Test void 올린_파일을_붙이면_S3에서_잰_크기와_타입이_저장된다() {
        String key = uploaded("video/mp4", 5_000L);
        IncidentAttachment a = service.attach(no, key);
        assertThat(a.id()).isPositive();
        assertThat(a.fileKey()).isEqualTo(key);
        assertThat(a.contentType()).isEqualTo("video/mp4");
        assertThat(a.sizeBytes()).isEqualTo(5_000L);
        assertThat(a.createdAt()).isEqualTo(LocalDateTime.of(2026, 9, 16, 14, 30));
    }

    // 이 기능의 존재 이유. 프론트의 "올렸어요" 만 믿으면 유령 첨부가 생긴다.
    @Test void 올리지_않은_key는_422_이고_저장하지_않는다() {
        String key = files.issueUploadUrl("image/jpeg").key();   // 발급만 받고 PUT 은 안 했다
        assertThatThrownBy(() -> service.attach(no, key)).isInstanceOf(ValidationException.class);
        assertThat(attachments.rows).isEmpty();
    }

    @Test void 상한을_넘은_동영상은_S3에서_지우고_저장하지_않는다() {
        String key = uploaded("video/quicktime", 10_001L);
        assertThatThrownBy(() -> service.attach(no, key))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("너무 큽니다");
        assertThat(storage.objects).doesNotContainKey(key);
        assertThat(attachments.rows).isEmpty();
    }

    @Test void 발급한_모양이_아닌_key는_422_이고_S3를_보지_않는다() {
        assertThatThrownBy(() -> service.attach(no, "private/report.pdf"))
                .isInstanceOf(ValidationException.class);
        assertThat(storage.finds).isZero();
    }

    @Test void 같은_파일을_두_번_붙이면_409() {
        String key = uploaded("image/png", 500L);
        service.attach(no, key);
        assertThatThrownBy(() -> service.attach(no, key)).isInstanceOf(ConflictException.class);
        assertThat(attachments.rows).hasSize(1);
    }

    @Test void 없는_신고면_404_이고_S3는_보지_않는다() {
        String key = uploaded("image/png", 500L);
        assertThatThrownBy(() -> service.attach("2026-0101-9999", key))
                .isInstanceOf(NotFoundException.class);
        assertThat(storage.finds).isZero();
    }

    @Test void 붙인_순서대로_조회되고_조회_URL은_key로_서명한다() {
        String photo = uploaded("image/jpeg", 800L);
        String video = uploaded("video/mp4", 9_000L);
        service.attach(no, photo);
        service.attach(no, video);

        List<IncidentAttachment> found = service.findFor(no);
        assertThat(found).extracting(IncidentAttachment::fileKey).containsExactly(photo, video);
        assertThat(service.downloadUrl(found.get(0))).isEqualTo("https://example.invalid/get/" + photo);
    }

    @Test void 첨부_전에는_빈_목록이다() {
        assertThat(service.findFor(no)).isEmpty();
    }
}
