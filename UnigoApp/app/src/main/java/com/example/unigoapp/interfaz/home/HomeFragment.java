package com.example.unigoapp.interfaz.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.unigoapp.MainActivity;
import com.example.unigoapp.R;
import com.example.unigoapp.databinding.FragmentHomeBinding;

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
        return root;
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