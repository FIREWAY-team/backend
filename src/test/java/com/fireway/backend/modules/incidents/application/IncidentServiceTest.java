package com.fireway.backend.modules.incidents.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fireway.backend.modules.incidents.domain.Incident;
import com.fireway.backend.modules.incidents.domain.IncidentStatus;
import com.fireway.backend.shared.exception.ConflictException;
import com.fireway.backend.shared.exception.NotFoundException;
import com.fireway.backend.shared.exception.ValidationException;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IncidentServiceTest {
    // 은행1동 일대. 시계를 고정해 접수번호를 결정적으로 만든다.
    private static final double LAT = 37.4381, LON = 127.1422;
    private static final Clock 고정시계 =
            Clock.fixed(Instant.parse("2026-09-15T05:30:00Z"), ZoneId.of("Asia/Seoul"));

    private FakeIncidentRepository repo;
    private IncidentService service;

    @BeforeEach void setUp() {
        repo = new FakeIncidentRepository();
        service = new IncidentService(repo, 고정시계);
    }

    @Test void 접수하면_번호가_붙고_RECEIVED_로_시작한다() {
        Incident i = service.receive("성남시 중원구 은행로 12-3", LAT, LON, "주택 화재");
        assertThat(i.incidentNo()).isEqualTo("2026-0915-0001");
        assertThat(i.status()).isEqualTo(IncidentStatus.RECEIVED);
        assertThat(i.id()).isPositive();
        assertThat(i.closedAt()).isNull();
    }

    @Test void 같은_날_접수는_일련번호가_올라간다() {
        service.receive("주소1", LAT, LON, null);
        service.receive("주소2", LAT, LON, null);
        assertThat(service.receive("주소3", LAT, LON, null).incidentNo()).isEqualTo("2026-0915-0003");
    }

    // 동시 접수로 번호가 겹치면 유니크 키가 막는다. 그때 번호만 다시 따서 넘어가야 한다.
    @Test void 번호가_겹치면_다시_따서_저장한다() {
        repo.failNextInserts = 2;
        assertThat(service.receive("주소", LAT, LON, null).id()).isPositive();
    }

    /**
     * 위경도를 뒤바꿔 넣는 실수가 이 프로젝트에서 반복됐다.
     * (127.14, 37.43) 으로 들어오면 위도가 127 이 되어 범위를 벗어난다.
     */
    @Test void 위경도가_뒤바뀌면_거부한다() {
        assertThatThrownBy(() -> service.receive("주소", LON, LAT, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("뒤바뀌지");
    }

    @Test void 권역을_크게_벗어난_좌표는_거부한다() {
        assertThatThrownBy(() -> service.receive("부산시청", 35.1798, 129.0750, null))
                .isInstanceOf(ValidationException.class);
    }

    // 인접 구 공조 출동을 막으면 안 된다. 중원구가 아니어도 경기 남부면 통과해야 한다.
    @Test void 인접_지역은_막지_않는다() {
        assertThat(service.receive("수원시청", 37.2636, 127.0286, null).id()).isPositive();
    }

    @Test void 상태로_거를_수_있고_null_이면_전체다() {
        service.receive("주소1", LAT, LON, null);
        service.receive("주소2", LAT, LON, null);
        assertThat(service.list(null)).hasSize(2);
        assertThat(service.list(IncidentStatus.RECEIVED)).hasSize(2);
        assertThat(service.list(IncidentStatus.CLOSED)).isEmpty();
    }

    @Test void 접수번호로_조회한다() {
        String no = service.receive("성남시 중원구 은행로 12-3", LAT, LON, "주택 화재").incidentNo();
        assertThat(service.get(no).address()).isEqualTo("성남시 중원구 은행로 12-3");
    }

    @Test void 없는_번호는_404_로_이어진다() {
        assertThatThrownBy(() -> service.get("2026-0101-9999"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test void 요약이_없으면_빈_문자열로_저장한다() {
        assertThat(service.receive("주소", LAT, LON, null).summary()).isEmpty();
    }

    /**
     * 예전 채번은 그날 건수를 세서 +1 했다. 행이 지워지면 건수가 뒤로 돌아가 이미 쓴 번호를
     * 다시 발급했고, 재시도해도 같은 번호만 나와 그날 접수가 통째로 막혔다.
     */
    @Test void 지운_뒤_접수해도_번호가_뒤로_돌아가지_않는다() {
        service.receive("주소1", LAT, LON, null);
        service.receive("주소2", LAT, LON, null);
        repo.deleteAll();
        assertThat(service.receive("주소3", LAT, LON, null).incidentNo()).isEqualTo("2026-0915-0003");
    }

    // 재시도가 다 떨어지는 건 동시 접수가 몰린 것이지 서버가 고장난 게 아니다. 500 이 아니라 409 다.
    @Test void 번호_재시도가_소진되면_409_로_이어진다() {
        repo.failNextInserts = 99;
        assertThatThrownBy(() -> service.receive("주소", LAT, LON, null))
                .isInstanceOf(ConflictException.class);
    }
}
