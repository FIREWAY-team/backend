package com.fireway.backend.modules.incidents.application.port;
import com.fireway.backend.modules.incidents.domain.Incident;
import com.fireway.backend.modules.incidents.domain.IncidentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface IncidentRepository {
    /** 저장하고 부여된 PK 를 돌려준다. incident_no 가 중복이면 예외가 올라온다. */
    long insert(Incident incident);

    /** status 가 null 이면 전체. 접수 최신순. */
    List<Incident> findAll(IncidentStatus status);

    Optional<Incident> findByNo(String incidentNo);

    /** 그날 접수된 건수. 접수번호 일련번호를 매기는 데 쓴다. */
    int countReceivedOn(LocalDate date);
}
