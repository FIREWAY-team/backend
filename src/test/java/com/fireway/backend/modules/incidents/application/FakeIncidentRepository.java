package com.fireway.backend.modules.incidents.application;
import com.fireway.backend.modules.incidents.application.port.IncidentRepository;
import com.fireway.backend.modules.incidents.domain.Incident;
import com.fireway.backend.modules.incidents.domain.IncidentStatus;
import java.time.LocalDate;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;

/** 메모리 저장소. 유니크 키와 정렬까지 실제 어댑터와 같게 흉내낸다. */
class FakeIncidentRepository implements IncidentRepository {
    private final List<Incident> rows = new ArrayList<>();
    private long seq = 0;
    /** 다음 insert 를 중복으로 실패시킬 횟수. 번호 재시도를 검증하는 데 쓴다. */
    int failNextInserts = 0;

    @Override public long insert(Incident incident) {
        if (failNextInserts > 0) { failNextInserts--; throw new DuplicateKeyException("uk_incidents_no"); }
        if (rows.stream().anyMatch(r -> r.incidentNo().equals(incident.incidentNo()))) {
            throw new DuplicateKeyException("uk_incidents_no");
        }
        long id = ++seq;
        rows.add(incident.withId(id));
        return id;
    }
    @Override public List<Incident> findAll(IncidentStatus status) {
        return rows.stream()
                .filter(r -> status == null || r.status() == status)
                .sorted(Comparator.comparing(Incident::receivedAt).reversed()
                        .thenComparing(Comparator.comparingLong(Incident::id).reversed()))
                .toList();
    }
    @Override public Optional<Incident> findByNo(String no) {
        return rows.stream().filter(r -> r.incidentNo().equals(no)).findFirst();
    }
    @Override public int countReceivedOn(LocalDate date) {
        return (int) rows.stream().filter(r -> r.receivedAt().toLocalDate().equals(date)).count();
    }
}
