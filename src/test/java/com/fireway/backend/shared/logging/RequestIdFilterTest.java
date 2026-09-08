package com.fireway.backend.shared.logging;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.test.web.servlet.MockMvc;

class RequestIdFilterTest {
    @Test void requestIdIsReturned() throws Exception { MockMvc mvc = MockMvcBuilders.standaloneSetup(new Probe()).addFilters(new RequestIdFilter()).build(); mvc.perform(get("/ping").header("X-Request-Id", "req-test")).andExpect(status().isOk()).andExpect(header().string("X-Request-Id", "req-test")); }
    @RestController static class Probe { @GetMapping("/ping") String ping() { return "ok"; } }
}
