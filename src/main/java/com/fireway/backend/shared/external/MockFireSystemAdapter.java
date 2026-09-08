package com.fireway.backend.shared.external;
import org.springframework.stereotype.Component;
@Component public class MockFireSystemAdapter implements FireSystemPort { public String findNearestStation(double lat, double lon) { return "station-mock-01"; } }
