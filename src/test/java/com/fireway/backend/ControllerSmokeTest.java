package com.fireway.backend;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fireway.backend.modules.cctv.application.CctvService;
import com.fireway.backend.modules.cctv.application.port.CctvReadingRepository;
import com.fireway.backend.modules.cctv.domain.CctvReading;
import com.fireway.backend.modules.cctv.interfaces.CctvController;
import com.fireway.backend.modules.routing.application.*;
import com.fireway.backend.shared.storage.StorageProperties;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import com.fireway.backend.modules.routing.infrastructure.*;
import com.fireway.backend.modules.vehicles.application.port.VehicleRepository;
import static org.mockito.Mockito.mock;
import com.fireway.backend.modules.routing.interfaces.RouteController;
import com.fireway.backend.modules.scenarios.interfaces.ScenarioController;
import com.fireway.backend.modules.staticdata.application.NoGoAreaService;
import com.fireway.backend.modules.staticdata.interfaces.NoGoController;
import com.fireway.backend.modules.vehicles.interfaces.VehicleController;
import com.fireway.backend.modules.scenarios.application.ScenarioService;
import com.fireway.backend.modules.vehicles.application.VehicleService;
import java.util.List;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ControllerSmokeTest {
    MockMvc mvc;
    // standaloneSetup 은 부트가 만들어준 Jackson 설정을 물려받지 않는다. application.yml 의
    // property-naming-strategy: SNAKE_CASE 가 빠지므로, 여기서 직접 물리지 않으면 요청 본문의
    // snake_case 필드가 RouteRequest 에 바인딩되지 않고 @NotBlank 가 터져 400 이 난다.
    // 테스트가 실제 API 계약(snake_case)을 그대로 쓰게 하려고 camelCase 로 바꾸지 않고 컨버터를 맞춘다.
    private static MappingJackson2HttpMessageConverter snakeCaseJson() {
        return new MappingJackson2HttpMessageConverter(new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE));
    }
    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
            new ScenarioController(new ScenarioService(null)),
            new NoGoController(new NoGoAreaService(List::of)),
            new CctvController(new CctvService(stubReadingRepo(), noStorage(), stubStorageProps())),
            new VehicleController(new VehicleService(null)),
            new RouteController(new RoutePlanner(
                new VehicleService(mock(VehicleRepository.class)),
                new MockNoGoLookup(),
                new MockCctvReadingLookup(),
                new MockValhallaClient("https://router.project-osrm.org"),
                new WeightedOverlapCalculator(),
                new GoldenTimePrioritizer(),
                new RuleBasedExplanationBuilder()
            ))
        ).setMessageConverters(snakeCaseJson()).build();
    }
    @Test void scenarios() throws Exception { mvc.perform(get("/api/scenarios")).andExpect(status().isOk()); }
    @Test void noGo() throws Exception { mvc.perform(get("/api/no_go")).andExpect(status().isOk()); }
    @Test void cctv() throws Exception {
        mvc.perform(get("/api/cctv/cctv-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demo_assignment.evidence_cctv_id").value("cctv_moran_a41"))
                .andExpect(jsonPath("$.demo_assignment.reassigned").value(true));
    }
    @Test void vehicles() throws Exception { mvc.perform(get("/api/vehicles")).andExpect(status().isOk()); }
    @Test void route() throws Exception { mvc.perform(post("/api/route").contentType("application/json").content("{\"vehicle_id\":\"pump-3.5\",\"from\":{\"lat\":37.44,\"lon\":127.14},\"to\":{\"lat\":37.45,\"lon\":127.16}}")).andExpect(status().isOk()); }

    private static CctvReadingRepository stubReadingRepo() {
        return new CctvReadingRepository() {
            @Override public Optional<CctvReading> findById(String id) {
                return Optional.of(new CctvReading(id, 4.1, 5.2, 1.1,
                        Map.of("pump-3.5", "PASS", "pump-8", "UNCERTAIN"),
                        0.94, null, null, 37.43, 127.13, null, null, "computed", null,
                        new CctvReading.DemoAssignment("cctv_moran_a41", true, true)));
            }
            @Override public java.util.List<CctvReading> findAll() { return java.util.List.of(); }
        };
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<com.fireway.backend.shared.storage.StoragePort> noStorage() {
        return (ObjectProvider<com.fireway.backend.shared.storage.StoragePort>)
                mock(ObjectProvider.class);
    }

    private static StorageProperties stubStorageProps() {
        return new StorageProperties("test", "ap-northeast-2", Duration.ofMinutes(5),
                Duration.ofMinutes(10), 10_485_760L, 52_428_800L,
                "build/local-storage", "http://localhost:8080");
    }
}
