package com.fireway.backend.modules.incidents.interfaces.dto;
import com.fireway.backend.modules.incidents.domain.Incident;
import java.time.LocalDateTime;
public record IncidentResponse(String incidentNo, String status, String address,
                               double lat, double lon, String summary,
                               LocalDateTime receivedAt, LocalDateTime closedAt) {
    public static IncidentResponse from(Incident i) {
        return new IncidentResponse(i.incidentNo(), i.status().name(), i.address(),
                i.lat(), i.lon(), i.summary(), i.receivedAt(), i.closedAt());
    }
}
