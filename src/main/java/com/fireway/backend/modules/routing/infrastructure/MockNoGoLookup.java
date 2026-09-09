package com.fireway.backend.modules.routing.infrastructure;

import com.fireway.backend.modules.routing.application.port.NoGoLookup;
import com.fireway.backend.modules.routing.domain.*;
import java.util.List;
import org.springframework.stereotype.Component;

/** TODO: Replace after staticdata forRouting() is merged; CCTV integration is also pending. */
@Component
public class MockNoGoLookup implements NoGoLookup {
    @Override
    public List<NoGoAreaSummary> forRouting(BoundingBox boundingBox) { return List.of(); }
}
