package com.fireway.backend.modules.routing.domain;

import java.util.List;

/** Routing-owned projection; the staticdata adapter will supply this contract. */
public record NoGoAreaSummary(String polygonId, String reason, String evidenceUrl,
                              List<double[]> pathAsLonLat) { }
