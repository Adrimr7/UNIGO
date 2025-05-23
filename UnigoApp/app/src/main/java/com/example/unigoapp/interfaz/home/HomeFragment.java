package com.example.unigoapp.interfaz.home;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.unigoapp.MainActivity;
import com.example.unigoapp.R;
import com.example.unigoapp.databinding.FragmentHomeBinding;
import com.example.unigoapp.interfaz.home.weather.AirApiWorker;
import com.example.unigoapp.interfaz.home.weather.WeatherApiWorker;

public class HomeFragment extends Fragment implements MainActivity.UpdatableFragment {

    private FragmentHomeBinding binding;
    private TextView tvOfflineWeather;
    private TextView tvMaxTemp;
    private TextView tvMintemp;
    private TextView tvNowtemp;
    private TextView tvforecast;
    private TextView tvOfflineAir;
    private TextView tvAirQuality;
    private ImageView ivWeather;
    private TextView tvTempTitle;
    private TextView tvForecastTitle;
    private TextView tvAirTitle;
    private ImageView ivMax;
    private ImageView ivMin;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        System.out.println("HomeFrag: onCreateView");

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        tvOfflineWeather = binding.disabledTextWeather;
        tvMaxTemp = binding.maxTempText;
        tvMintemp = binding.minTempText;
        tvNowtemp = binding.nowTempText;
        tvforecast = binding.forecast;
        tvAirQuality = binding.airQuallityText;
        ivWeather = binding.weatherImg;
        tvTempTitle = binding.tempTitle;
        tvForecastTitle = binding.forecastTitle;
        tvAirTitle = binding.airTitle;
        ivMax = binding.maxImg;
        ivMin = binding.minImg;

        tvOfflineAir = binding.disabledTextAir;

        callWeatherWorker();
        callAirWorker();
        return root;
    }

    private void callAirWorker(){
        // solicitud de worker, enviar el workRequest y usar el lifecycleOwner del fragment
        // para ver el resultado
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AirApiWorker.class)
                .build();
        WorkManager.getInstance(requireContext())
                .enqueue(workRequest);
        WorkManager.getInstance(requireContext())
                .getWorkInfoByIdLiveData(workRequest.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        Log.d("info", "Succes---------------------------------------------------------------------");
                        updateAir();
                    }else{
                        Log.e("WORKER", "Error--------------------------------------------------------------------");
                    }
                });
    }

    private void updateAir(){
        Context context = getContext();

        SharedPreferences airPrefs = context.getSharedPreferences("air", MODE_PRIVATE);
        String language = context.getSharedPreferences("Ajustes", MODE_PRIVATE).getString("Idioma", "es");


        String quality_es = airPrefs.getString("quality", null);
        String quality_eu = airPrefs.getString("quality_eusk", null);


        Boolean offline = ((MainActivity) requireActivity()).estaOffline();

        Log.e("ONLINE","ESTADO: " + offline);
        Log.e("AIRE","CALIDAD: " + quality_es);


        if ((offline) || quality_es == null || quality_eu == null){
            tvOfflineAir.setVisibility(View.VISIBLE);
            tvAirQuality.setVisibility(View.INVISIBLE);
        } else{
            tvOfflineAir.setVisibility(View.INVISIBLE);
            tvAirQuality.setVisibility(View.VISIBLE);
            if (language.equals("es")) {
                tvAirQuality.setText(String.valueOf(quality_es));
            } else if (language.equals("eu")) {
                tvAirQuality.setText(String.valueOf(quality_eu));
            } else{
                tvAirQuality.setText("No data for this language");
            }

        }
    }

    private void callWeatherWorker(){
        // solicitud de worker, enviar el workRequest y usar el lifecycleOwner del fragment
        // para ver el resultado
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WeatherApiWorker.class)
                .build();
        WorkManager.getInstance(requireContext())
                .enqueue(workRequest);
        WorkManager.getInstance(requireContext())
                .getWorkInfoByIdLiveData(workRequest.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        Log.d("info", "Success---------------------------------------------------------------------");
                        updateWeather();
                    }else{
                        Log.e("WORKER", "Error--------------------------------------------------------------------");
                    }
                });
    }

    public void updateWeather(){
        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences("weather", MODE_PRIVATE);
        String language = context.getSharedPreferences("Ajustes", MODE_PRIVATE).getString("Idioma", "es");

        int tempMin = (int) prefs.getFloat("temp_min", -99);
        int tempMax = (int) prefs.getFloat("temp_max", -99);
        int tempNow = (int) prefs.getFloat("temp_now", -99);

        String forecastEs = prefs.getString("esp_txt", null);
        String forecastEu = prefs.getString("eusk_txt", null);

        Boolean offline = ((MainActivity) requireActivity()).estaOffline();

        Log.e("ONLINE","ESTADO: " + offline);
        Log.e("TEMP_MAX","TEMP: " + tempMax);


        if ((offline) || tempMax == -99){

            tvOfflineWeather.setVisibility(View.VISIBLE);
            tvNowtemp.setVisibility(View.INVISIBLE);
            tvMaxTemp.setVisibility(View.INVISIBLE);
            tvMintemp.setVisibility(View.INVISIBLE);
            tvforecast.setVisibility(View.INVISIBLE);
            ivMax.setVisibility(View.INVISIBLE);
            ivMin.setVisibility(View.INVISIBLE);
            tvForecastTitle.setVisibility(View.INVISIBLE);
            ivWeather.setVisibility(View.INVISIBLE);
            tvNowtemp.setText(String.valueOf(tempNow+"ºC"));
            tvMaxTemp.setText(String.valueOf(tempMax+"º"));
            tvMintemp.setText(String.valueOf(tempMin+"º"));

        } else{
            selectWeatherIcon(forecastEs);
            ivMax.setVisibility(View.VISIBLE);
            ivMin.setVisibility(View.VISIBLE);
            tvForecastTitle.setVisibility(View.VISIBLE);
            tvforecast.setVisibility(View.VISIBLE);
            tvMaxTemp.setVisibility(View.VISIBLE);
            tvMintemp.setVisibility(View.VISIBLE);
            tvNowtemp.setVisibility(View.VISIBLE);
            ivWeather.setVisibility(View.VISIBLE);
            tvOfflineWeather.setVisibility(View.INVISIBLE);
            tvNowtemp.setText(String.valueOf(tempNow+"ºC"));
            tvMaxTemp.setText(String.valueOf(tempMax+"º"));
            tvMintemp.setText(String.valueOf(tempMin+"º"));
            String e = (String) tvMaxTemp.getText();
            if (language.equals("es")) {
                tvforecast.setText(forecastEs);
            } else if (language.equals("eu")) {
                tvforecast.setText(forecastEu);
            } else{
                tvforecast.setText("There is not forecast data for the selected language.");
            }
        }

    }

    private void selectWeatherIcon(String forecast){
        if (forecast.contains("torm")) {
            ivWeather.setImageResource(R.drawable.storm);
        } else if (forecast.contains("chub") || (forecast.contains("lluv"))){
            ivWeather.setImageResource(R.drawable.rain);
        } else if (forecast.contains("nub")) {
            ivWeather.setImageResource(R.drawable.cloud);
        } else if (forecast.contains("sol")){
            ivWeather.setImageResource(R.drawable.sun);
        } else{
            ivWeather.setImageResource(R.drawable.sun);
        }

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    @Override
    public void actualizarTextos() {
        System.out.println("HomeFrag: actualizarTextos");
        updateWeather();
        updateAir();
        tvTempTitle.setText(R.string.temperatura);
        tvForecastTitle.setText(R.string.pronostico);
        tvAirTitle.setText(R.string.calidad_aire);
        tvOfflineWeather.setText(R.string.offline);
        tvOfflineAir.setText(R.string.offline);

    }
}