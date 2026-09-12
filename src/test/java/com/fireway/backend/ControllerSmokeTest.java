package com.fireway.backend;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fireway.backend.modules.cctv.interfaces.CctvController;
import com.fireway.backend.modules.routing.application.RouteService;
import com.fireway.backend.modules.routing.interfaces.RouteController;
import com.fireway.backend.modules.scenarios.interfaces.ScenarioController;
import com.fireway.backend.modules.staticdata.application.NoGoAreaService;
import com.fireway.backend.modules.staticdata.interfaces.NoGoController;
import com.fireway.backend.modules.vehicles.interfaces.VehicleController;
import com.fireway.backend.modules.scenarios.application.ScenarioService;
import com.fireway.backend.modules.vehicles.application.VehicleService;
import org.junit.jupiter.api.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ControllerSmokeTest {
    MockMvc mvc;

    @BeforeEach void setUp() {
        // standaloneSetup 은 부트 자동설정을 안 태우므로 snake_case 를 직접 걸어준다(JacksonConfig 와 동일).
        // 없으면 요청 본문의 scenario_id 가 바인딩되지 않아 @Valid 가 400을 낸다.
        var converter = new MappingJackson2HttpMessageConverter(
                new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE));
        mvc = MockMvcBuilders.standaloneSetup(
                new ScenarioController(new ScenarioService(null)),
                new NoGoController(new NoGoAreaService(List::of)),
                new CctvController(),
                new VehicleController(new VehicleService(null)),
                new RouteController(new RouteService(null))).setMessageConverters(converter).build();
    }

    @Test void scenarios() throws Exception { mvc.perform(get("/api/scenarios")).andExpect(status().isOk()); }
    @Test void noGo() throws Exception { mvc.perform(get("/api/no_go")).andExpect(status().isOk()); }
    @Test void cctv() throws Exception { mvc.perform(get("/api/cctv/cctv-01")).andExpect(status().isOk()); }
    @Test void vehicles() throws Exception { mvc.perform(get("/api/vehicles")).andExpect(status().isOk()); }
    @Test void route() throws Exception { mvc.perform(post("/api/route").contentType("application/json").content("{\"scenario_id\":\"bank-01\"}")).andExpect(status().isOk()); }
}
