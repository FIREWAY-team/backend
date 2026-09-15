package com.fireway.backend.modules.routing.application;

import java.util.Arrays;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/** Local metric projection for Seongnam road intersection checks. */
public final class RouteGeometry {
    private static final GeometryFactory GEOMETRY = new GeometryFactory();
    private static final double X = 111_320 * Math.cos(Math.toRadians(37.44));
    private static final double Y = 111_320;
    private static final double ROAD_MARGIN_M = 6;
    private RouteGeometry() {}

    private static Geometry geometry(List<double[]> path) {
        Coordinate[] coordinates = path.stream().map(p -> new Coordinate(p[0] * X, p[1] * Y))
                .toArray(Coordinate[]::new);
        if (coordinates.length == 1) return GEOMETRY.createPoint(coordinates[0]);
        if (coordinates.length >= 4 && coordinates[0].equals2D(coordinates[coordinates.length - 1]))
            return GEOMETRY.createPolygon(coordinates);
        return GEOMETRY.createLineString(coordinates);
    }

    public static boolean touches(List<double[]> route, List<double[]> road) {
        return route != null && route.size() >= 2 && road != null && !road.isEmpty()
                && GEOMETRY.createLineString(route.stream().map(p -> new Coordinate(p[0] * X, p[1] * Y)).toArray(Coordinate[]::new)).isWithinDistance(geometry(road), ROAD_MARGIN_M);
    }

    /** Valhalla expects polygon rings, including when static data is a road centerline. */
    public static List<double[]> exclusionRing(List<double[]> road) {
        return Arrays.stream(geometry(road).buffer(ROAD_MARGIN_M).getCoordinates())
                .map(c -> new double[]{c.x / X, c.y / Y}).toList();
    }
}
