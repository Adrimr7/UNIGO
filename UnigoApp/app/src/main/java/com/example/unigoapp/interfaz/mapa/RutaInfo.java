package com.example.unigoapp.interfaz.mapa;

import org.osmdroid.util.GeoPoint;
import java.util.List;

public class RutaInfo {
    private final List<GeoPoint> puntos;
    private final String nombreOrigen;
    private final String nombreDestino;
    private final GeoPoint estacionOrigen;
    private final GeoPoint estacionDestino;

    public RutaInfo(List<GeoPoint> puntos, String nombreOrigen, String nombreDestino, 
                    GeoPoint estacionOrigen, GeoPoint estacionDestino) {
        this.puntos = puntos;
        this.nombreOrigen = nombreOrigen;
        this.nombreDestino = nombreDestino;
        this.estacionOrigen = estacionOrigen;
        this.estacionDestino = estacionDestino;
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
}
