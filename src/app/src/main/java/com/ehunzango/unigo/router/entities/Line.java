package com.ehunzango.unigo.router.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Line {
    public enum Type { // TODO: add a speed var, so if we go by distance metrics it's just to mult (this is if we don't have exact time mettrics)
        // GTFS Route Types https://gtfs.org/documentation/schedule/reference/#routestxt
        TRAM(0),        // Streetcar, Light rail
        SUBWAY(1),      // Subway, Metro
        TRAIN(2),       // Rail
        BUS(3),         // Bus
        FERRY(4),       // Ferry
        CABLE_TRAM(5),  // Cable tram
        AERIAL_LIFT(6), // Aerial lift
        FUNICULAR(7),   // Funicular
        TROLLEYBUS(11), // Trolleybus
        MONORAIL(12),   // Monorail
        // CUSTOM
        WALK(-1),       // Tipi-Tapa
        BIKE(-3),       // Bicycle routes
        NONE(-2);       // Special purpose (e.g., virtual nodes)

        private final int gtfsCode;

        Type(int code) {
            this.gtfsCode = code;
        }

        public int getGtfsCode() { return gtfsCode; }

        // Converts a GTFS route_type integer to the corresponding Type enum
        public static Type fromGtfsCode(int code) {
            for (Type type : values()) {
                if (type.gtfsCode == code) {
                    return type;
                }
            }
            return NONE; // fallback if unknown type
        }
    }


    public final String name;
    public final Line.Type type;
    public final Map<Node, Integer> node_map = new HashMap<>();
    public final List<Node> node_list = new ArrayList<>();
    public final List<Float> deltas = new ArrayList<>();
    public final List<Date> start_times;

    public Line(String name, Line.Type type, List<Date> start_times) {
        this.name = name;
        this.type = type;
        this.start_times = start_times;
    }

    public void addNode(Node node, float delta) {
        this.node_list.add(node);
        this.node_map.put(node, this.node_map.size());
        this.deltas.add(delta);
    }

    public void addNode(List<Node> nodes, List<Float> deltas) {
        int index = this.node_list.size();
        for (Node node : nodes) {
            this.node_map.put(node, index++);
        }
        this.node_list.addAll(nodes);
        this.deltas.addAll(deltas);
    }

    public Line dup() {
        Line nLine = new Line(this.name, this.type, new ArrayList<>());
        List<Node> nodes = this.node_list.stream()
                .map(n -> n.clone(nLine))
                .collect(Collectors.toList());
        List<Float> deltas = new ArrayList<>(this.deltas);

        // pop-it, flip-it and ship-it
        deltas.remove(deltas.size() - 1); // pop Float.POSITIVE_INFINITY
        Collections.reverse(nodes);             // flip
        Collections.reverse(deltas);            // flip
        deltas.add(Float.POSITIVE_INFINITY);    // ship Float.POSITIVE_INFINITY

        nLine.addNode(nodes, deltas);

        return nLine;
    }


    @Override
    public String toString() {
        return String.format("%s[%s]", name, type.name());
    }
}