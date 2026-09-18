package com.fireway.backend.modules.staticdata.application;
import com.fireway.backend.modules.staticdata.application.port.NoGoAreaRepository;
import com.fireway.backend.modules.staticdata.domain.BoundingBox;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import java.util.List;
import org.springframework.stereotype.Service;
@Service public class NoGoAreaService {
    private final NoGoAreaRepository repository;
    public NoGoAreaService(NoGoAreaRepository repository) { this.repository = repository; }

    /** 지도 표시용. unverified 도 포함해서 전부 내려준다. */
    public List<NoGoArea> findAll() { return repository.findAll(); }

    /**
     * 라우팅 주입용. verification_status 가 ok 인 것만.
     * unverified 구간은 건물 위에 그려져 있어서, 가까운 도로에 스냅되면
     * 실제 진입로를 잘못 막는다. 그래서 라우팅에는 넣지 않는다.
     */
    public List<NoGoArea> forRouting() { return repository.findRoutable(); }

    /**
     * 라우팅 주입용 중 범위 안의 것만. 중원구 전체가 1,274건이라 경로 하나 뽑는데
     * 전건을 넘길 이유가 없다. 호출 쪽이 경로를 감싸는 bbox 를 준다.
     */
    public List<NoGoArea> forRouting(BoundingBox box) { return repository.findRoutableWithin(box); }

    /**
     * 지도 표시용 중 화면 범위 안의 것만. 전건이 1,276건이라 지도를 움직일 때마다
     * 전부 내려줄 이유가 없다. forRouting(box) 와 달리 unverified 도 포함한다.
     */
    public List<NoGoArea> findAll(BoundingBox box) { return repository.findAllWithin(box); }
}
