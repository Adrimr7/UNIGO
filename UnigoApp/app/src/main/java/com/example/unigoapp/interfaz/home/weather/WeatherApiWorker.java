package com.example.unigoapp.interfaz.home.weather;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.KeyFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONObject;


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

    private boolean save_data(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);

            // Acceder al objeto temperatureRange
            JSONObject temperatureRange = json.getJSONObject("temperatureRange");
            JSONObject temperature = json.getJSONObject("temperature");
            JSONObject forecastText = json.getJSONObject("forecastText");

            // Extraer valores mínimo y máximo
            double tempMin = temperatureRange.getDouble("min");
            double tempMax = temperatureRange.getDouble("max");
            double tempNow = temperature.getDouble("value");

            // Obtener los textos en español y euskera
            String textoEspanol = forecastText.getString("SPANISH");
            String textoEuskera = forecastText.getString("BASQUE");

            //Guardamos los datos en shared preferences
            Context context = getApplicationContext();
            SharedPreferences prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putFloat("temp_min", (float) tempMin);
            editor.putFloat("temp_max", (float) tempMax);
            editor.putFloat("temp_now", (float) tempNow);
            editor.putString("esp_txt", textoEspanol);
            editor.putString("eusk_txt", textoEuskera);

            editor.apply();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String weather_api_call(String token){
        try {
            LocalDate fechaActual = LocalDate.now();
            String dia = fechaActual.format(DateTimeFormatter.ofPattern("dd"));
            String mes = fechaActual.format(DateTimeFormatter.ofPattern("MM"));
            String anio = fechaActual.format(DateTimeFormatter.ofPattern("yyyy"));

            String endpoint = "https://api.euskadi.eus/euskalmet/weather/regions/basque_country/zones/vitoria_gasteiz/locations/gasteiz/forecast/at/"+anio+"/"+mes+"/"+dia+"/for/"+anio+mes+dia; // Reemplaza con el endpoint correcto
            Log.i("la url","URL:" + endpoint);
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            System.out.println("Código de respuesta: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "ISO-8859-1")
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

    private String generate_JwtToken() {
        try {
            // Cargar la clave privada desde assets
            InputStream inputStream = getApplicationContext().getAssets().open("privateKey.pem");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            String privateKeyPem = stringBuilder.toString()
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
            Dotenv dotenv = Dotenv.configure()
                    .directory("/assets")
                    .filename("env") // Nombre del archivo sin el punto inicial
                    .load();
            String email = dotenv.get("EMAIL");
            // Crear el token JWT
            String token = JWT.create()
                    .withIssuer("sampleApp")
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
