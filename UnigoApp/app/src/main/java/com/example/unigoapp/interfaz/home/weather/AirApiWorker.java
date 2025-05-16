package com.example.unigoapp.interfaz.home.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AirApiWorker extends Worker {
    public AirApiWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String airJson = air_api_call();
        String airJson_eusk = air_api_call_eusk();
        boolean success = saveData(airJson, false);
        boolean success_eusk = saveData(airJson_eusk,true);
        if (success && success_eusk){
            return Result.success();
        } else {
            return Result.failure();
        }
    }

    private boolean saveData(String airJson, boolean is_eusk){
        try {
            JSONArray jsonArray = new JSONArray(airJson);
            String firstAirQualityStation = null;

            // Recorrer el array principal
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject entry = jsonArray.getJSONObject(i);
                if (!entry.has("station")) continue; // Si no tiene "station", saltar

                JSONArray stations = entry.getJSONArray("station");
                for (int j = 0; j < stations.length(); j++) {
                    JSONObject station = stations.getJSONObject(j);
                    if (station.has("airQualityStation")) {
                        firstAirQualityStation = station.getString("airQualityStation");
                        break; // Terminar al encontrar el primero
                    }
                }
                if (firstAirQualityStation != null) break; // Salir del bucle externo
            }

            if (firstAirQualityStation == null){
                return false;
            } else{

                Context context = getApplicationContext();
                SharedPreferences prefs = context.getSharedPreferences("air", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                if (is_eusk){
                    editor.putString("quality_eusk", firstAirQualityStation);
                } else {
                    editor.putString("quality", firstAirQualityStation);
                }

                editor.apply();

                return true;

            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private String air_api_call() {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            String formattedDateTime = now.format(formatter);

            LocalDateTime atMidnight = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
            String formattedDateMidn = atMidnight.format(formatter);

            String endpoint = "https://api.euskadi.eus/air-quality/measurements/hourly/stations/85/from/" + formattedDateMidn + "/to/" + formattedDateTime;
            Log.i("la url", "URL:" + endpoint);
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            System.out.println("Código de respuesta: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Imprimir la respuesta JSON
                System.out.println("Respuesta de la API:");
                System.out.println(response.toString());
                return response.toString();
            } else {
                System.out.println("Error en la solicitud: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String air_api_call_eusk() {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            String formattedDateTime = now.format(formatter);

            LocalDateTime atMidnight = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
            String formattedDateMidn = atMidnight.format(formatter);

            String endpoint = "https://api.euskadi.eus/air-quality/measurements/hourly/stations/85/from/" + formattedDateMidn + "/to/" + formattedDateTime + "?lang=BASQUE";
            Log.i("la url", "URL:" + endpoint);
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            System.out.println("Código de respuesta: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Imprimir la respuesta JSON
                System.out.println("Respuesta de la API:");
                System.out.println(response.toString());
                return response.toString();
            } else {
                System.out.println("Error en la solicitud: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

