package com.example.unigoapp.interfaz.mapa;

import android.content.Context;

import com.example.unigoapp.interfaz.mapa.andar.GrafoAndar;
import com.example.unigoapp.interfaz.mapa.bici.GrafoCarrilesBiciOptimizado;
import com.example.unigoapp.interfaz.mapa.bus.GrafoBus;

public class GrafosSingleton {
    private static GrafoCarrilesBiciOptimizado grafoCarrilesBici;
    private static GrafoBus grafoBuses;
    private static GrafoAndar grafoAndar;

    private GrafosSingleton() {}

    public static synchronized GrafoAndar getGrafoAndar(Context context) {
        if (grafoAndar == null) {
            grafoAndar = new GrafoAndar(context);
        }
        return grafoAndar;
    }

    public static synchronized GrafoBus getGrafoBuses(Context context) {
        if (grafoBuses == null) {
            grafoBuses = new GrafoBus(context);
        }
        return grafoBuses;
    }

    public static synchronized GrafoCarrilesBiciOptimizado getGrafoBici(Context context) {
        if (grafoCarrilesBici == null) {
            grafoCarrilesBici = new GrafoCarrilesBiciOptimizado(context);
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


}
