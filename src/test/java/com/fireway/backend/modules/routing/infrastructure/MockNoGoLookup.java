package com.fireway.backend.modules.routing.infrastructure;

import com.fireway.backend.modules.routing.application.port.NoGoLookup;
import com.fireway.backend.modules.routing.domain.*;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 테스트 전용 스텁. 실제 구현은 StaticDataNoGoLookup 이다.
 * main 에 두면 @Component 가 둘이 되어 NoGoLookup 빈이 모호해진다.
 */
@Component
public class MockNoGoLookup implements NoGoLookup {
    @Override
    public List<NoGoAreaSummary> forRouting(BoundingBox boundingBox) { return List.of(); }
}
