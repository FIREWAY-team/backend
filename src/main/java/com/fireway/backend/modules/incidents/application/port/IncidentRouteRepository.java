package com.fireway.backend.modules.incidents.application.port;
import com.fireway.backend.modules.incidents.domain.IncidentRoute;
import java.util.List;
public interface IncidentRouteRepository {
    /** 한 신고의 경로 후보를 통째로 갈아끼운다. 다시 계산하면 이전 결과는 대체된다. */
    void replaceAll(long incidentId, List<IncidentRoute> routes);

    /** 순위 오름차순. */
    List<IncidentRoute> findByIncidentId(long incidentId);
}
