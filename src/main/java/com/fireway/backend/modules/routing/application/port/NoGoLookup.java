package com.fireway.backend.modules.routing.application.port;

import com.fireway.backend.modules.routing.domain.*;
import java.util.List;

public interface NoGoLookup {
    List<NoGoAreaSummary> forRouting(BoundingBox boundingBox);
}
