package com.example.unigoapp.interfaz.home.weather;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.KeyFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import io.github.cdimascio.dotenv.Dotenv;


public class WeatherApiWorker extends Worker {
    public WeatherApiWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String tokenJWT = generate_JwtToken();
        if (tokenJWT == null){
            return Result.failure();
        }
        String response = weather_api_call(tokenJWT);
        if (response == null){
            return Result.failure();
        }
        boolean succes = save_data(response);
        if (!succes){
            return Result.failure();
        }
        return Result.success();
    }

    public boolean save_data(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray forecastArray = jsonObject.getJSONArray("forecast");

            List<ForecastDay> forecastList = new ArrayList<>();

            for (int i = 0; i < forecastArray.length(); i++) {
                JSONObject dayForecast = forecastArray.getJSONObject(i);

                String date = dayForecast.getString("date");
                JSONObject temperature = dayForecast.getJSONObject("temperature");
                double minTemp = temperature.getDouble("min");
                double maxTemp = temperature.getDouble("max");
                String weatherDescription = dayForecast.getString("weather");
                double precipitation = dayForecast.getDouble("precipitation");

                ForecastDay forecastDay = new ForecastDay(date, minTemp, maxTemp, weatherDescription, precipitation);
                forecastList.add(forecastDay);
            }

            // Almacenar la lista en el singleton
            WeatherData.getInstance().setForecastList(forecastList);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String weather_api_call(String token){
        try {
            String endpoint = "https://opendata.euskadi.eus/api-euskalmet/-/api-de-euskalmet/forecast/7days/Vitoria-Gasteiz"; // Reemplaza con el endpoint correcto

            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", "Bearer " + token);
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

    public String generate_JwtToken() {
        try {
            // Cargar la clave privada desde un archivo PEM
            String privateKeyPem = new String(Files.readAllBytes(Paths.get("ruta/a/tu/clave_privada.pem")));
            privateKeyPem = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);

            // Establecer los tiempos de emisión y expiración
            long nowMillis = System.currentTimeMillis();
            Date now = new Date(nowMillis);
            Date exp = new Date(nowMillis + 3600_000); // 1 hora de validez
            Dotenv dotenv = Dotenv.load();
            String email = dotenv.get("Email");
            // Crear el token JWT
            String token = JWT.create()
                    .withIssuer("unigoappEib")
                    .withAudience("met01.apikey")
                    .withClaim("email", email) // Reemplaza con tu correo
                    .withClaim("version", "1.0.0")
                    .withIssuedAt(now)
                    .withExpiresAt(exp)
                    .sign(Algorithm.RSA256(null, privateKey));

            System.out.println("Token JWT generado:");
            System.out.println(token);
            return token;
        }
        catch (Exception e){
            Log.e("i","Error al generar el token:" + e);
            return null;
        }
    }

}
