package com.fireway.backend.modules.incidents.interfaces;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fireway.backend.modules.incidents.application.IncidentService;
import com.fireway.backend.modules.incidents.application.port.IncidentRepository;
import com.fireway.backend.modules.incidents.domain.Incident;
import com.fireway.backend.modules.incidents.domain.IncidentStatus;
import com.fireway.backend.shared.exception.GlobalExceptionHandler;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IncidentControllerTest {
    private static final Clock 고정시계 =
            Clock.fixed(Instant.parse("2026-09-15T05:30:00Z"), ZoneId.of("Asia/Seoul"));
    private MockMvc mvc;

    /** 메모리 저장소. 컨트롤러 계약만 보므로 최소 구현이면 된다. */
    private static final class Repo implements IncidentRepository {
        final List<Incident> rows = new ArrayList<>();
        long seq = 0;
        public long insert(Incident i) { rows.add(i.withId(++seq)); return seq; }
        public List<Incident> findAll(IncidentStatus s) {
            return rows.stream().filter(r -> s == null || r.status() == s).toList();
        }
        public Optional<Incident> findByNo(String no) {
            return rows.stream().filter(r -> r.incidentNo().equals(no)).findFirst();
        }
        public int countReceivedOn(LocalDate d) { return rows.size(); }
    }

    @BeforeEach void setUp() {
        // standaloneSetup 은 부트 자동설정을 안 태우므로 snake_case 와 시간 모듈을 직접 건다.
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mvc = MockMvcBuilders
                .standaloneSetup(new IncidentController(new IncidentService(new Repo(), 고정시계)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    private static String body(String address, double lat, double lon) {
        return """
                {"address":"%s","lat":%s,"lon":%s,"summary":"주택 화재"}""".formatted(address, lat, lon);
    }

    @Test void 접수하면_201_과_접수번호를_돌려준다() throws Exception {
        mvc.perform(post("/api/incidents").contentType("application/json")
                        .content(body("성남시 중원구 은행로 12-3", 37.4381, 127.1422)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incident_no").value("2026-0915-0001"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.closed_at").doesNotExist());
    }

    @Test void 접수_후_목록과_상세로_다시_읽힌다() throws Exception {
        mvc.perform(post("/api/incidents").contentType("application/json")
                .content(body("성남시 중원구 은행로 12-3", 37.4381, 127.1422)));
        mvc.perform(get("/api/incidents")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/incidents/2026-0915-0001")).andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("성남시 중원구 은행로 12-3"));
    }

    // 위경도를 뒤바꿔 넣는 실수를 422 로 돌려준다. 조용히 저장되면 지도에서 사라진다.
    @Test void 위경도가_뒤바뀌면_422() throws Exception {
        mvc.perform(post("/api/incidents").contentType("application/json")
                        .content(body("주소", 127.1422, 37.4381)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test void 주소가_비면_400_대로_막힌다() throws Exception {
        mvc.perform(post("/api/incidents").contentType("application/json")
                        .content("""
                                {"address":"","lat":37.4381,"lon":127.1422}"""))
                .andExpect(status().is4xxClientError());
    }

    @Test void 없는_접수번호는_404() throws Exception {
        mvc.perform(get("/api/incidents/2026-0101-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test void 알_수_없는_상태값은_422() throws Exception {
        mvc.perform(get("/api/incidents").param("status", "NOPE"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void 상태로_거를_수_있다() throws Exception {
        mvc.perform(post("/api/incidents").contentType("application/json")
                .content(body("주소", 37.4381, 127.1422)));
        mvc.perform(get("/api/incidents").param("status", "received"))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/incidents").param("status", "CLOSED"))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
