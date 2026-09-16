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
    /** 발급한 번호는 지워도 남는다. 어댑터의 MAX(일련번호) 와 같은 성질이다. */
    private final Map<String, Integer> issued = new HashMap<>();

    @Override public long insert(Incident incident) {
        if (failNextInserts > 0) { failNextInserts--; throw new DuplicateKeyException("uk_incidents_no"); }
        if (rows.stream().anyMatch(r -> r.incidentNo().equals(incident.incidentNo()))) {
            throw new DuplicateKeyException("uk_incidents_no");
        }
        long id = ++seq;
        rows.add(incident.withId(id));
        remember(incident.incidentNo());
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
    @Override public int lastSequenceOn(LocalDate date) {
        return issued.getOrDefault(Incident.numberPrefix(date), 0);
    }

    /** 행을 지워도 번호는 되돌아가지 않아야 한다. 그 성질을 검증하는 데 쓴다. */
    void deleteAll() { rows.clear(); }

    private void remember(String incidentNo) {
        String prefix = incidentNo.substring(0, Incident.NUMBER_PREFIX_LENGTH);
        int n = Integer.parseInt(incidentNo.substring(Incident.NUMBER_PREFIX_LENGTH));
        issued.merge(prefix, n, Math::max);
    }
}
