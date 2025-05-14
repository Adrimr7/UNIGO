package com.example.unigoapp.interfaz.mapa.bus;

import android.content.Context;
import android.util.Log;

import com.example.unigoapp.interfaz.mapa.RutaInfo;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GrafoBus {
    private final Graph<GeoPoint, DefaultWeightedEdge> grafo;
    private final Map<String, GeoPoint> nodosMap;
    private final Map<String, GeoPoint> estacionesMap;
    private final Map<String, EstacionProperties> estacionesProperties;
    private final Context context;
    private static final double DISTANCIA_MINIMA_NODOS = 20.0; // 20 metros

    private static class EstacionProperties {
        private final String nombre;
        private final int stopId;

        EstacionProperties(String nombre, int stopId) {
            this.nombre = nombre;
            this.stopId = stopId;
        }

        public String getNombre() {
            return nombre;
        }

        public int getStopId() {
            return stopId;
        }
    }

    public GrafoBus(Context context) {
        this.context = context;
        this.grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        this.nodosMap = new HashMap<>();
        this.estacionesMap = new HashMap<>();
        this.estacionesProperties = new HashMap<>();
        construirGrafoEficiente();
    }

    private void construirGrafoEficiente() {
        long startTime = System.currentTimeMillis();

        try (InputStream is = context.getAssets().open("rutas_buses.geojson")) {
            String json = inputStreamToString(is);
            JSONArray features = new JSONObject(json).getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {
                procesarFeature(features.getJSONObject(i));
            }

            Log.d("TiempoGrafo", "Grafo construido en: " +
                    (System.currentTimeMillis() - startTime) + " ms");
            Log.d("Estadisticas", "Nodos: " + grafo.vertexSet().size() +
                    ", Aristas: " + grafo.edgeSet().size());
        } catch (Exception e) {
            Log.e("GrafoError", "Error construyendo grafo", e);
        }
    }

    private String inputStreamToString(InputStream is) throws Exception {
        try (java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    private void procesarFeature(JSONObject feature) throws Exception {
        if (feature.isNull("geometry")) return;

        JSONObject geometry = feature.getJSONObject("geometry");
        JSONObject properties = feature.getJSONObject("properties");

        // procesar estaciones de bus
        if ("Point".equals(geometry.getString("type")) &&
            properties.has("stop_id") && properties.has("stop_name")) {
            JSONArray coordArray = geometry.getJSONArray("coordinates");
            GeoPoint estacion = new GeoPoint(coordArray.getDouble(1), coordArray.getDouble(0));
            int stopId = properties.getInt("stop_id");
            String stopIdStr = String.valueOf(stopId);
            String nombre = properties.getString("stop_name");

            estacionesMap.put(stopIdStr, estacion);
            estacionesProperties.put(stopIdStr, new EstacionProperties(nombre, stopId));
            nodosMap.put(String.format("%.6f,%.6f", estacion.getLatitude(), estacion.getLongitude()), estacion);
            grafo.addVertex(estacion);
            return;
        }

        // procesar segmentos de rutas
        if ("LineString".equals(geometry.getString("type"))) {
            JSONArray coords = geometry.getJSONArray("coordinates");
            List<GeoPoint> puntosLinea = new ArrayList<>();

            for (int j = 0; j < coords.length(); j++) {
                JSONArray punto = coords.getJSONArray(j);
                puntosLinea.add(new GeoPoint(punto.getDouble(1), punto.getDouble(0)));
            }

            if (puntosLinea.size() < 2) return;

            List<GeoPoint> puntosSimplificados = simplificarLinea(puntosLinea);
            conectarNodosEnLinea(puntosSimplificados);
        }
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

    private GeoPoint encontrarEstacionCercana(GeoPoint punto) {
        GeoPoint masCercana = null;
        double distanciaMinima = Double.MAX_VALUE;

        for (GeoPoint estacion : estacionesMap.values()) {
            double distancia = punto.distanceToAsDouble(estacion);
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercana = estacion;
            }
        }

        return masCercana;
    }
    public RutaInfo calcularRuta(GeoPoint origen, GeoPoint destino) {
        long startTime = System.currentTimeMillis();

        // estaciones cercanas
        GeoPoint estacionInicio = encontrarEstacionCercana(origen);
        GeoPoint estacionFin = encontrarEstacionCercana(destino);

        if (estacionInicio == null || estacionFin == null || estacionInicio.equals(estacionFin)) {
            return (RutaInfo) Collections.emptyList();
        }

        // info sobre estaciones
        Log.d("RutaBus", String.format("Estación origen: %s - Lat: %.6f, Lon: %.6f",
            obtenerNombreEstacion(estacionInicio),
            estacionInicio.getLatitude(),
            estacionInicio.getLongitude()));
        Log.d("RutaBus", String.format("Estación destino: %s - Lat: %.6f, Lon: %.6f",
            obtenerNombreEstacion(estacionFin),
            estacionFin.getLatitude(),
            estacionFin.getLongitude()));

        try {
            DijkstraShortestPath<GeoPoint, DefaultWeightedEdge> dijkstra =
                new DijkstraShortestPath<>(grafo);
            GraphPath<GeoPoint, DefaultWeightedEdge> path = dijkstra.getPath(estacionInicio, estacionFin);

            if (path == null) {
                return (RutaInfo) Collections.emptyList();
            }

            List<GeoPoint> rutaFinal = new ArrayList<>(path.getVertexList());

            Log.d("TiempoRuta", "Ruta calculada en: " + (System.currentTimeMillis() - startTime) + " ms");
            Log.d("Info", "Distancia desde origen a estación más cercana: " +
                  origen.distanceToAsDouble(estacionInicio));            String nombreOrigen = obtenerNombreEstacion(estacionInicio);
            String nombreDestino = obtenerNombreEstacion(estacionFin);

            return new RutaInfo(rutaFinal, nombreOrigen, nombreDestino, estacionInicio, estacionFin);
        } catch (Exception e) {
            Log.e("RutaError", "Error calculando ruta", e);
            return null;
        }
    }

    private String obtenerNombreEstacion(GeoPoint estacion) {
        for (Map.Entry<String, GeoPoint> entry : estacionesMap.entrySet()) {
            if (entry.getValue().equals(estacion)) {
                EstacionProperties props = estacionesProperties.get(entry.getKey());
                if (props != null) {
                    return props.getNombre();
                }
                break;
            }
        }
        return "Estación desconocida";
    }
}