package com.example.unigoapp.interfaz.mapa;

import android.content.Context;

import com.example.unigoapp.interfaz.mapa.andar.GrafoAndar;
import com.example.unigoapp.interfaz.mapa.bici.GrafoCarrilesBiciOptimizado;
import com.example.unigoapp.interfaz.mapa.bus.GrafoBus;

public class GrafosSingleton {
    private static GrafoCarrilesBiciOptimizado grafoCarrilesBici;
    private static GrafoBus grafoBuses;
    private static GrafoAndar grafoAndar;

    // los procesos son para comprobar si se ha cargado o no algo concreto.

    private static int procesoAndar = 0;
    private static int procesoBici = 0;
    private static int procesoBus = 0;

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

    public static synchronized void setGrafoAndar(GrafoAndar grafo) {
        grafoAndar = grafo;
    }

    public static synchronized void setGrafoBuses(GrafoBus grafo) {
        grafoBuses = grafo;
    }

    public static synchronized void setGrafoBici(GrafoCarrilesBiciOptimizado grafo) {
        grafoCarrilesBici = grafo;
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


}
