package com.fireway.backend.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fireway.backend.shared.exception.GlobalExceptionHandler;
import com.fireway.backend.shared.exception.ValidationException;

/**
 * DB나 AWS 없이 도는 테스트. StoragePort 를 가짜로 갈아끼워 정책만 검증한다
 * (BackendApplicationTests 주석의 이유로 @SpringBootTest 를 쓰지 않는다).
 */
class FileUploadTest {

    static final StorageProperties PROPERTIES = new StorageProperties(
            "fire-dispatch-uploads", "ap-northeast-2",
            Duration.ofMinutes(5), Duration.ofMinutes(10),
            1_000L, "build/local-storage", "http://localhost:8080");

    FakeStorage storage;
    FileUploadService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        storage = new FakeStorage();
        service = new FileUploadService(storage, PROPERTIES);

        // 운영과 같은 snake_case 로 주고받는지까지 보려면 컨버터를 맞춰야 한다
        // (standaloneSetup 은 application.yml 의 jackson 설정을 읽지 않는다).
        ObjectMapper objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mvc = MockMvcBuilders.standaloneSetup(new FileUploadController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void 허용된_타입은_업로드_URL을_받는다() throws Exception {
        mvc.perform(post("/api/files/upload-url")
                        .contentType("application/json")
                        .content("{\"content_type\":\"image/jpeg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").exists())
                .andExpect(jsonPath("$.upload_url").exists())
                .andExpect(jsonPath("$.expires_in_seconds").value(300));
    }

    @Test
    void 허용되지_않는_타입은_422다() throws Exception {
        mvc.perform(post("/api/files/upload-url")
                        .contentType("application/json")
                        .content("{\"content_type\":\"application/pdf\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void key에_사용자_입력이_들어가지_않는다() {
        StoragePort.PresignedUpload upload = service.issueUploadUrl("image/png");
        assertThat(upload.key()).matches("uploads/\\d{4}-\\d{2}-\\d{2}/[0-9a-f-]{36}");
    }

    @Test
    void 올라오지_않은_key는_확인에서_걸린다() {
        assertThatThrownBy(() -> service.confirmUpload("uploads/2026-09-09/없는키"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void 크기를_넘긴_파일은_지우고_거절한다() {
        storage.objects.put("big", new StoragePort.StoredObject("big", 1_001L, "image/jpeg"));

        assertThatThrownBy(() -> service.confirmUpload("big"))
                .isInstanceOf(ValidationException.class);
        assertThat(storage.objects).doesNotContainKey("big");
    }

    static class FakeStorage implements StoragePort {
        final Map<String, StoredObject> objects = new HashMap<>();

        public PresignedUpload presignUpload(String key, String contentType, Duration ttl) {
            return new PresignedUpload("https://example.invalid/" + key, key, ttl.toSeconds());
        }

        public String presignDownload(String key, Duration ttl) {
            return "https://example.invalid/" + key;
        }

        public Optional<StoredObject> find(String key) {
            return Optional.ofNullable(objects.get(key));
        }

        public void delete(String key) {
            objects.remove(key);
        }
    }
}
