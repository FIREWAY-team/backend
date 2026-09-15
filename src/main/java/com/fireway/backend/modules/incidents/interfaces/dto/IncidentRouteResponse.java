package com.fireway.backend.modules.incidents.interfaces.dto;
import com.fireway.backend.modules.incidents.domain.IncidentRoute;
import java.util.List;
public record IncidentRouteResponse(int rank, boolean recommended, String vehicleId,
                                    Integer distanceM, Integer etaSeconds, String polyline,
                                    boolean passableForVehicle, boolean meetsGoldenTime,
                                    Double passableProb, String explanation,
                                    List<IncidentRoute.Blocked> excludedReasons,
                                    List<String> unlockedByCctv) {
    public static IncidentRouteResponse from(IncidentRoute r) {
        return new IncidentRouteResponse(r.rank(), r.recommended(), r.vehicleId(),
                r.distanceM(), r.etaSeconds(), r.polyline(), r.passableForVehicle(),
                r.meetsGoldenTime(), r.passableProb(), r.explanation(),
                r.excludedReasons(), r.unlockedByCctv());
    }
}
