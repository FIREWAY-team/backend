package com.fireway.backend.modules.staticdata.application;
import static org.assertj.core.api.Assertions.assertThat;
import com.fireway.backend.modules.staticdata.domain.BoundingBox;
import com.fireway.backend.modules.staticdata.domain.Coordinate;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import com.fireway.backend.modules.staticdata.domain.NoGoArea.GeometryType;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoGoAreaServiceTest {
    private static NoGoArea area(long id, String status) {
        return at(id, status, 37.44, 127.15);
    }
    private static NoGoArea at(long id, String status, double lat, double lon) {
        return new NoGoArea(id, "ext-" + id, "금광1동", "소방차 진입곤란 지정", 1, status,
                status.equals(NoGoArea.UNVERIFIED) ? "지도상 미확인" : "", GeometryType.LINE_STRING,
                List.of(new Coordinate(lat, lon), new Coordinate(lat + 0.001, lon + 0.001)));
    }
    private static final NoGoArea OK1 = area(1, NoGoArea.OK);
    private static final NoGoArea GHOST = area(2, NoGoArea.UNVERIFIED);
    private static final NoGoArea OK2 = area(3, NoGoArea.OK);

    private NoGoAreaService service(NoGoArea... rows) {
        return new NoGoAreaService(() -> List.of(rows));   // 포트의 기본 필터 구현을 탄다
    }

    @Test void 지도용_목록은_전부_내려준다() {
        assertThat(service(OK1, GHOST, OK2).findAll()).containsExactly(OK1, GHOST, OK2);
    }

    // 이게 이 변경의 핵심이다. unverified 구간은 건물 위에 있어서 라우팅에 들어가면
    // 가까운 실제 진입로를 잘못 막는다.
    @Test void 라우팅용_목록에서는_미확인_구간이_빠진다() {
        assertThat(service(OK1, GHOST, OK2).forRouting()).containsExactly(OK1, OK2);
    }

    // bbox 조회도 미확인 구간을 먼저 걸러야 한다. 범위 안에 있다고 통과시키면 안 된다.
    @Test void 범위_조회에서도_미확인_구간은_빠진다() {
        BoundingBox box = new BoundingBox(37.43, 127.14, 37.45, 127.16);
        assertThat(service(OK1, GHOST, OK2).forRouting(box)).containsExactly(OK1, OK2);
    }

    @Test void 범위_밖_구간은_빠진다() {
        NoGoArea 멀리 = at(9, NoGoArea.OK, 37.50, 127.30);
        BoundingBox box = new BoundingBox(37.43, 127.14, 37.45, 127.16);
        assertThat(service(OK1, 멀리).forRouting(box)).containsExactly(OK1);
    }

    // 끝점 하나만 걸쳐도 포함한다. 선분이 범위를 스치는 경우를 놓치면 안 된다.
    @Test void 한_점만_걸쳐도_포함한다() {
        NoGoArea 걸침 = at(10, NoGoArea.OK, 37.4499, 127.1599);
        BoundingBox box = new BoundingBox(37.43, 127.14, 37.45, 127.16);
        assertThat(service(걸침).forRouting(box)).containsExactly(걸침);
    }

    @Test void 전부_미확인이면_라우팅에_아무것도_안_간다() {
        assertThat(service(GHOST).forRouting()).isEmpty();
    }

    // note 는 나중에 CCTV 판독 결과로 덮인다. 그때 필터가 뚫리면 안 되므로
    // 판정은 note 가 아니라 verification_status 로만 한다.
    @Test void note_가_바뀌어도_판정은_verification_status_를_따른다() {
        NoGoArea noteOverwritten = new NoGoArea(4, "ext-4", "금광1동", "소방차 진입곤란 지정", 1,
                NoGoArea.UNVERIFIED, "상시 점유 매대", GeometryType.LINE_STRING,
                List.of(new Coordinate(37.44, 127.15), new Coordinate(37.441, 127.151)));
        assertThat(noteOverwritten.routable()).isFalse();
        assertThat(service(noteOverwritten).forRouting()).isEmpty();
    }
}
