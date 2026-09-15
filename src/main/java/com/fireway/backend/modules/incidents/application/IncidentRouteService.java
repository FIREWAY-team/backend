package com.fireway.backend.modules.incidents.application;
import com.fireway.backend.modules.incidents.application.port.IncidentRouteRepository;
import com.fireway.backend.modules.incidents.application.port.RoutePlanning;
import com.fireway.backend.modules.incidents.domain.Incident;
import com.fireway.backend.modules.incidents.domain.IncidentRoute;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class IncidentRouteService {
    private final IncidentService incidents;
    private final RoutePlanning planner;
    private final IncidentRouteRepository repository;

    public IncidentRouteService(IncidentService incidents, RoutePlanning planner,
                                IncidentRouteRepository repository) {
        this.incidents = incidents;
        this.planner = planner;
        this.repository = repository;
    }

    /**
     * 출발지에서 신고 지점까지 경로를 산출하고 근거까지 저장한다.
     * 다시 부르면 이전 결과를 대체한다 — 차량이 바뀌면 판정도 통째로 바뀌기 때문이다.
     */
    @Transactional
    public List<IncidentRoute> planAndSave(String incidentNo, String vehicleId,
                                           double fromLat, double fromLon) {
        Incident incident = incidents.get(incidentNo);   // 없으면 여기서 404
        List<IncidentRoute> planned = planner.plan(vehicleId, fromLat, fromLon,
                incident.lat(), incident.lon());
        repository.replaceAll(incident.id(), planned);
        return repository.findByIncidentId(incident.id());
    }

    public List<IncidentRoute> findFor(String incidentNo) {
        return repository.findByIncidentId(incidents.get(incidentNo).id());
    }
}
