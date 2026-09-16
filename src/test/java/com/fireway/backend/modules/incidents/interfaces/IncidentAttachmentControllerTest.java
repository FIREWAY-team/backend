package com.fireway.backend.modules.incidents.interfaces;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fireway.backend.modules.incidents.application.IncidentAttachmentService;
import com.fireway.backend.modules.incidents.application.IncidentService;
import com.fireway.backend.modules.incidents.application.port.IncidentAttachmentRepository;
import com.fireway.backend.modules.incidents.application.port.IncidentRepository;
import com.fireway.backend.modules.incidents.domain.Incident;
import com.fireway.backend.modules.incidents.domain.IncidentAttachment;
import com.fireway.backend.modules.incidents.domain.IncidentStatus;
import com.fireway.backend.shared.exception.GlobalExceptionHandler;
import com.fireway.backend.shared.storage.FileUploadService;
import com.fireway.backend.shared.storage.StoragePort;
import com.fireway.backend.shared.storage.StorageProperties;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IncidentAttachmentControllerTest {
    private static final Clock 고정시계 =
            Clock.fixed(Instant.parse("2026-09-16T05:30:00Z"), ZoneId.of("Asia/Seoul"));
    private static final String NO = "2026-0916-0001";

    /** 컨트롤러 계약만 보므로 최소 구현이면 된다. 신고는 NO 한 건만 있다. */
    private static final class Incidents implements IncidentRepository {
        public long insert(Incident i) { return 1; }
        public List<Incident> findAll(IncidentStatus s) { return List.of(); }
        public Optional<Incident> findByNo(String no) {
            return NO.equals(no)
                    ? Optional.of(new Incident(1, NO, IncidentStatus.RECEIVED, "주소", 37.4381, 127.1422, "",
                            LocalDateTime.now(고정시계), null))
                    : Optional.empty();
        }
        public int lastSequenceOn(LocalDate d) { return 0; }
    }
    private static final class Attachments implements IncidentAttachmentRepository {
        final List<IncidentAttachment> rows = new ArrayList<>();
        public long insert(IncidentAttachment a) {
            if (rows.stream().anyMatch(r -> r.fileKey().equals(a.fileKey()))) throw new DuplicateKeyException("uk");
            rows.add(a.withId(rows.size() + 1));
            return rows.size();
        }
        public List<IncidentAttachment> findByIncidentId(long id) { return List.copyOf(rows); }
    }
    private static final class Storage implements StoragePort {
        final Map<String, StoredObject> objects = new HashMap<>();
        public PresignedUpload presignUpload(String key, String type, Duration ttl) {
            return new PresignedUpload("https://example.invalid/" + key, key, ttl.toSeconds());
        }
        public String presignDownload(String key, Duration ttl) { return "https://example.invalid/get/" + key; }
        public Optional<StoredObject> find(String key) { return Optional.ofNullable(objects.get(key)); }
        public void delete(String key) { objects.remove(key); }
    }

    private MockMvc mvc;
    private Storage storage;
    private FileUploadService files;

    @BeforeEach void setUp() {
        storage = new Storage();
        files = new FileUploadService(storage, new StorageProperties("bucket", "ap-northeast-2",
                Duration.ofMinutes(5), Duration.ofMinutes(10), 1_000L, 10_000L, "build/local-storage", "http://localhost:8080"));
        var service = new IncidentAttachmentService(
                new IncidentService(new Incidents(), 고정시계), files, new Attachments(), 고정시계);
        // standaloneSetup 은 부트 자동설정을 안 태우므로 snake_case 와 시간 모듈을 직접 건다.
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mvc = MockMvcBuilders.standaloneSetup(new IncidentAttachmentController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    private String uploaded(String contentType, long sizeBytes) {
        String key = files.issueUploadUrl(contentType).key();
        storage.objects.put(key, new StoragePort.StoredObject(key, sizeBytes, contentType));
        return key;
    }

    private static String body(String key) { return "{\"key\":\"%s\"}".formatted(key); }

    @Test void 첨부하면_201_과_조회_URL을_돌려준다() throws Exception {
        String key = uploaded("video/mp4", 9_000L);
        mvc.perform(post("/api/incidents/" + NO + "/attachments").contentType("application/json").content(body(key)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.content_type").value("video/mp4"))
                .andExpect(jsonPath("$.size_bytes").value(9_000))
                .andExpect(jsonPath("$.download_url").value("https://example.invalid/get/" + key))
                .andExpect(jsonPath("$.created_at").exists());
    }

    @Test void 첨부_후_목록으로_다시_읽힌다() throws Exception {
        mvc.perform(post("/api/incidents/" + NO + "/attachments").contentType("application/json")
                .content(body(uploaded("image/jpeg", 500L))));
        mvc.perform(get("/api/incidents/" + NO + "/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].download_url").exists());
    }

    @Test void 상한을_넘으면_422() throws Exception {
        mvc.perform(post("/api/incidents/" + NO + "/attachments").contentType("application/json")
                        .content(body(uploaded("image/jpeg", 1_001L))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test void key가_비면_422() throws Exception {
        mvc.perform(post("/api/incidents/" + NO + "/attachments").contentType("application/json").content(body("")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void 같은_파일을_두_번_붙이면_409() throws Exception {
        String key = uploaded("image/png", 500L);
        mvc.perform(post("/api/incidents/" + NO + "/attachments").contentType("application/json").content(body(key)));
        mvc.perform(post("/api/incidents/" + NO + "/attachments").contentType("application/json").content(body(key)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test void 없는_신고는_404() throws Exception {
        mvc.perform(get("/api/incidents/2026-0101-9999/attachments"))
                .andExpect(status().isNotFound());
    }
}
