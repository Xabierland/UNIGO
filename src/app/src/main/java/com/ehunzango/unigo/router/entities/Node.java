package com.ehunzango.unigo.router.entities;

public class Node {

    public double x;
    public double y;
    public Line line;

    public Node(double latitude, double longitude, Line line) {
        this.x = latitude;
        this.y = longitude;
        this.line = line;
    }
    public Node clone() {
        return new Node(this.x, this.y, this.line);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return Double.compare(node.x, x) == 0 &&
                Double.compare(node.y, y) == 0 &&
                line.equals(node.line);
    }

    @Override
    public int hashCode() {
        return (int) (this.x * 17 + this.y * 7 + this.line.hashCode() * 3);
    }

    @Override
    public String toString() {
        return String.format("%s(%.2f, %.2f)", line.toString(), x, y);
    }

    // [stackoverflow](https://stackoverflow.com/questions/639695/how-to-convert-latitude-or-longitude-to-meters)
    public double dist(Node other) {
        var R = 6378.137; // Radius of earth in KM
        var dLat = other.x * Math.PI / 180 - this.x * Math.PI / 180;
        var dLon = other.y * Math.PI / 180 - this.y * Math.PI / 180;
        var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(this.x * Math.PI / 180) * Math.cos(other.x * Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        var d = R * c;
        return d * 1000; // meters
    }

    // NOTE, we can remove the "* METERS_PER_DEGREE", if we just want a fast heuristic, and not realistic values
    private static final double EARTH_EQUATOR_LEN = 40_075_000.0; // m
    private static final double EARTH_CIRCUMFERENCE_PERIMETER = EARTH_EQUATOR_LEN;
    private static final double WEIGHT = 0.98;
    private static final double METERS_PER_DEGREE = EARTH_CIRCUMFERENCE_PERIMETER / 360 * WEIGHT;
    public double fast_euclides(Node other) {
        double X, Y;
        X = (this.x - other.x);
        Y = (this.y - other.y);
        return Math.sqrt(X * X + Y * Y) * METERS_PER_DEGREE;
    }
    public double fast_manhattan(Node other) {
        return Math.abs(this.x - other.x + this.y - other.y) * METERS_PER_DEGREE;
    }

    public boolean isSameCordsAs(Node goal) {
        return this.x == goal.x && this.y == goal.y;
    }
}
