package com.fireway.backend.modules.incidents.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fireway.backend.modules.incidents.application.port.IncidentRouteRepository;
import com.fireway.backend.modules.incidents.application.port.RoutePlanning;
import com.fireway.backend.modules.incidents.domain.Incident;
import com.fireway.backend.modules.incidents.domain.IncidentRoute;
import com.fireway.backend.shared.exception.NotFoundException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IncidentRouteServiceTest {
    private static final Clock 고정시계 =
            Clock.fixed(Instant.parse("2026-09-15T05:30:00Z"), ZoneId.of("Asia/Seoul"));
    private static final double LAT = 37.4381, LON = 127.1422;

    /** 메모리 저장소. replaceAll 이 정말 갈아끼우는지 보려면 상태가 있어야 한다. */
    private static final class Routes implements IncidentRouteRepository {
        final Map<Long, List<IncidentRoute>> byIncident = new HashMap<>();
        int replaceCalls = 0;
        public void replaceAll(long incidentId, List<IncidentRoute> routes) {
            replaceCalls++;
            List<IncidentRoute> stored = new ArrayList<>();
            for (IncidentRoute r : routes) {
                stored.add(new IncidentRoute(stored.size() + 1, incidentId, r.vehicleId(), r.rank(),
                        r.distanceM(), r.etaSeconds(), r.polyline(), r.passableForVehicle(),
                        r.meetsGoldenTime(), r.passableProb(), r.explanation(),
                        r.excludedReasons(), r.unlockedByCctv(), LocalDateTime.now(고정시계)));
            }
            byIncident.put(incidentId, stored);
        }
        public List<IncidentRoute> findByIncidentId(long id) {
            return byIncident.getOrDefault(id, List.of());
        }
    }

    private static IncidentRoute candidate(int rank, String vehicleId, boolean passable, String... blockedIds) {
        List<IncidentRoute.Blocked> blocked = Arrays.stream(blockedIds)
                .map(b -> new IncidentRoute.Blocked(b, "소방차 진입곤란 지정", null)).toList();
        return new IncidentRoute(0, 0, vehicleId, rank, 1840, 420, "polyline",
                passable, rank == 1, 0.9, "설명", blocked, List.of(), null);
    }

    private FakeIncidentRepository incidentRepo;
    private Routes routeRepo;
    private IncidentService incidents;
    private String no;
    private List<IncidentRoute> planned;

    @BeforeEach void setUp() {
        incidentRepo = new FakeIncidentRepository();
        routeRepo = new Routes();
        incidents = new IncidentService(incidentRepo, 고정시계);
        no = incidents.receive("성남시 중원구 은행로 12-3", LAT, LON, "주택 화재").incidentNo();
        planned = List.of(candidate(1, "pump-3.5", true), candidate(2, "pump-3.5", true));
    }

    private IncidentRouteService service(RoutePlanning planner) {
        return new IncidentRouteService(incidents, planner, routeRepo);
    }

    @Test void 산출한_경로를_저장하고_돌려준다() {
        var out = service((v, fl, fo, tl, to) -> planned).planAndSave(no, "pump-3.5", 37.4415, 127.1432);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).rank()).isEqualTo(1);
        assertThat(out.get(0).incidentId()).isPositive();
    }

    /** 도착지는 신고 좌표여야 한다. 출발지만 받는 계약이 지켜지는지 본다. */
    @Test void 도착지는_신고_좌표를_쓴다() {
        double[] seen = new double[2];
        service((v, fl, fo, tl, to) -> { seen[0] = tl; seen[1] = to; return planned; })
                .planAndSave(no, "pump-3.5", 37.4415, 127.1432);
        assertThat(seen[0]).isEqualTo(LAT);
        assertThat(seen[1]).isEqualTo(LON);
    }

    // 차량이 바뀌면 판정이 통째로 바뀐다. 이전 결과가 남아 섞이면 안 된다.
    @Test void 다시_산출하면_이전_결과를_대체한다() {
        var svc = service((v, fl, fo, tl, to) ->
                v.equals("pump-8") ? List.of(candidate(1, "pump-8", false, "ext-1")) : planned);
        svc.planAndSave(no, "pump-3.5", 37.4415, 127.1432);
        var out = svc.planAndSave(no, "pump-8", 37.4415, 127.1432);
        assertThat(routeRepo.replaceCalls).isEqualTo(2);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).vehicleId()).isEqualTo("pump-8");
    }

    /** 이 기능의 존재 이유. 막힌 구간과 사유가 저장돼야 나중에 설명할 수 있다. */
    @Test void 회피_근거가_보존된다() {
        var out = service((v, fl, fo, tl, to) ->
                List.of(candidate(1, "pump-8", false, "geumgwang1-impassable-003", "ext-9")))
                .planAndSave(no, "pump-8", 37.4415, 127.1432);
        assertThat(out.get(0).excludedReasons())
                .extracting(IncidentRoute.Blocked::polygonId)
                .containsExactly("geumgwang1-impassable-003", "ext-9");
        assertThat(out.get(0).excludedReasons().get(0).reason()).isEqualTo("소방차 진입곤란 지정");
    }

    // 1순위여도 차가 못 지나가면 권장이 아니다.
    @Test void 통과_못_하면_권장이_아니다() {
        var out = service((v, fl, fo, tl, to) -> List.of(candidate(1, "pump-8", false, "ext-1")))
                .planAndSave(no, "pump-8", 37.4415, 127.1432);
        assertThat(out.get(0).recommended()).isFalse();
    }

    @Test void 최우선_후보가_통과하면_권장이다() {
        var out = service((v, fl, fo, tl, to) -> planned).planAndSave(no, "pump-3.5", 37.4415, 127.1432);
        assertThat(out.get(0).recommended()).isTrue();
        assertThat(out.get(1).recommended()).isFalse();
    }

    @Test void 없는_신고면_404_이고_계산도_안_한다() {
        boolean[] called = { false };
        assertThatThrownBy(() -> service((v, fl, fo, tl, to) -> { called[0] = true; return planned; })
                .planAndSave("2026-0101-9999", "pump-3.5", 37.4415, 127.1432))
                .isInstanceOf(NotFoundException.class);
        assertThat(called[0]).isFalse();
    }

    @Test void 산출_전에는_빈_목록이다() {
        assertThat(service((v, fl, fo, tl, to) -> planned).findFor(no)).isEmpty();
    }
}
