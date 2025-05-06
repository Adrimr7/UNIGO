package com.example.unigoapp.interfaz.mapa;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.unigoapp.R;
import com.example.unigoapp.MainActivity;
import com.example.unigoapp.databinding.FragmentMapaBinding;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


public class MapaFragment extends Fragment implements MainActivity.UpdatableFragment {

    private FragmentMapaBinding binding;
    private TextView tvMapa;
    private MapView mvMapa;
    private static final GeoPoint CENTRO_GASTEIZ = new GeoPoint(42.846718, -2.671635);
    private static final int PERMISO_UBICACION = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        System.out.println("MapaFrag: onCreateView");
        MapaVistaModelo mapaVistaModelo =
                new ViewModelProvider(this).get(MapaVistaModelo.class);

        binding = FragmentMapaBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mvMapa = binding.mapView;
        tvMapa = binding.tvMapa;

        mapaVistaModelo.getText().observe(getViewLifecycleOwner(), tvMapa::setText);

        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        ImageButton btnCenter = root.findViewById(R.id.btn_center);

        configurarMapa();

        comprobarPermisoUbicacion();

        btnCenter.setOnClickListener(v -> centrarMapaEnGasteiz());

        return root;
    }

    private void comprobarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Permiso de ubicación")
                        .setMessage("Necesitamos acceso a tu ubicación para mostrarte rutas precisas")
                        .setPositiveButton("Entendido", (dialog, which) ->
                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        PERMISO_UBICACION))
                        .show();
            } else {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION}, PERMISO_UBICACION);
            }
        } else {
            habilitarUbicacion();
        }
    }
    private void habilitarUbicacion() {
        System.out.println("MapaFrag: habilitarUbicacion");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mvMapa.getOverlays().add(new MyLocationNewOverlay(
                    new GpsMyLocationProvider(requireContext()),
                    mvMapa
            ));
        }
    }

    private void configurarMapa() {
        System.out.println("MapaFrag: configurarMapa");
        mvMapa.setTileSource(TileSourceFactory.MAPNIK);

        mvMapa.setMultiTouchControls(true);
        mvMapa.setBuiltInZoomControls(false);
        mvMapa.setTilesScaledToDpi(true);
        mvMapa.setExpectedCenter(CENTRO_GASTEIZ);


        IMapController controladorMapa =  mvMapa.getController();
        controladorMapa.setZoom(15);
        controladorMapa.setCenter(CENTRO_GASTEIZ);
        mvMapa.setScrollableAreaLimitDouble(
                new BoundingBox(43.1, -2.4, 42.7, -2.8)
        );
        mvMapa.setMinZoomLevel(10.9);
        Marker marker = new Marker(mvMapa);
        marker.setPosition(CENTRO_GASTEIZ);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Vitoria-Gasteiz");
        mvMapa.getOverlays().add(marker);
        centrarMapaEnGasteiz();

    }

    private void centrarMapaEnGasteiz() {
        mvMapa.getController().animateTo(CENTRO_GASTEIZ);
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

    @Override
    public void onResume() {
        super.onResume();
        mvMapa.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mvMapa.onPause();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISO_UBICACION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            habilitarUbicacion();
        }
    }
}