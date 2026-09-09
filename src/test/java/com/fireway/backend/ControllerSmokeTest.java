package com.fireway.backend;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fireway.backend.modules.cctv.interfaces.CctvController;
import com.fireway.backend.modules.routing.application.*;
import com.fireway.backend.modules.routing.infrastructure.*;
import com.fireway.backend.modules.vehicles.application.port.VehicleRepository;
import static org.mockito.Mockito.mock;
import com.fireway.backend.modules.routing.interfaces.RouteController;
import com.fireway.backend.modules.scenarios.interfaces.ScenarioController;
import com.fireway.backend.modules.staticdata.interfaces.NoGoController;
import com.fireway.backend.modules.vehicles.interfaces.VehicleController;
import com.fireway.backend.modules.scenarios.application.ScenarioService;
import com.fireway.backend.modules.vehicles.application.VehicleService;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ControllerSmokeTest {
    MockMvc mvc;
    @BeforeEach void setUp() { mvc = MockMvcBuilders.standaloneSetup(new ScenarioController(new ScenarioService(null)), new NoGoController(), new CctvController(), new VehicleController(new VehicleService(null)), new RouteController(new RoutePlanner(new VehicleService(mock(VehicleRepository.class)),
            new MockNoGoLookup(), new MockValhallaClient(), new WeightedOverlapCalculator(),
            new GoldenTimePrioritizer(), new RuleBasedExplanationBuilder()))).build(); }
    @Test void scenarios() throws Exception { mvc.perform(get("/api/scenarios")).andExpect(status().isOk()); }
    @Test void noGo() throws Exception { mvc.perform(get("/api/no_go")).andExpect(status().isOk()); }
    @Test void cctv() throws Exception { mvc.perform(get("/api/cctv/cctv-01")).andExpect(status().isOk()); }
    @Test void vehicles() throws Exception { mvc.perform(get("/api/vehicles")).andExpect(status().isOk()); }
    @Test void route() throws Exception { mvc.perform(post("/api/route").contentType("application/json").content("{\"vehicle_id\":\"pump-3.5\",\"from\":{\"lat\":37.44,\"lon\":127.14},\"to\":{\"lat\":37.45,\"lon\":127.16}}")).andExpect(status().isOk()); }
}
