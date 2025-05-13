package com.example.unigoapp.interfaz.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.unigoapp.MainActivity;
import com.example.unigoapp.R;
import com.example.unigoapp.databinding.FragmentHomeBinding;
import com.example.unigoapp.interfaz.home.weather.WeatherApiWorker;

public class HomeFragment extends Fragment implements MainActivity.UpdatableFragment {

    private FragmentHomeBinding binding;
    private TextView tvHome;
    private TextView tvOffline;
    private TextView tvMaxTemp;
    private TextView tvMintemp;
    private TextView tvNowtemp;
    private TextView tvforecast;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        System.out.println("HomeFrag: onCreateView");
        HomeVistaModelo homeVistaModelo =
                new ViewModelProvider(this).get(HomeVistaModelo.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        tvOffline = binding.disabledText;
        tvMaxTemp = binding.maxTempText;
        tvMintemp = binding.minTempText;
        tvNowtemp = binding.nowTempText;
        tvforecast = binding.forecast;

        tvHome = binding.tvHome;
        homeVistaModelo.getText().observe(getViewLifecycleOwner(), tvHome::setText);
        callWorker();
        return root;
    }

    public void callWorker(){
        // Crear la solicitud de trabajo
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WeatherApiWorker.class)
                .build();

        // Obtener WorkManager y enviar el trabajo
        WorkManager.getInstance(requireContext())
                .enqueue(workRequest);

        // Observar el resultado usando el LifecycleOwner del Fragment
        WorkManager.getInstance(requireContext())
                .getWorkInfoByIdLiveData(workRequest.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        Log.d("info", "Succes---------------------------------------------------------------------");
                        updateWeather();
                    }else{
                        Log.e("WORKER", "Error--------------------------------------------------------------------");
                    }
                });
    }

    public void updateWeather(){
        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE);

        float tempMin = prefs.getFloat("temp_min", -99);
        float tempMax = prefs.getFloat("temp_max", -99);
        float tempNow = prefs.getFloat("temp_now", -99);

        String forecastEs = prefs.getString("forecast_es", null);
        String forecastEu = prefs.getString("forecast_eu", null);

        Boolean offline = ((MainActivity) requireActivity()).estaOffline();

        Log.e("ONLINE","ESTADO: " + offline);
        Log.e("TEMP_MAX","TEMP: " + tempMax);


        if ((offline) || tempMax == -99){

            tvOffline.setVisibility(View.VISIBLE);
            tvNowtemp.setVisibility(View.INVISIBLE);
            tvMaxTemp.setVisibility(View.INVISIBLE);
            tvMintemp.setVisibility(View.INVISIBLE);
            tvforecast.setVisibility(View.INVISIBLE);
        } else{
            tvOffline.setVisibility(View.INVISIBLE);
            tvNowtemp.setText(String.valueOf(tempNow));
            tvMaxTemp.setText(String.valueOf(tempMax));
            tvMintemp.setText(String.valueOf(tempMin));
            tvforecast.setText(String.valueOf(forecastEs));
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
        tvHome.setText(R.string.texto_home);
    }
}