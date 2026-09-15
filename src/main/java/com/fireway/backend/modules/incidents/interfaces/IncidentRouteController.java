package com.fireway.backend.modules.incidents.interfaces;
import com.fireway.backend.modules.incidents.application.IncidentRouteService;
import com.fireway.backend.modules.incidents.interfaces.dto.IncidentRouteResponse;
import com.fireway.backend.modules.incidents.interfaces.dto.RoutePlanRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/incidents/{incidentNo}/routes")
public class IncidentRouteController {
    private final IncidentRouteService service;
    public IncidentRouteController(IncidentRouteService service) { this.service = service; }

    /** 경로를 산출하고 판단 근거까지 저장한다. 다시 부르면 이전 결과를 대체한다. */
    @PostMapping public List<IncidentRouteResponse> plan(@PathVariable String incidentNo,
                                                         @Valid @RequestBody RoutePlanRequest request) {
        return service.planAndSave(incidentNo, request.vehicleId(), request.fromLat(), request.fromLon())
                .stream().map(IncidentRouteResponse::from).toList();
    }

    /** 저장된 경로. 아직 산출 전이면 빈 목록. */
    @GetMapping public List<IncidentRouteResponse> list(@PathVariable String incidentNo) {
        return service.findFor(incidentNo).stream().map(IncidentRouteResponse::from).toList();
    }
}
