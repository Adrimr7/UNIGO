package com.example.unigoapp.interfaz.mapa;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unigoapp.interfaz.mapa.bici.GrafoCarrilesBiciOptimizado;

public class MapaVistaModelo extends ViewModel {

    private final MutableLiveData<String> mText;
    private GrafoCarrilesBiciOptimizado grafoCarrilesBici;

    public MapaVistaModelo() {
        mText = new MutableLiveData<>();
        mText.setValue("Esta es la vista del mapa");
    }

    public LiveData<String> getText() {
        return mText;
    }

    public GrafoCarrilesBiciOptimizado getGrafo() {
        if (grafoCarrilesBici == null) {
            return null;
        }
        return grafoCarrilesBici;
    }

    public void setGrafo(GrafoCarrilesBiciOptimizado grafo) {
        grafoCarrilesBici = grafo;
    }
}