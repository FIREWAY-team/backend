package com.fireway.backend.modules.routing.domain;

public record BoundingBox(Coordinate southWest, Coordinate northEast) {
    public static BoundingBox around(Coordinate from, Coordinate to, double paddingM) {
        double latPadding = paddingM / 111_320.0;
        double maxLatitude = Math.max(Math.abs(from.lat()), Math.abs(to.lat()));
        double lonPadding = Math.min(180, latPadding / Math.max(0.000001, Math.cos(Math.toRadians(maxLatitude))));
        return new BoundingBox(
                new Coordinate(Math.max(-90, Math.min(from.lat(), to.lat()) - latPadding),
                        Math.max(-180, Math.min(from.lon(), to.lon()) - lonPadding)),
                new Coordinate(Math.min(90, Math.max(from.lat(), to.lat()) + latPadding),
                        Math.min(180, Math.max(from.lon(), to.lon()) + lonPadding)));
    }
}
