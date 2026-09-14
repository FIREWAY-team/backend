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
    private double passableProb = 1.0; // 3-layer decision: raised by CCTV PASS, lowered by unresolved static no-go.
    private boolean meetsGoldenTime;
    // 3-layer decision output: does the current vehicle actually fit through every un-unlocked no-go
    // that this route touches? Default true — the planner flips this to false when a blocking no-go
    // has no CCTV PASS for the dispatched vehicle.
    private boolean passableForVehicle = true;
    // CCTV frame ids that unlocked at least one static no-go on this route. Empty when the route
    // stays clear of static no-gos entirely, or when no CCTV verdict exists near any touched polygon.
    private List<String> unlockedByCctv = List.of();
    // Route touched at least one static no-go for which we have no CCTV verdict at all — carries
    // planner's uncertainty forward to the UI (yellow badge candidate).
    private boolean hasUnresolvedStaticNoGo;
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
    public boolean passableForVehicle() { return passableForVehicle; }
    public List<String> unlockedByCctv() { return unlockedByCctv; }
    public boolean hasUnresolvedStaticNoGo() { return hasUnresolvedStaticNoGo; }
    public String explanation() { return explanation; }
    public List<ExcludedReason> excludedReasons() { return excludedReasons; }
}
