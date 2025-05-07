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

    public GrafoCarrilesBici(Context context) {
        this.context = context;
        this.grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        cargarGeoJsonYConstruirGrafo();
    }

    private void cargarGeoJsonYConstruirGrafo() {
        try {
            InputStream is = context.getAssets().open("viasciclistas23.geojson");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject geojson = new JSONObject(json);
            JSONArray features = geojson.getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);

                if (feature.isNull("geometry")) continue;
                JSONObject geometry = feature.getJSONObject("geometry");
                if (!geometry.getString("type").equals("LineString")) continue;

                JSONArray coords = geometry.getJSONArray("coordinates");

                GeoPoint prev = null;
                for (int j = 0; j < coords.length(); j++) {
                    JSONArray punto = coords.getJSONArray(j);
                    double lon = punto.getDouble(0);
                    double lat = punto.getDouble(1);
                    GeoPoint actual = new GeoPoint(lat, lon);

                    grafo.addVertex(actual);
                    if (prev != null) {
                        grafo.addVertex(prev);
                        DefaultWeightedEdge edge = grafo.addEdge(prev, actual);
                        if (edge != null) {
                            double distancia = prev.distanceToAsDouble(actual);
                            grafo.setEdgeWeight(edge, distancia);
                        }
                    }
                    prev = actual;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private GeoPoint encontrarNodoMasCercano(GeoPoint punto) {
        GeoPoint masCercano = null;
        double minDist = Double.MAX_VALUE;

        for (GeoPoint nodo : grafo.vertexSet()) {
            double dist = punto.distanceToAsDouble(nodo);
            if (dist < minDist) {
                minDist = dist;
                masCercano = nodo;
            }
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

