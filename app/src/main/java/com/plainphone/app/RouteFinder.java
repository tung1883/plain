package com.plainphone.app;

import org.maplibre.android.geometry.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/** A* shortest-path search over a {@link RoadGraph}. Must run off the UI thread — a graph with
 *  tens of thousands of nodes/edges makes this real CPU work, not something to do inline from a
 *  tap handler. */
final class RouteFinder {

    private RouteFinder() {}

    static final class Result {
        final List<LatLng> points;
        final double distanceMeters;

        Result(List<LatLng> points, double distanceMeters) {
            this.points = points;
            this.distanceMeters = distanceMeters;
        }
    }

    /** Returns {@code null} if the graph isn't loaded, or no path exists between the two
     *  snapped endpoints (disconnected sub-graph). */
    static Result route(RoadGraph graph, double fromLat, double fromLon,
                         double toLat, double toLon) {
        if (graph == null) return null;
        int start = graph.nearestNode(fromLat, fromLon);
        int goal = graph.nearestNode(toLat, toLon);
        if (start < 0 || goal < 0) return null;
        if (start == goal) {
            return new Result(Collections.singletonList(new LatLng(fromLat, fromLon)), 0);
        }

        int n = graph.nodeCount();
        double[] g = new double[n];
        int[] cameFrom = new int[n];
        boolean[] visited = new boolean[n];
        Arrays.fill(g, Double.MAX_VALUE);
        Arrays.fill(cameFrom, -1);
        g[start] = 0;

        double[] f = new double[n];
        Arrays.fill(f, Double.MAX_VALUE);
        f[start] = Haversine.meters(graph.nodeLat[start], graph.nodeLon[start],
                graph.nodeLat[goal], graph.nodeLon[goal]);

        PriorityQueue<Integer> open = new PriorityQueue<>((a, b) -> Double.compare(f[a], f[b]));
        open.add(start);

        while (!open.isEmpty()) {
            int current = open.poll();
            if (visited[current]) continue;
            if (current == goal) break;
            visited[current] = true;

            for (int edgeIdx : graph.outgoingEdges(current)) {
                int next = graph.edgeTo[edgeIdx];
                if (visited[next]) continue;
                double candidate = g[current] + graph.edgeLength[edgeIdx];
                if (candidate < g[next]) {
                    g[next] = candidate;
                    cameFrom[next] = current;
                    f[next] = candidate + Haversine.meters(
                            graph.nodeLat[next], graph.nodeLon[next],
                            graph.nodeLat[goal], graph.nodeLon[goal]);
                    open.add(next);
                }
            }
        }

        if (g[goal] == Double.MAX_VALUE) return null;

        List<LatLng> path = new ArrayList<>();
        double distance = g[goal];
        int node = goal;
        while (node != -1) {
            path.add(new LatLng(graph.nodeLat[node], graph.nodeLon[node]));
            node = cameFrom[node];
        }
        Collections.reverse(path);
        return new Result(path, distance);
    }
}
