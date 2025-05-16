package com.example.unigoapp.interfaz.mapa.tranvia;

import android.content.Context;
import android.util.Log;

import com.example.unigoapp.interfaz.mapa.RutaInfo;
import com.example.unigoapp.interfaz.mapa.bus.EstacionProperties;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GrafoTranvia {
    private final Graph<GeoPoint, DefaultWeightedEdge> grafo;
    private final Map<String, GeoPoint> nodosMap;
    private final Context context;
    private Map<String, GeoPoint> estacionesMap;
    private Map<String, EstacionProperties> estacionesProperties;
    private static final double DISTANCIA_MINIMA_NODOS = 14.0; // 14 metros

    public GrafoTranvia(Context context) {
        this.context = context;
        this.grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        this.nodosMap = new HashMap<>();
        this.estacionesMap = new HashMap<>();
        this.estacionesProperties = new HashMap<>();
        construirGrafoEficiente();
    }

    private void construirGrafoEficiente() {
        long startTime = System.currentTimeMillis();

        try (InputStream is = context.getAssets().open("new_tranvias.geojson")) {
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

        JSONArray coordArray = geometry.getJSONArray("coordinates");

        List<GeoPoint> puntosLinea = new ArrayList<>();
        if (coordArray.length() > 2) {
            for (int j = 0; j < coordArray.length(); j++) {
                JSONArray punto = coordArray.getJSONArray(j);
                puntosLinea.add(new GeoPoint(punto.getDouble(1), punto.getDouble(0)));
            }

            if (puntosLinea.size() < 2) return;

            // simplificar puntos y conectar nodos
            List<GeoPoint> puntosSimplificados = simplificarLinea(puntosLinea);
            conectarNodosEnLinea(puntosSimplificados);
        }
        else
        {
            GeoPoint estacion = new GeoPoint(coordArray.getDouble(0), coordArray.getDouble(1));
            String nombre = properties.getString("stop_name");
            String stopId = feature.getString("id");

            estacionesMap.put(stopId, estacion);
            estacionesProperties.put(stopId, new EstacionProperties(nombre, Integer.parseInt(stopId)));
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

    public RutaInfo calcularRuta(GeoPoint origen, GeoPoint destino) {
        long startTime = System.currentTimeMillis();
        RutaInfo rutaDevolver = new RutaInfo(new ArrayList<>(), "No encontrada", "No encontrada", origen, destino, LocalTime.now(), LocalTime.now());;

        GeoPoint estacionInicio = encontrarNodoCercano(origen);
        GeoPoint estacionFin = encontrarNodoCercano(destino);

        if (estacionInicio == null || estacionFin == null || estacionInicio.equals(estacionFin)) {
            return rutaDevolver;
        }

        // encontrar los paths
        try {
            DijkstraShortestPath<GeoPoint, DefaultWeightedEdge> dijkstra = new DijkstraShortestPath<>(grafo);
            GraphPath<GeoPoint, DefaultWeightedEdge> rutaPropuesta = dijkstra.getPath(estacionInicio, estacionFin);
            
            if (rutaPropuesta == null) {
                return rutaDevolver;
            }
            
            List<GeoPoint> rutaFinal = new ArrayList<>(rutaPropuesta.getVertexList());

            Log.d("TiempoRuta", "Ruta calculada en: " + (System.currentTimeMillis() - startTime) + " ms");
            Log.d("Info", "Distancia desde origen al nodo más cercano: " + origen.distanceToAsDouble(estacionInicio));
            LocalTime proxTranvia = calcularProximoTranvia();
            // 1min 10s entre parada y parada
            LocalTime tiempoLlegada = proxTranvia.plusMinutes(
                    (long)(rutaFinal.size() * 0.4)
            );
            return new RutaInfo(rutaFinal, "", "",
                    estacionInicio, estacionFin, calcularProximoTranvia(), tiempoLlegada);
        } catch (Exception e) {
            Log.e("RutaError", "Error calculando ruta", e);
            return rutaDevolver;
        }
    }

    private LocalTime calcularProximoTranvia() {
        LocalTime ahora = LocalTime.now();
        int minutos = ahora.getMinute();
        int proximosMinutos = (((minutos / 15) + 1) * 15) - 1;

        return ahora
                .withMinute(proximosMinutos)
                .withSecond(0)
                .withNano(0);
    }
}