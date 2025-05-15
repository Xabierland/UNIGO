package com.ehunzango.unigo.router;

import com.ehunzango.unigo.router.entities.Line;
import com.ehunzango.unigo.router.entities.Node;
import com.ehunzango.unigo.router.utils.DistanceCache;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

// NOTE: this will solve for public transport data, no walk or car transports, those are going to be
//       done by google API calls, sad stuff :(
public class RouteFinder {
    // NOTE: this is just bs, it can be improved by fetching to google the stuff (we could even,
    //       fetch all the walks between nodes and make it "independent" from fetching stuff for
    //       public transport data)
    private final static float WALK_SPEED = 1.0f / 1.3f; // seconds / meters

    private final DistanceCache distanceCache = new DistanceCache();

    /*
     * TODO:
     *   add:
     *     - load data from GTFS
     *     - add more data to the calc (line timings, walk speep is a little bit sus...)
     *     - take into account that some lines may be filtered in hollidays, or by the user (this can be done previously)
     *     - do ghost walk or make google API calls?
     *     - do we store data of previous routes?
     *     - add priority to staying in the same line.
     *     - remember to split lines in 2 when they are bidirectional (all the cool kids do this)
     *     - go to church.
     *   test:
     *     - validity of results
     *     - speed in calc, test some diferent heuristics, cache etc...
     */

    // FUTURE: We could make goal a list of goal, and precompute them due to our problem domain:
    //    - From any point inside a bounded area go to a near point from a location chosen
    //    from list of limited locations.
    public List<Node> findShortestPath(Node start, Node goal, List<Line> allLines) {
        PriorityQueue<Step> queue = new PriorityQueue<>(Comparator.comparingDouble(s -> s.cost));
        Map<Node, Float> dist = new HashMap<>();
        Map<Node, Node> prev = new HashMap<>();

        dist.put(start, 0.0f);
        queue.add(new Step(start, 0.0f));

        while (!queue.isEmpty()) {
            Step current = queue.poll();
            System.out.println(current);
            Node currNode = current.node;

            if (current.cost > dist.getOrDefault(currNode, Float.POSITIVE_INFINITY)) { continue; }

            if (currNode.isSameCordsAs(goal)) { return reconstructPath(prev, goal); }

            Integer index = currNode.line.node_map.get(currNode);

            if (index == null) {
                continue;
            }

            // 1. Move forward on the same line
            if (index + 1 < currNode.line.node_list.size()) {
                // NOTE: remember that bidirectional lines are splited into 2 individual lines :)
                Node next = currNode.line.node_list.get(index + 1);
                float delta = currNode.line.deltas.get(index); // cost from curr -> next
                float heuristic = (float)next.fast_manhattan(goal); // hcost from next -> goal
                relax(queue, dist, prev, currNode, next, delta + heuristic);
            }

            // 2. Transfers: look for the nearest cords in different lines
            for (Line line : allLines) {
                if (line == currNode.line) continue;

                float min_distance = Float.POSITIVE_INFINITY;
                Node node = null;
                for (Node n : line.node_list) {
                    float d = distanceCache.getDistance(currNode, n);
                    if (d < min_distance) {
                        min_distance = d;
                        node = n;
                        if (d == 0.0f) break;
                    }
                }

                // TODO: take into account when is going to come the next line and add the
                //       wait time.
                float transferPenalty;
                if (min_distance == 0) {
                    transferPenalty = 0.0f;
                } else {
                    // TODO: we need to check the walk distance to the point and check if we will be
                    //       able to get the incoming bus, train... Or we need to add even more time
                    transferPenalty = min_distance * WALK_SPEED;
                }

                if (node != null) {
                    float heuristic = (float)node.fast_manhattan(goal); // hcost from next -> goal
                    relax(queue, dist, prev, currNode, node, transferPenalty + heuristic);
                }
            }

            // 3. Walk: go from where we are to the destination
            relax(queue, dist, prev, currNode, goal, (float)currNode.fast_manhattan(goal));
        }

        return null; // No path found
    }

    private void relax(PriorityQueue<Step> queue, Map<Node, Float> dist, Map<Node, Node> prev,
                       Node from, Node to, float cost) {
        float newDist = dist.get(from) + cost;
        if (!dist.containsKey(to) || newDist < dist.get(to)) {
            dist.put(to, newDist);
            prev.put(to, from);
            queue.add(new Step(to, newDist));
        }
    }

    private List<Node> reconstructPath(Map<Node, Node> prev, Node goal) {
        LinkedList<Node> path = new LinkedList<>();
        System.out.println("------------------------------------------");
        for (Node n : prev.keySet()) {
            System.out.printf("%s -> %s\n", prev.get(n), n);
        }
        System.out.println("------------------------------------------");
        Line walkLine = new Line("Tipi-Tapa", Line.Type.WALK, List.of());
        Node p = null;
        for (Node at = goal; at != null; at = prev.get(at)) {
            System.out.println(at);
            if (p != null && p.line != at.line && !at.isSameCordsAs(path.getFirst())) {
                Node walk = at.clone();
                walk.line = walkLine;
                path.addFirst(walk);
            }
            path.addFirst(at);
            p = at;
        }
        return path;
    }

    public static class Step {
        public final Node node;
        public final float cost;

        public Step(Node node, float cost) {
            this.node = node;
            this.cost = cost;
        }

        @Override
        public String toString() {
            return String.format("Step[%04.2f -> %s]", cost, node.toString());
        }
    }
}
