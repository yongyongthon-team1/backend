package com.uniro.backend.domain.store.support;

// 좌표 계산 유틸 (거리, 반경을 감싸는 사각형)
public final class Geo {

    private static final double EARTH_RADIUS_M = 6_371_000;
    private static final double METERS_PER_DEGREE_LAT = 111_320;

    private Geo() {
    }

    public static Origin origin(double lat, double lng) {
        double latRad = Math.toRadians(lat);

        return new Origin(lat, lng, latRad, Math.cos(latRad));
    }

    // 기준점. 위도의 라디안 값과 cos 값을 한 번만 계산해 두고 여러 점까지의 거리에 쓴다.
    public record Origin(double lat, double lng, double latRad, double cosLat) {

        // 기준점에서 다른 점까지의 거리(미터, Haversine)
        public double distanceTo(double otherLat, double otherLng) {
            double otherLatRad = Math.toRadians(otherLat);
            double dLat = otherLatRad - latRad;
            double dLng = Math.toRadians(otherLng - lng);
            double sinLat = Math.sin(dLat / 2);
            double sinLng = Math.sin(dLng / 2);
            double a = sinLat * sinLat
                    + cosLat * Math.cos(otherLatRad) * sinLng * sinLng;

            return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a));
        }

        // 반경(미터)을 감싸는 사각형. 정확한 거리는 distanceTo로 다시 거른다.
        public Box box(double radiusMeters) {
            double deltaLat = radiusMeters / METERS_PER_DEGREE_LAT;
            double deltaLng = deltaLat / cosLat;

            return new Box(
                    lat - deltaLat, lat + deltaLat,
                    lng - deltaLng, lng + deltaLng
            );
        }
    }

    public record Box(double minLat, double maxLat, double minLng, double maxLng) {
    }
}
