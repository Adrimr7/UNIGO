package com.example.unigoapp.interfaz.home.weather;

import com.example.unigoapp.interfaz.home.weather.ForecastDay;

import java.util.ArrayList;
import java.util.List;

public class WeatherData {
    private static WeatherData instance;
    private List<ForecastDay> forecastList;

    private WeatherData() {
        forecastList = new ArrayList<>();
    }

    public static synchronized WeatherData getInstance() {
        if (instance == null) {
            instance = new WeatherData();
        }
        return instance;
    }

    public void setForecastList(List<ForecastDay> forecastList) {
        this.forecastList = forecastList;
    }

    public ForecastDay getForecastByDate(String date) {
        for (ForecastDay forecast : forecastList) {
            if (forecast.getDate().equals(date)) {
                return forecast;
            }
        }
        return null; // o lanzar una excepción si prefieres
    }

    public List<ForecastDay> getAllForecasts() {
        return new ArrayList<>(forecastList); // Devuelve una copia para evitar modificaciones externas
    }
}
