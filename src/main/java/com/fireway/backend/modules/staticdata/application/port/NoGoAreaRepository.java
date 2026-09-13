package com.fireway.backend.modules.staticdata.application.port;
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
}
