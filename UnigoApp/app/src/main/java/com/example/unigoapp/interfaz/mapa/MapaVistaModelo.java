package com.example.unigoapp.interfaz.mapa;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MapaVistaModelo extends ViewModel {

    private final MutableLiveData<String> mText;

    public MapaVistaModelo() {
        mText = new MutableLiveData<>();
        mText.setValue("Esta es la vista del mapa");
    }

    public LiveData<String> getText() {
        return mText;
    }
}