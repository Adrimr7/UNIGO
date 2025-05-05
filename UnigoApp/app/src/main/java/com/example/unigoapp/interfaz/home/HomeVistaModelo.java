package com.example.unigoapp.interfaz.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeVistaModelo extends ViewModel {

    private final MutableLiveData<String> mText;

    public HomeVistaModelo() {
        mText = new MutableLiveData<>();
        mText.setValue("Esta es la vista de home");
    }

    public LiveData<String> getText() {
        return mText;
    }
}