package com.fireway.backend.modules.incidents.interfaces;
import com.fireway.backend.modules.incidents.application.IncidentService;
import com.fireway.backend.modules.incidents.domain.IncidentStatus;
import com.fireway.backend.modules.incidents.interfaces.dto.IncidentRequest;
import com.fireway.backend.modules.incidents.interfaces.dto.IncidentResponse;
import com.fireway.backend.shared.exception.ValidationException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/incidents") public class IncidentController {
    private final IncidentService service;
    public IncidentController(IncidentService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse receive(@Valid @RequestBody IncidentRequest request) {
        return IncidentResponse.from(service.receive(
                request.address(), request.lat(), request.lon(), request.summary()));
    }

    /** status 를 주지 않으면 전체. 접수 최신순. */
    @GetMapping public List<IncidentResponse> list(@RequestParam(required = false) String status) {
        return service.list(parse(status)).stream().map(IncidentResponse::from).toList();
    }

    @GetMapping("/{incidentNo}")
    public IncidentResponse get(@PathVariable String incidentNo) {
        return IncidentResponse.from(service.get(incidentNo));
    }

    private static IncidentStatus parse(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return IncidentStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("알 수 없는 상태입니다: " + status);
        }
    }
}
