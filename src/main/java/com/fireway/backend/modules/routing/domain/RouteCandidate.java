package com.fireway.backend.modules.routing.domain;

import java.util.List;
import lombok.Setter;

@Setter
public class RouteCandidate {
    private int rank;
    private final List<double[]> coordinates;
    private final String polyline;
    private final int etaSec;
    private final double distanceM;
    private double passableProb = 1.0; // Placeholder until CCTV readings are integrated.
    private boolean meetsGoldenTime;
    private String explanation = "";
    private List<ExcludedReason> excludedReasons = List.of();

    public RouteCandidate(int rank, List<double[]> coordinates, String polyline, int etaSec, double distanceM) {
        this.rank = rank;
        this.coordinates = List.copyOf(coordinates);
        this.polyline = polyline;
        this.etaSec = etaSec;
        this.distanceM = distanceM;
    }

    public int rank() { return rank; }
    public List<double[]> coordinates() { return coordinates; }
    public String polyline() { return polyline; }
    public int etaSec() { return etaSec; }
    public double distanceM() { return distanceM; }
    public double passableProb() { return passableProb; }
    public boolean meetsGoldenTime() { return meetsGoldenTime; }
    public String explanation() { return explanation; }
    public List<ExcludedReason> excludedReasons() { return excludedReasons; }
}
