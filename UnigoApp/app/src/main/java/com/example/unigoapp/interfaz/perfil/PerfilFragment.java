package com.example.unigoapp.interfaz.perfil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.unigoapp.MainActivity;
import com.example.unigoapp.R;
import com.example.unigoapp.databinding.FragmentPerfilBinding;

public class PerfilFragment extends Fragment implements MainActivity.UpdatableFragment {

    private FragmentPerfilBinding binding;
    private CardView cvIdioma;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        System.out.println("PerfilFrag: onCreateView");

        PerfilVistaModelo perfilVistaModelo =
                new ViewModelProvider(this).get(PerfilVistaModelo.class);

        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        cvIdioma = binding.languageCard;


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void actualizarTextos() {
        System.out.println("PerfilFrag: actualizarTextos");
    }
}