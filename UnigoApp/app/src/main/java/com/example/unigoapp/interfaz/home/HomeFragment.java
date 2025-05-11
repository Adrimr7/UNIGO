package com.example.unigoapp.interfaz.home;

import android.content.Context;
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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        System.out.println("HomeFrag: onCreateView");
        HomeVistaModelo homeVistaModelo =
                new ViewModelProvider(this).get(HomeVistaModelo.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

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
                    }else{
                        Log.e("WORKER", "Error--------------------------------------------------------------------");
                    }
                });
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