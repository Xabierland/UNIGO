package com.ehunzango.unigo.router.utils;

/**
 * NOTE: NO IDEA HOW THIS BS WORKS:
 * src: https://gis.stackexchange.com/questions/147425/formula-to-convert-from-wgs-84-utm-zone-34n-to-wgs-84
 */
public class UTMToWGS84 {
    // Constants for WGS84
    private static final double a = 6378137.0;
    private static final double f = 1 / 298.257223563;
    private static final double k0 = 0.9996;
    private static final double drad = Math.PI / 180.0;

    // Derived constants
    private static final double b = a * (1 - f);
    private static final double esq = (1 - (b * b) / (a * a));
    private static final double e = Math.sqrt(esq);
    private static final double e_ = Math.sqrt(1 - e * e);
    private static final double e0 = e / e_;
    private static final double e0sq = e * e / (1 - e * e);

    public static double[] convert(double x, double y, int zone, boolean isNorthernHemisphere) {
        if (x < 160000 || x > 840000 || y < 0 || y > 10000000) {
            throw new IllegalArgumentException("Invalid UTM coordinates");
        }

        double zcm = 3 + 6 * (zone - 1) - 180; // Central meridian
        double e1 = (1 - e_) / (1 + e_);
        double M0 = 0;

        double M = isNorthernHemisphere ? (M0 + y / k0) : (M0 + (y - 10000000) / k0);
        double mu = M / (a * (1 - esq * (1.0 / 4 + esq * (3.0 / 64 + 5 * esq / 256))));

        double ee1 = e1 * e1;
        double eee1 = ee1 * e1;
        double phi1 = mu
                + e1 * (3.0 / 2 - 27 * ee1 / 32) * Math.sin(2 * mu)
                + ee1 * (21.0 / 16 - 55 * ee1 / 32) * Math.sin(4 * mu)
                + eee1 * (151.0 / 96) * Math.sin(6 * mu)
                + e1 * (1097.0 / 512) * Math.sin(8 * mu);

        double C1 = e0sq * Math.pow(Math.cos(phi1), 2);
        double T1 = Math.pow(Math.tan(phi1), 2);
        double a1 = 1 - Math.pow(e * Math.sin(phi1), 2);
        double N1 = a / Math.sqrt(a1);
        double R1 = N1 * (1 - e * e) / (a1);
        double D = (x - 500000) / (N1 * k0);

        double phi = (D * D) * (0.5 - D * D * (5 + 3 * T1 + 10 * C1 - 4 * C1 * C1 - 9 * e0sq) / 24);
        phi = phi + Math.pow(D, 6) * (61 + 90 * T1 + 298 * C1 + 45 * T1 * T1 - 252 * e0sq - 3 * C1 * C1) / 720;
        phi = phi1 - (N1 * Math.tan(phi1) / R1) * phi;

        double outLat = Math.floor(1000000 * phi / drad) / 1000000;

        double lng = D * (1 + D * D * ((-1 - 2 * T1 - C1) / 6 + D * D * (5 - 2 * C1 + 28 * T1 - 3 * C1 * C1 + 8 * e0sq + 24 * T1 * T1) / 120)) / Math.cos(phi1);
        double lngd = zcm + lng / drad;

        // Output Longitude:
        double outLon = Math.floor(1000000 * lngd) / 1000000;

        return new double[]{outLon, outLat};
    }
}
