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

    /**
     * 그날 실제로 쓴 접수번호 일련번호의 최댓값. 아직 없으면 0.
     * 건수가 아니라 최댓값인 이유는, 행이 하나라도 지워졌을 때 건수는 뒤로 돌아가지만
     * 이미 발급한 번호는 그대로이기 때문이다. 뒤로 돌아가면 같은 번호를 다시 발급한다.
     */
    int lastSequenceOn(LocalDate date);
}
