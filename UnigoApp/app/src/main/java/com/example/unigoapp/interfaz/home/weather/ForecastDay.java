package com.example.unigoapp.interfaz.home.weather;

public class ForecastDay {
    private String date;
    private double minTemp;
    private double maxTemp;
    private String weatherDescription;
    private double precipitation;

    public ForecastDay(String date, double minTemp, double maxTemp, String weatherDescription, double precipitation) {
        this.date = date;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.weatherDescription = weatherDescription;
        this.precipitation = precipitation;
    }

    public String getDate() {
        return date;
    }

    public double getMinTemp() {
        return minTemp;
    }

    public double getMaxTemp() {
        return maxTemp;
    }

    public String getWeatherDescription() {
        return weatherDescription;
    }

    public double getPrecipitation() {
        return precipitation;
    }
}

