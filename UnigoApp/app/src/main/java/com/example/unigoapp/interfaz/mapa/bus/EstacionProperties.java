package com.example.unigoapp.interfaz.mapa.bus;

import java.io.Serializable;
import java.time.LocalTime;

public class EstacionProperties implements Serializable {
    private final String nombre;
    private final int stopId;
    private LocalTime nextArrival;

    public EstacionProperties(String nombre, int stopId) {
        this.nombre = nombre;
        this.stopId = stopId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getStopId() {
        return String.valueOf(stopId);
    }

    public void setNextArrival(LocalTime time) {
        this.nextArrival = time;
    }

    public LocalTime getNextArrival() {
        return nextArrival;
    }
}