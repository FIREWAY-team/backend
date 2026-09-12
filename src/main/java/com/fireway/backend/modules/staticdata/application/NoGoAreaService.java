package com.fireway.backend.modules.staticdata.application;
import com.fireway.backend.modules.staticdata.application.port.NoGoAreaRepository;
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
}
