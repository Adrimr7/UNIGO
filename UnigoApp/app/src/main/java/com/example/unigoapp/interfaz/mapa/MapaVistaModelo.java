package com.example.unigoapp.interfaz.mapa;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unigoapp.interfaz.mapa.bici.GrafoCarrilesBiciOptimizado;
import com.example.unigoapp.interfaz.mapa.bus.GrafoBus;

public class MapaVistaModelo extends ViewModel {

    private final MutableLiveData<String> mText;
    private GrafoCarrilesBiciOptimizado grafoCarrilesBici;
    private GrafoBus grafoBuses;

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

    public void setGrafoBici(GrafoCarrilesBiciOptimizado grafo) {
        grafoCarrilesBici = grafo;
    }

    public void setGrafoBus(GrafoBus grafo) {
        grafoBuses = grafo;
    }
}