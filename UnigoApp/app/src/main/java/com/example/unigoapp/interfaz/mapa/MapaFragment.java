package com.example.unigoapp.interfaz.mapa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.unigoapp.R;
import com.example.unigoapp.MainActivity;
import com.example.unigoapp.databinding.FragmentMapaBinding;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapaFragment extends Fragment implements MainActivity.UpdatableFragment {

    private FragmentMapaBinding binding;
    private TextView tvMapa;
    private MapView mvMapa;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        System.out.println("MapaFrag: onCreateView");
        MapaVistaModelo mapaVistaModelo =
                new ViewModelProvider(this).get(MapaVistaModelo.class);

        binding = FragmentMapaBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        tvMapa = binding.tvMapa;
        mapaVistaModelo.getText().observe(getViewLifecycleOwner(), tvMapa::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void actualizarTextos() {
        System.out.println("MapaFrag: actualizarTextos");
        tvMapa.setText("");
    }
}