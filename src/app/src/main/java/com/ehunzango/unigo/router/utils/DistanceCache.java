package com.ehunzango.unigo.router.utils;


import com.ehunzango.unigo.router.entities.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DistanceCache {
    private final Map<Key, Float> cache = new HashMap<>();

    public float getDistance(Node a, Node b) {
        return cache.computeIfAbsent(new Key(a, b), k -> (float) a.dist(b));
    }

    private static class Key {
        private final double Ax;
        private final double Ay;
        private final double Bx;
        private final double By;

        public Key(Node a, Node b) {
            // Make the key symmetric: (a, b) == (b, a)
            if (a.x * 3 + a.y * 7 < b.x * 3 + b.y * 7) {
                this.Ax = a.x;
                this.Ay = a.y;
                this.Bx = b.x;
                this.By = b.y;
            } else {
                this.Ax = b.x;
                this.Ay = b.y;
                this.Bx = a.x;
                this.By = a.y;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return key.Ax == Ax && key.Ay == Ay && key.Bx == Bx && key.By == By;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Ax, Ay, Bx, By);
        }
    }
}
