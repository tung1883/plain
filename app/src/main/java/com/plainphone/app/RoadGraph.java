package com.plainphone.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Lazy, process-wide driving-network graph for routing — loaded from
 *  {@code assets/map/roads.json} (built at dev time by {@code tools/nav/build_roads.py} from a
 *  raw OSM extract; the bundled vector tiles aren't used here since they clip/simplify road
 *  geometry at tile boundaries, which breaks clean topology).
 *
 * <p>Stored as primitive parallel arrays rather than a {@code List} of boxed node/edge objects —
 * a tens-of-thousands-of-nodes graph as boxed objects would be a real memory cost on a phone.
 * This is the one place in the nav-plugin routing code that deliberately isn't "a tidy object
 * per row." */
final class RoadGraph {

    interface OnLoaded {
        void onLoaded(boolean ok);
    }

    private static volatile RoadGraph instance;
    private static volatile boolean loading;

    final double[] nodeLat;
    final double[] nodeLon;
    final int[] edgeFrom;
    final int[] edgeTo;
    final float[] edgeLength;
    /** CSR-style adjacency: edges with {@code from == n} are
     *  {@code adjacencyEdge[adjacencyStart[n] .. adjacencyStart[n+1])}. */
    private final int[] adjacencyStart;
    private final int[] adjacencyEdge;

    private RoadGraph(double[] nodeLat, double[] nodeLon, int[] edgeFrom, int[] edgeTo,
                       float[] edgeLength) {
        this.nodeLat = nodeLat;
        this.nodeLon = nodeLon;
        this.edgeFrom = edgeFrom;
        this.edgeTo = edgeTo;
        this.edgeLength = edgeLength;

        int n = nodeLat.length;
        int[] degree = new int[n + 1];
        for (int from : edgeFrom) degree[from + 1]++;
        for (int i = 0; i < n; i++) degree[i + 1] += degree[i];
        this.adjacencyStart = degree;

        int[] cursor = new int[n];
        System.arraycopy(adjacencyStart, 0, cursor, 0, n);
        int[] adjEdge = new int[edgeFrom.length];
        for (int e = 0; e < edgeFrom.length; e++) {
            int from = edgeFrom[e];
            adjEdge[cursor[from]++] = e;
        }
        this.adjacencyEdge = adjEdge;
    }

    static RoadGraph get() {
        return instance;
    }

    static boolean isLoaded() {
        return instance != null;
    }

    static void ensureLoaded(Context context, OnLoaded callback) {
        if (instance != null) {
            if (callback != null) callback.onLoaded(true);
            return;
        }
        synchronized (RoadGraph.class) {
            if (loading) return;
            loading = true;
        }
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            RoadGraph loaded = load(appContext);
            instance = loaded;
            loading = false;
            if (callback != null) callback.onLoaded(loaded != null);
        }, "RoadGraph-load").start();
    }

    /** Edge indices leaving {@code node}, for A*'s neighbor expansion. */
    int[] outgoingEdges(int node) {
        int start = adjacencyStart[node];
        int end = adjacencyStart[node + 1];
        int[] out = new int[end - start];
        System.arraycopy(adjacencyEdge, start, out, 0, out.length);
        return out;
    }

    int nodeCount() {
        return nodeLat.length;
    }

    /** Brute-force nearest-node scan — cheap at this graph's scale (tens of thousands of
     *  nodes), done at most twice per route request. No spatial index needed for v1. */
    int nearestNode(double lat, double lon) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < nodeLat.length; i++) {
            double d = Haversine.meters(lat, lon, nodeLat[i], nodeLon[i]);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    private static RoadGraph load(Context context) {
        try (InputStream in = context.getAssets().open("map/roads.json")) {
            String json = readAll(in);
            JSONObject root = new JSONObject(json);
            JSONObject nodes = root.getJSONObject("nodes");
            JSONArray latArr = nodes.getJSONArray("lat");
            JSONArray lonArr = nodes.getJSONArray("lon");
            int nodeCount = latArr.length();
            double[] nodeLat = new double[nodeCount];
            double[] nodeLon = new double[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                nodeLat[i] = latArr.getDouble(i);
                nodeLon[i] = lonArr.getDouble(i);
            }

            JSONObject edges = root.getJSONObject("edges");
            JSONArray fromArr = edges.getJSONArray("from");
            JSONArray toArr = edges.getJSONArray("to");
            JSONArray lenArr = edges.getJSONArray("length");
            int edgeCount = fromArr.length();
            int[] edgeFrom = new int[edgeCount];
            int[] edgeTo = new int[edgeCount];
            float[] edgeLength = new float[edgeCount];
            for (int i = 0; i < edgeCount; i++) {
                edgeFrom[i] = fromArr.getInt(i);
                edgeTo[i] = toArr.getInt(i);
                edgeLength[i] = (float) lenArr.getDouble(i);
            }

            return new RoadGraph(nodeLat, nodeLon, edgeFrom, edgeTo, edgeLength);
        } catch (Exception e) {
            return null;
        }
    }

    private static String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return sb.toString();
    }
}
