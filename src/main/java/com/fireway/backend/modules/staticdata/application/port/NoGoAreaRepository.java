package com.fireway.backend.modules.staticdata.application.port;
import com.fireway.backend.modules.staticdata.domain.BoundingBox;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import java.util.List;
public interface NoGoAreaRepository {
    List<NoGoArea> findAll();

    /**
     * 라우팅이 막아도 되는 구간만. 기본 구현은 메모리에서 거른다(테스트 스텁용).
     * 실제 어댑터는 SQL 로 걸러서 V2_3 의 인덱스를 탄다.
     */
    default List<NoGoArea> findRoutable() {
        return findAll().stream().filter(NoGoArea::routable).toList();
    }

    /**
     * 범위 안의, 라우팅이 막아도 되는 구간만. 경로 주변만 필요한 쪽이 쓴다.
     * 기본 구현은 메모리에서 거르고, 실제 어댑터는 공간 인덱스를 탄다.
     * 구간의 어느 한 점이라도 범위에 걸치면 포함한다(선분이 범위를 관통하는 경우까지).
     */
    default List<NoGoArea> findRoutableWithin(BoundingBox box) {
        return findRoutable().stream()
                .filter(area -> area.path().stream().anyMatch(box::contains))
                .toList();
    }
}
