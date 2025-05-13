package com.example.unigoapp.interfaz.mapa.andar;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GrafoAndar {
    private Graph<GeoPoint, DefaultWeightedEdge> grafo;
    private Map<String, GeoPoint> nodosMap;
    private final Context context;
    private static final double DISTANCIA_MINIMA_NODOS = 20.0; // 20 metros

    public GrafoAndar(Context context) {
        this.context = context;
        this.grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        this.nodosMap = new HashMap<>();
        construirGrafoEficiente();
    }

    public void guardarGrafoYMapa(Graph<GeoPoint, DefaultWeightedEdge> grafo, Map<String, GeoPoint> nodosMap, String rutaArchivo) throws IOException {
        /*
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(rutaArchivo)))) {
            oos.writeObject(grafo);
            oos.writeObject(nodosMap);
        }
        */
    }

    public void cargarGrafoYMapa(String rutaArchivo) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Paths.get(rutaArchivo)))) {
            Graph<GeoPoint, DefaultWeightedEdge> grafoActual = (Graph<GeoPoint, DefaultWeightedEdge>) ois.readObject();
            Map<String, GeoPoint> nodosMapActual = (Map<String, GeoPoint>) ois.readObject();
            grafo = grafoActual;
            nodosMap = nodosMapActual;
        }
    }

    private void logMemoria(String tag) {
        Runtime runtime = Runtime.getRuntime();

        long maxMemory = runtime.maxMemory();        // Máxima memoria que puede usar la app
        long totalMemory = runtime.totalMemory();    // Memoria actualmente asignada
        long freeMemory = runtime.freeMemory();      // Memoria libre dentro de la asignada
        long usedMemory = totalMemory - freeMemory;  // Memoria usada real

        Log.d(tag, String.format("Max: %.2f MB, Total: %.2f MB, Usada: %.2f MB, Libre: %.2f MB",
                maxMemory / (1024.0 * 1024.0),
                totalMemory / (1024.0 * 1024.0),
                usedMemory / (1024.0 * 1024.0),
                freeMemory / (1024.0 * 1024.0)));
    }


    private void construirGrafoEficiente() {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            long startTime = System.nanoTime();

            try (InputStream is = context.getAssets().open("grafo.bin");
                 ObjectInputStream ois = new ObjectInputStream(is)) {

                Graph<GeoPoint, DefaultWeightedEdge> grafoCargado = (Graph<GeoPoint, DefaultWeightedEdge>) ois.readObject();
                Map<String, GeoPoint> nodosMapCargado = (Map<String, GeoPoint>) ois.readObject();
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;

                new Handler(Looper.getMainLooper()).post(() -> {
                    this.grafo = grafoCargado;
                    this.nodosMap = nodosMapCargado;

                    Log.d("TiempoGrafo", "Grafo cargado en: " + durationMs + " ms");
                    Log.d("Estadisticas", "Nodos: " + grafo.vertexSet().size() +
                            ", Aristas: " + grafo.edgeSet().size());
                });

            } catch (IOException | ClassNotFoundException e) {
                Log.e("CargaGrafo", "Error al cargar grafo desde assets", e);
            }
        });

        /*
        try (InputStream is = context.getAssets().open("mapa_andar_finish.geojson")) {
            String json = inputStreamToString(is);
            JSONArray features = new JSONObject(json).getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {
                procesarFeature(features.getJSONObject(i));
            }

            Log.d("TiempoGrafo", "Grafo construido en: " +
                    (System.currentTimeMillis() - startTime) + " ms");
            Log.d("Estadisticas", "Nodos: " + grafo.vertexSet().size() +
                    ", Aristas: " + grafo.edgeSet().size());
            File archivo = new File(context.getFilesDir(), "aaa.bin");
            guardarGrafoYMapa(grafo, nodosMap, archivo.getAbsolutePath());
        } catch (Exception e) {
            Log.e("GrafoError", "Error construyendo grafo", e);
        }

         */
    }

    private String inputStreamToString(InputStream is) throws Exception {
        try (java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    private void procesarFeature(JSONObject feature) throws Exception {
        if (feature.isNull("geometry")) return;

        JSONObject geometry = feature.getJSONObject("geometry");
        if (!"LineString".equals(geometry.getString("type"))) return;

        JSONArray coords = geometry.getJSONArray("coordinates");
        List<GeoPoint> puntosLinea = new ArrayList<>();

        for (int j = 0; j < coords.length(); j++) {
            JSONArray punto = coords.getJSONArray(j);
            puntosLinea.add(new GeoPoint(punto.getDouble(1), punto.getDouble(0)));
        }

        if (puntosLinea.size() < 2) return;

        // simplificar puntos y conectar nodos
        List<GeoPoint> puntosSimplificados = simplificarLinea(puntosLinea);

        conectarNodosEnLinea(puntosSimplificados);
    }

    private List<GeoPoint> simplificarLinea(List<GeoPoint> puntos) {
        List<GeoPoint> simplificados = new ArrayList<>();
        simplificados.add(puntos.get(0));

        for (int i = 1; i < puntos.size() - 1; i++) {
            if (puntos.get(i).distanceToAsDouble(simplificados.get(simplificados.size() - 1)) > DISTANCIA_MINIMA_NODOS) {
                simplificados.add(puntos.get(i));
            }
        }

        simplificados.add(puntos.get(puntos.size() - 1));
        return simplificados;
    }

    private void conectarNodosEnLinea(List<GeoPoint> puntos) {
        GeoPoint anterior = obtenerNodoGrafo(puntos.get(0));

        for (int i = 1; i < puntos.size(); i++) {
            GeoPoint actual = obtenerNodoGrafo(puntos.get(i));

            if (!anterior.equals(actual)) {
                DefaultWeightedEdge edge = grafo.addEdge(anterior, actual);
                if (edge != null) {
                    grafo.setEdgeWeight(edge, anterior.distanceToAsDouble(actual));
                }
                anterior = actual;
            }
        }
    }

    private GeoPoint obtenerNodoGrafo(GeoPoint punto) {
        String clave = String.format("%.6f,%.6f", punto.getLatitude(), punto.getLongitude());

        for (Map.Entry<String, GeoPoint> entry : nodosMap.entrySet()) {
            if (entry.getValue().distanceToAsDouble(punto) <= DISTANCIA_MINIMA_NODOS) {
                return entry.getValue();
            }
        }

        grafo.addVertex(punto);
        nodosMap.put(clave, punto);
        return punto;
    }

    public List<GeoPoint> calcularRuta(GeoPoint origen, GeoPoint destino) {
        long startTime = System.currentTimeMillis();

        GeoPoint inicio = encontrarNodoCercano(origen);
        GeoPoint fin = encontrarNodoCercano(destino);

        if (inicio == null || fin == null || inicio.equals(fin)) {
            return Collections.emptyList();
        }

        try {
            DijkstraShortestPath<GeoPoint, DefaultWeightedEdge> dijkstra =
                    new DijkstraShortestPath<>(grafo);
            GraphPath<GeoPoint, DefaultWeightedEdge> path = dijkstra.getPath(inicio, fin);

            Log.d("TiempoRuta", "Ruta calculada en: " +
                    (System.currentTimeMillis() - startTime) + " ms");

            return path != null ? path.getVertexList() : Collections.emptyList();
        } catch (Exception e) {
            Log.e("RutaError", "Error calculando ruta", e);
            return Collections.emptyList();
        }
    }

    private GeoPoint encontrarNodoCercano(GeoPoint punto) {
        GeoPoint masCercano = null;
        double distanciaMinima = Double.MAX_VALUE;

        for (GeoPoint nodo : nodosMap.values()) {
            double distancia = punto.distanceToAsDouble(nodo);
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercano = nodo;
                if (distanciaMinima < DISTANCIA_MINIMA_NODOS) {
                    break;
                }
            }
        }

        return masCercano;
    }
}