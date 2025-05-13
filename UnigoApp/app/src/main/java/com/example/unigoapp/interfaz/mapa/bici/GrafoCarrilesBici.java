package com.example.unigoapp.interfaz.mapa.bici;

import android.content.Context;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GrafoCarrilesBici {
    private Graph<GeoPoint, DefaultWeightedEdge> grafo;
    private final Context context;
    private final Map<GeoPoint, GeoPoint> cacheNodoCercano;
    public GrafoCarrilesBici(Context context) {
        this.context = context;
        this.grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        this.cacheNodoCercano = new HashMap<>();
        cargarGeoJsonYConstruirGrafo();
    }

    private void cargarGeoJsonYConstruirGrafo() {
        try (InputStream is = context.getAssets().open("viasciclistas23.geojson")) {
            String json = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
            JSONObject geojson = new JSONObject(json);
            JSONArray features = geojson.getJSONArray("features");

            Map<GeoPoint, GeoPoint> uniquePoints = new HashMap<>();

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                if (feature.isNull("geometry")) continue;

                JSONObject geometry = feature.getJSONObject("geometry");
                if (!geometry.getString("type").equals("LineString")) continue;

                JSONArray coords = geometry.getJSONArray("coordinates");
                GeoPoint prev = null;

                for (int j = 0; j < coords.length(); j++) {
                    JSONArray punto = coords.getJSONArray(j);
                    GeoPoint actual = new GeoPoint(punto.getDouble(1), punto.getDouble(0));

                    GeoPoint existing = encontrarPuntoSimilar(uniquePoints, actual, 0.00001);
                    if (existing == null) {
                        uniquePoints.put(actual, actual);
                        grafo.addVertex(actual);
                    } else {
                        actual = existing;
                    }

                    if (prev != null && !actual.equals(prev)) {
                        DefaultWeightedEdge edge = grafo.addEdge(prev, actual);
                        if (edge != null) {
                            grafo.setEdgeWeight(edge, prev.distanceToAsDouble(actual));
                        }
                    }
                    prev = actual;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private GeoPoint encontrarPuntoSimilar(Map<GeoPoint, GeoPoint> mapaPuntos, GeoPoint target, double tolerancia) {
        for (GeoPoint punto : mapaPuntos.keySet()) {
            if (Math.abs(punto.getLatitude() - target.getLatitude()) < tolerancia &&
                    Math.abs(punto.getLongitude() - target.getLongitude()) < tolerancia) {
                return punto;
            }
        }
        return null;
    }

    private GeoPoint encontrarNodoMasCercano(GeoPoint punto) {
        if (cacheNodoCercano.containsKey(punto)) {
            return cacheNodoCercano.get(punto);
        }

        GeoPoint masCercano = null;
        double minDist = Double.MAX_VALUE;

        for (GeoPoint nodo : grafo.vertexSet()) {
            double dist = punto.distanceToAsDouble(nodo);
            if (dist < minDist) {
                minDist = dist;
                masCercano = nodo;
            }
        }

        if (masCercano != null) {
            cacheNodoCercano.put(punto, masCercano);
        }

        return masCercano;
    }

    public List<GeoPoint> calcularRuta(GeoPoint origen, GeoPoint destino) {
        GeoPoint inicio = encontrarNodoMasCercano(origen);
        GeoPoint fin = encontrarNodoMasCercano(destino);

        if (inicio == null || fin == null) return Collections.emptyList();

        DijkstraShortestPath<GeoPoint, DefaultWeightedEdge> dijkstra =
                new DijkstraShortestPath<>(grafo);

        List<GeoPoint> ruta = new ArrayList<>();
        List<DefaultWeightedEdge> aristas = dijkstra.getPath(inicio, fin).getEdgeList();

        ruta.add(inicio);
        for (DefaultWeightedEdge e : aristas) {
            GeoPoint target = grafo.getEdgeTarget(e);
            if (!ruta.contains(target)) {
                ruta.add(target);
            }
        }
        return ruta;
    }
}

