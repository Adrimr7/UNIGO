package com.example.unigoapp.interfaz.perfil;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PerfilVistaModelo extends ViewModel {

    private final MutableLiveData<String> mText;

    public PerfilVistaModelo() {
        mText = new MutableLiveData<>();
        mText.setValue("Esta es la vista de notificaciones");
    }

    public LiveData<String> getText() {
        return mText;
    }
}