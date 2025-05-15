package com.example.unigoapp.interfaz.mapa.bus;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusSchedule {
    private final Map<String, List<LocalTime>> horariosParadas;

    public BusSchedule() {
        this.horariosParadas = new HashMap<>();
    }

    public void addArrivalTime(String stopId, String timeString) {
        List<LocalTime> times = horariosParadas.computeIfAbsent(stopId, k -> new ArrayList<>());
        LocalTime time = parseExtendedTime(timeString);
        times.add(time);
    }

    private LocalTime parseExtendedTime(String timeString) {
        String[] parts = timeString.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid time format: " + timeString);
        }

        int horas = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);

        // horas extranas (25,26...)
        if (horas >= 24) {
            horas = horas % 24;
        }

        return LocalTime.of(horas, minutes);
    }    
    
    public LocalTime getNextArrival(String stopId, LocalTime currentTime) {
        List<LocalTime> times = horariosParadas.get(stopId);
        if (times == null || times.isEmpty()) {
            return null;
        }

        LocalTime nextArrival = null;
        LocalTime nextDayTime = currentTime.plusHours(24);

        for (LocalTime time : times) {
            // hoy o ayer
            if (time.isAfter(currentTime)) {
                if (nextArrival == null || time.isBefore(nextArrival)) {
                    nextArrival = time;
                }
            }
            // cambio de 24 horas
            else if (time.isBefore(currentTime) && time.isBefore(LocalTime.of(5, 0))) {
                LocalTime adjustedTime = time.plusHours(24);
                if (adjustedTime.isAfter(currentTime) && (nextArrival == null || adjustedTime.isBefore(nextArrival))) {
                    nextArrival = time;
                }
            }
        }
        return nextArrival;
    }
}
