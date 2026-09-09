package com.fireway.backend.modules.routing.infrastructure;

import java.util.ArrayList;
import java.util.List;

/** Valhalla polyline6; public coordinate arrays always use [longitude, latitude]. */
public final class PolylineCodec {
    private PolylineCodec() { }

    public static String encode(List<double[]> coordinates) {
        StringBuilder encoded = new StringBuilder();
        long lat = 0, lon = 0;
        for (double[] point : coordinates) {
            long nextLat = Math.round(point[1] * 1_000_000), nextLon = Math.round(point[0] * 1_000_000);
            append(encoded, nextLat - lat);
            append(encoded, nextLon - lon);
            lat = nextLat;
            lon = nextLon;
        }
        return encoded.toString();
    }

    private static void append(StringBuilder encoded, long delta) {
        long value = delta < 0 ? ~(delta << 1) : delta << 1;
        while (value >= 0x20) {
            encoded.append((char) ((0x20 | (value & 0x1f)) + 63));
            value >>= 5;
        }
        encoded.append((char) (value + 63));
    }

    public static List<double[]> decode(String shape) {
        List<double[]> coordinates = new ArrayList<>();
        int[] cursor = {0};
        long lat = 0, lon = 0;
        while (cursor[0] < shape.length()) {
            lat += read(shape, cursor);
            lon += read(shape, cursor);
            coordinates.add(new double[]{lon / 1_000_000.0, lat / 1_000_000.0});
        }
        return coordinates;
    }

    private static long read(String shape, int[] cursor) {
        long value = 0;
        int shift = 0, chunk;
        do {
            if (cursor[0] >= shape.length() || shift > 30) throw new IllegalArgumentException("Invalid polyline6");
            chunk = shape.charAt(cursor[0]++) - 63;
            if (chunk < 0 || chunk > 63) throw new IllegalArgumentException("Invalid polyline6 character");
            value |= (long) (chunk & 0x1f) << shift;
            shift += 5;
        } while (chunk >= 0x20);
        return (value & 1) != 0 ? ~(value >> 1) : value >> 1;
    }
}
