package com.example.unigoapp.interfaz.mapa.bus;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.unigoapp.interfaz.mapa.RutaInfo;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraManyToManyShortestPaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GrafoBus {
    private final Graph<GeoPoint, DefaultWeightedEdge> grafo;
    private Map<String, GeoPoint> nodosMap;
    private Map<String, GeoPoint> estacionesMap;
    private Map<String, EstacionProperties> estacionesProperties;
    private final Context context;
    private static final double DISTANCIA_MINIMA_NODOS = 30.0; // 14 metros
    private final BusSchedule busSchedule;
    private static final Set<String> VALID_STOP_IDS = Set.of(
            "1029", "1327", "1027", "1028", "1036", "1025", "1037", "1026",
            "1034", "1023", "1035", "1024", "1328", "1032", "1033", "1022",
            "1041", "1031", "1030", "1040"
    );
    private static final List<String> ORDERED_STOP_IDS = List.of(
            "1022", "1023", "1024", "1025", "1026", "1027", "1028", "1328",
            "1029", "1030", "1031", "1032", "1033", "1034", "1035", "1036",
            "1037", "1327", "1040", "1041"
    );

    public GrafoBus(Context context) {
        this.context = context;
        this.grafo = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        this.nodosMap = new HashMap<>();
        this.estacionesMap = new HashMap<>();
        this.estacionesProperties = new HashMap<>();
        this.busSchedule = new BusSchedule();
        construirGrafoEficiente();
        cargarHorarios();
    }
    /*
    private void construirGrafoEficiente() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            long startTime = System.nanoTime();

            try (InputStream is = context.getAssets().open("grafo_buses.bin");
                 ObjectInputStream ois = new ObjectInputStream(is)) {

                Graph<GeoPoint, DefaultWeightedEdge> grafoCargado = (Graph<GeoPoint, DefaultWeightedEdge>) ois.readObject();
                Map<String, GeoPoint> nodosMapCargado = (Map<String, GeoPoint>) ois.readObject();
                Map<String, GeoPoint> mapEstaciones = (Map<String, GeoPoint>) ois.readObject();
                Map<String, EstacionProperties> propEstaciones = (Map<String, EstacionProperties>) ois.readObject();
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;

                new Handler(Looper.getMainLooper()).post(() -> {
                    this.grafo = grafoCargado;
                    this.nodosMap = nodosMapCargado;
                    this.estacionesMap = mapEstaciones;
                    this.estacionesProperties = propEstaciones;

                    Log.d("TiempoGrafo", "Grafo BUS cargado en: " + durationMs + " ms");
                    Log.d("Estadisticas", "Nodos: " + grafo.vertexSet().size() +
                            ", Aristas: " + grafo.edgeSet().size());
                });

            } catch (IOException | ClassNotFoundException e) {
                Log.e("CargaGrafo", "Error al cargar grafo desde assets", e);
            }
        });
    }
    */

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
            File archivo = new File(context.getFilesDir(), "grafo_buses_simple.bin");
            guardarGrafoYMapa(grafo, nodosMap, archivo.getAbsolutePath(), estacionesMap, estacionesProperties);
        } catch (Exception e) {
            Log.e("GrafoError", "Error construyendo grafo", e);
        }

    }


    private void cargarHorarios() {
        try (InputStream is = context.getAssets().open("horarios_bus_simple.txt")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean isHeader = true;

            Map<String, Map<String, String>> tripSchedules = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length >= 5) {  // trip_id,arrival_time,departure_time,stop_id,stop_sequence
                    String tripId = parts[0];
                    String arrivalTime = parts[1];
                    String stopId = parts[3];
                    int stopSequence = Integer.parseInt(parts[4]);
                    try {
                        busSchedule.addArrivalTime(stopId, arrivalTime);
                    } catch (Exception e) {
                        Log.e("Horarios", "Error al procesar horario: " + line, e);
                    }
                }
            }
            System.out.println("Horarios" + " Carga de horarios completada");
        } catch (IOException e) {
            System.out.println("Horarios" + " Error en carga de horarios" + e);
        }
    }

    public void guardarGrafoYMapa(Graph<GeoPoint, DefaultWeightedEdge> grafo, Map<String, GeoPoint> nodosMap,
                                  String rutaArchivo, Map<String, GeoPoint> estacionesMap, Map<String, EstacionProperties> estacionesProperties) throws IOException {

        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(rutaArchivo)))) {
            oos.writeObject(grafo);
            oos.writeObject(nodosMap);
            oos.writeObject(estacionesMap);
            oos.writeObject(estacionesProperties);
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
            String stopId = properties.getString("stop_id");
            if (!VALID_STOP_IDS.contains(stopId)) return;

            JSONArray coordArray = geometry.getJSONArray("coordinates");
            GeoPoint estacion = new GeoPoint(coordArray.getDouble(1), coordArray.getDouble(0));
            String nombre = properties.getString("stop_name");

            estacionesMap.put(stopId, estacion);
            estacionesProperties.put(stopId, new EstacionProperties(nombre, Integer.parseInt(stopId)));
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

        Log.d("BusSearch", String.format("Buscando estación cercana a: Lat: %.6f, Lon: %.6f",
                punto.getLatitude(), punto.getLongitude()));

        Log.d("BusSearch", "Número total de estaciones: " + estacionesMap.size());

        for (Map.Entry<String, GeoPoint> entry : estacionesMap.entrySet()) {
            GeoPoint estacion = entry.getValue();
            double distancia = punto.distanceToAsDouble(estacion);

            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercana = estacion;
            }
        }

        if (masCercana != null) {
            String nombreEstacion = obtenerNombreEstacion(masCercana);
            Log.d("BusSearch", String.format("Estación más cercana encontrada: %s a %.2f metros",
                    nombreEstacion, distanciaMinima));
        } else {
            Log.d("BusSearch", "No se encontró ninguna estación cercana");
        }

        return masCercana;
    }

    public RutaInfo calcularRuta(GeoPoint origen, GeoPoint destino) {
        long startTime = System.currentTimeMillis();

        // estaciones cercanas
        GeoPoint estacionInicio = encontrarEstacionCercana(origen);
        GeoPoint estacionFin = encontrarEstacionCercana(destino);

        if (estacionInicio == null || estacionFin == null || estacionInicio.equals(estacionFin)) {
            return new RutaInfo(new ArrayList<>(), "No encontrada", "No encontrada", origen, destino, LocalTime.now(), LocalTime.now());
        }

        try {
            DijkstraShortestPath<GeoPoint, DefaultWeightedEdge> dijkstra =
                    new DijkstraShortestPath<>(grafo);
            GraphPath<GeoPoint, DefaultWeightedEdge> path = dijkstra.getPath(estacionInicio, estacionFin);

            if (path == null) {
                Log.d("RutaDebug", "No se encontró ruta entre las estaciones");
            }

            List<GeoPoint> rutaFinal = path != null ?
                    new ArrayList<>(path.getVertexList()) :
                    new ArrayList<>();

            Log.d("TiempoRuta", "Ruta calculada en: " + (System.currentTimeMillis() - startTime) + " ms");
            Log.d("Info", "Distancia desde origen a estación más cercana: " +
                    origen.distanceToAsDouble(estacionInicio));
            String nombreOrigen = obtenerNombreEstacion(estacionInicio);
            String nombreDestino = obtenerNombreEstacion(estacionFin);

            return new RutaInfo(rutaFinal, nombreOrigen, nombreDestino, estacionInicio,
                    estacionFin, obtenerSiguienteLlegada(estacionInicio), obtenerLlegadaEnTiempo(estacionFin, LocalTime.now()));
        } catch (Exception e) {
            Log.e("RutaError", "Error calculando ruta", e);
            return null;
        }
    }

    public LocalTime obtenerSiguienteLlegada(GeoPoint estacion) {
        String stopId = encontrarStopId(estacion);
        if (stopId != null) {
            return busSchedule.getNextArrival(stopId, LocalTime.now());
        }
        return null;
    }
    public LocalTime obtenerLlegadaEnTiempo(GeoPoint estacion, LocalTime tiempo) {
        String stopId = encontrarStopId(estacion);
        if (stopId != null) {
            return busSchedule.getNextArrival(stopId, tiempo);
        }
        return null;
    }

    private String encontrarStopId(GeoPoint estacion) {
        for (Map.Entry<String, GeoPoint> entry : estacionesMap.entrySet()) {
            if (entry.getValue().equals(estacion)) {
                return entry.getKey();
            }
        }
        return null;
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