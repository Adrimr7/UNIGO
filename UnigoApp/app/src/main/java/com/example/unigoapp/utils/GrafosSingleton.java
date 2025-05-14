package com.example.unigoapp.utils;

import android.content.Context;
import android.util.Log;

import com.example.unigoapp.interfaz.mapa.RutaInfo;
import com.example.unigoapp.interfaz.mapa.andar.GrafoAndar;
import com.example.unigoapp.interfaz.mapa.bici.GrafoCarrilesBiciOptimizado;
import com.example.unigoapp.interfaz.mapa.bus.GrafoBus;
import com.example.unigoapp.interfaz.mapa.tranvia.GrafoTranvia;

import org.osmdroid.util.GeoPoint;

import java.util.Collections;
import java.util.List;

public class GrafosSingleton {
    private static GrafoCarrilesBiciOptimizado grafoCarrilesBici;
    private static GrafoBus grafoBuses;
    private static GrafoAndar grafoAndar;
    private static GrafoTranvia grafoTranvia;

    // los procesos son para comprobar si se ha cargado o no algo concreto.

    private static volatile int procesoAndar = 0;
    private static volatile int procesoBici = 0;
    private static volatile int procesoBus = 0;
    private static volatile int procesoTranvia = 0;

    private GrafosSingleton() {}

    public static synchronized GrafoAndar getGrafoAndar(Context context) {
        if (grafoAndar == null) {
            procesoAndar = 1;
            grafoAndar = new GrafoAndar(context);
            procesoAndar = 2;
        }
        return grafoAndar;
    }

    public static synchronized GrafoBus getGrafoBuses(Context context) {
        if (grafoBuses == null) {
            procesoBus = 1;
            grafoBuses = new GrafoBus(context);
            procesoBus = 2;
        }
        return grafoBuses;
    }

    public static synchronized GrafoCarrilesBiciOptimizado getGrafoBici(Context context) {
        if (grafoCarrilesBici == null) {
            procesoBici = 1;
            grafoCarrilesBici = new GrafoCarrilesBiciOptimizado(context);
            procesoBici = 2;
        }
        return grafoCarrilesBici;
    }
    public static synchronized GrafoTranvia getGrafoTranvia(Context context) {
        if (grafoTranvia == null) {
            procesoTranvia = 1;
            grafoTranvia = new GrafoTranvia(context);
            procesoTranvia = 2;
        }
        return grafoTranvia;
    }

    public static synchronized void setGrafoAndar(GrafoAndar grafo) {
        grafoAndar = grafo;
    }

    public static synchronized void setGrafoBuses(GrafoBus grafo) {
        grafoBuses = grafo;
    }

    public static synchronized void setGrafoBici(GrafoCarrilesBiciOptimizado grafo) {
        grafoCarrilesBici = grafo;
    }

    public static synchronized void setGrafoTranvia(GrafoTranvia grafo) {
        grafoTranvia = grafo;
    }

    public static synchronized int getProcesoAndar() {
        return procesoAndar;
    }

    public static synchronized int getProcesoBici() {
        return procesoBici;
    }

    public static synchronized int getProcesoBus() {
        return procesoBus;
    }
    public static synchronized int getProcesoTranvia() {
        return procesoTranvia;
    }

    public static synchronized List<GeoPoint> calcRutaAndar(GeoPoint origen, GeoPoint destino) {
        return grafoAndar.calcularRuta(origen, destino);
    }

    public static synchronized List<GeoPoint> calcRutaBici(GeoPoint origen, GeoPoint destino) {
        return grafoCarrilesBici.calcularRuta(origen, destino);
    }
    public static synchronized RutaInfo calcRutaBus(GeoPoint origen, GeoPoint destino) {
        RutaInfo rutaInfo = grafoBuses.calcularRuta(origen, destino);
        if (rutaInfo != null) {
            Log.d("RutaBus", String.format("Estación origen: %s - Lat: %.6f, Lon: %.6f",
                rutaInfo.getNombreOrigen(),
                rutaInfo.getEstacionOrigen().getLatitude(),
                rutaInfo.getEstacionOrigen().getLongitude()));
            Log.d("RutaBus", String.format("Estación destino: %s - Lat: %.6f, Lon: %.6f", 
                rutaInfo.getNombreDestino(),
                rutaInfo.getEstacionDestino().getLatitude(),
                rutaInfo.getEstacionDestino().getLongitude()));
            return rutaInfo;
        }
        return (RutaInfo) Collections.emptyList();
    }

    public static synchronized List<GeoPoint> calcRutaTranvia(GeoPoint origen, GeoPoint destino) {
        return grafoTranvia.calcularRuta(origen, destino);
    }

}
