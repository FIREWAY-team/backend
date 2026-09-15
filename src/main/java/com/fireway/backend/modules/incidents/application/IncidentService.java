package com.fireway.backend.modules.incidents.application;
import com.fireway.backend.modules.incidents.application.port.IncidentRepository;
import com.fireway.backend.modules.incidents.domain.Incident;
import com.fireway.backend.modules.incidents.domain.IncidentStatus;
import com.fireway.backend.shared.exception.NotFoundException;
import com.fireway.backend.shared.exception.ValidationException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service public class IncidentService {
    /** 같은 날 접수번호 충돌은 유니크 키가 막는다. 그 경우 번호만 다시 따서 재시도한다. */
    private static final int NUMBERING_RETRIES = 3;
    // 관할 확인이 아니라 "좌표가 뒤바뀌었는가"를 잡는 검사다. 경기 남부를 넉넉히 덮는 범위로
    // 두고, 위경도를 바꿔 넣으면(위도 자리에 127이 오면) 반드시 걸리게만 한다.
    // 인접 구 공조 출동까지 막으면 안 되므로 중원구로 좁히지 않는다.
    private static final double MIN_LAT = 36.9, MAX_LAT = 37.9, MIN_LON = 126.5, MAX_LON = 127.6;

    private final IncidentRepository repository;
    private final Clock clock;

    public IncidentService(IncidentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Incident receive(String address, double lat, double lon, String summary) {
        if (lat < MIN_LAT || lat > MAX_LAT || lon < MIN_LON || lon > MAX_LON) {
            throw new ValidationException(
                    "좌표가 관할 권역을 크게 벗어납니다: lat=%f, lon=%f — 위경도가 뒤바뀌지 않았는지 확인하세요."
                            .formatted(lat, lon));
        }
        LocalDateTime now = LocalDateTime.now(clock);
        DuplicateKeyException last = null;
        for (int attempt = 0; attempt < NUMBERING_RETRIES; attempt++) {
            Incident draft = Incident.received(nextNumber(now.toLocalDate()), address, lat, lon, summary, now);
            try {
                return draft.withId(repository.insert(draft));
            } catch (DuplicateKeyException e) {
                last = e;   // 동시 접수로 번호가 겹쳤다. 다시 센다.
            }
        }
        throw new IllegalStateException("접수번호를 배정하지 못했습니다", last);
    }

    /** 2026-0915-0001 형식. 그날 몇 번째 신고인지 한눈에 보이게 한다. */
    private String nextNumber(LocalDate date) {
        return "%d-%02d%02d-%04d".formatted(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                repository.countReceivedOn(date) + 1);
    }

    /** status 가 null 이면 전체. 접수 최신순. */
    public List<Incident> list(IncidentStatus status) { return repository.findAll(status); }

    public Incident get(String incidentNo) {
        return repository.findByNo(incidentNo)
                .orElseThrow(() -> new NotFoundException("신고를 찾을 수 없습니다: " + incidentNo));
    }
}
