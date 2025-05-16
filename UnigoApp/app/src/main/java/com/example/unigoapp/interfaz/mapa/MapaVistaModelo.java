package com.example.unigoapp.interfaz.mapa;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unigoapp.interfaz.mapa.andar.GrafoAndar;
import com.example.unigoapp.interfaz.mapa.bici.GrafoCarrilesBiciOptimizado;
import com.example.unigoapp.interfaz.mapa.bus.GrafoBus;

public class MapaVistaModelo extends ViewModel {

    private final MutableLiveData<String> mText;
    private GrafoCarrilesBiciOptimizado grafoCarrilesBici;
    private GrafoBus grafoBuses;
    private GrafoAndar grafoAndar;
    public boolean estaCargando = true;

    public MapaVistaModelo() {
        mText = new MutableLiveData<>();
        mText.setValue("Esta es la vista del mapa");
    }

    public LiveData<String> getText() {
        return mText;
    }

    public GrafoCarrilesBiciOptimizado getGrafoBici() {
        if (grafoCarrilesBici == null) {
            return null;
        }
        return grafoCarrilesBici;
    }

    public GrafoBus getGrafoBus() {
        if (grafoBuses == null) {
            return null;
        }
        return grafoBuses;
    }

    public GrafoAndar getGrafoAndar() {
        if (grafoAndar == null) {
            return null;
        }
        return grafoAndar;
    }


    public void setGrafoBici(GrafoCarrilesBiciOptimizado grafo) {
        grafoCarrilesBici = grafo;
    }

    public void setGrafoBus(GrafoBus grafo) {
        grafoBuses = grafo;
    }

    public void setGrafoAndar(GrafoAndar grafo) {
        grafoAndar = grafo;
    }
    public void setCargandoGrafoAndar(boolean esta) {
        estaCargando = esta;
    }
}