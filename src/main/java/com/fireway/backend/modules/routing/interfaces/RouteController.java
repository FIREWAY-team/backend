package com.fireway.backend.modules.routing.interfaces;
import com.fireway.backend.modules.routing.application.RouteService;
import com.fireway.backend.modules.routing.application.RouteService.RouteRequestValues;
import com.fireway.backend.modules.routing.interfaces.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/route") public class RouteController {
    private final RouteService service; public RouteController(RouteService service) { this.service = service; }
    @PostMapping public RouteResponse route(@Valid @RequestBody RouteRequest r) { return RouteResponse.from(service.mock(new RouteRequestValues(r.scenarioId(), r.lat(), r.lon(), r.vehicleId()))); }
}
