package com.example.unigoapp.interfaz.mapa;

import org.osmdroid.util.GeoPoint;
import java.util.List;
import java.time.LocalTime;

public class RutaInfo {    
    private final List<GeoPoint> puntos;
    private final String nombreOrigen;
    private final String nombreDestino;
    private final GeoPoint estacionOrigen;
    private final GeoPoint estacionDestino;
    private LocalTime proximoBus;
    private LocalTime tiempoEstimado;

    public RutaInfo(List<GeoPoint> puntos, String nombreOrigen, String nombreDestino, 
                    GeoPoint estacionOrigen, GeoPoint estacionDestino, LocalTime proximoBus, LocalTime tiempoEstimado) {
        this.puntos = puntos;
        this.nombreOrigen = nombreOrigen;
        this.nombreDestino = nombreDestino;
        this.estacionOrigen = estacionOrigen;
        this.estacionDestino = estacionDestino;
        this.proximoBus = proximoBus;
        this.tiempoEstimado = tiempoEstimado;
    }

    public List<GeoPoint> getPuntos() {
        return puntos;
    }

    public String getNombreOrigen() {
        return nombreOrigen;
    }

    public String getNombreDestino() {
        return nombreDestino;
    }

    public GeoPoint getEstacionOrigen() {
        return estacionOrigen;
    }

    public GeoPoint getEstacionDestino() {
        return estacionDestino;
    }

    public LocalTime getProximoBus() {
        return proximoBus;
    }

    public LocalTime getTiempoEstimado() {
        return tiempoEstimado;
    }

    public void setProximoBus(LocalTime proximoBus) {
        this.proximoBus = proximoBus;
    }
}
