package com.example.unigoapp.interfaz.mapa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextThemeWrapper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.unigoapp.R;
import com.example.unigoapp.MainActivity;
import com.example.unigoapp.databinding.FragmentMapaBinding;
import com.example.unigoapp.utils.ToastPersonalizado;
import com.example.unigoapp.utils.GrafosSingleton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MapaFragment extends Fragment implements MainActivity.UpdatableFragment {

    private FragmentMapaBinding binding;
    private TextView tvMapa;
    private MapView mvMapa;
    private static final GeoPoint PUNTO_UNIVERSIDAD = new GeoPoint(42.839536, -2.670918);
    private static final GeoPoint CENTRO_GASTEIZ = new GeoPoint(42.846172, -2.673754);
    private static final int PERMISO_UBICACION = 0;
    private Polyline rutaActual;
    private Marker markerDestino;
    private Marker markerOrigen;
    private ProgressDialog progressDialog;
    private FloatingActionButton fabOpciones;
    private ScheduledExecutorService scheduler;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        System.out.println("MapaFrag: onCreateView");
        MapaVistaModelo mapaVistaModelo = new ViewModelProvider(this).get(MapaVistaModelo.class);

        binding = FragmentMapaBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mvMapa = binding.mapView;
        tvMapa = binding.tvMapa;
        fabOpciones = binding.fabOptions;

        mapaVistaModelo.getText().observe(getViewLifecycleOwner(), tvMapa::setText);

        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        ImageButton btnCenter = root.findViewById(R.id.btn_center);
        btnCenter.setOnClickListener(v -> centrarMapaEnGasteiz());
        System.out.println("ProcesoANDAR es: " + GrafosSingleton.getProcesoAndar());
        if (GrafosSingleton.getProcesoAndar() != 2)
        {
            progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage(getString(R.string.cargando_grafos));
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleWithFixedDelay(() -> {

                    int procesoAndar = GrafosSingleton.getProcesoAndar();
                    if (procesoAndar == 2) {
                        requireActivity().runOnUiThread(() -> {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        });

                        long endTime = System.currentTimeMillis();
                        System.out.println("TiempoEjecucion: Grafos cargados en " + (endTime - startTime) + " ms");
                        scheduler.shutdown();
                    }
                    // empieza a los 10 segundos, cada segundo comprueba
                }, 0, 1, TimeUnit.SECONDS);
            }).start();
        }
        else {
            //ToastPersonalizado.showToast(getContext(), "");
        }

        mvMapa.setOnTouchListener((v, event) -> false); // necesario para que reciba los clicks
        mvMapa.getOverlays().add(new MapEventsOverlay(requireContext(), new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                System.out.println("Ha pulsado una vez");
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                System.out.println("Ha mantenido pulsado");
                return false;
            }
        }));

        fabOpciones.setOnClickListener(this::mostrarOpcionesRuta);
        
        configurarMapa();
        System.out.println("Mapa configurado");
        comprobarPermisoUbicacion();

        return root;
    }

    private void comprobarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.permiso_ubi)
                        .setMessage(R.string.acceso_ubi)
                        .setPositiveButton(R.string.entendido, (dialog, which) ->
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

            MyLocationNewOverlay ubicacionOverlay = new MyLocationNewOverlay(
                    new GpsMyLocationProvider(requireContext()), mvMapa);
            ubicacionOverlay.enableMyLocation();
            mvMapa.getOverlays().add(ubicacionOverlay);
        }
    }

    private void configurarMapa() {
        System.out.println("MapaFrag: configurarMapa");

        // cache y paths
        File basePath = new File(requireContext().getCacheDir().getAbsolutePath(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(basePath);
        File tileCache = new File(basePath, "tiles");
        Configuration.getInstance().setOsmdroidTileCache(tileCache);
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        // limites de tiles
        Configuration.getInstance().setCacheMapTileCount((short)12);
        Configuration.getInstance().setCacheMapTileOvershoot((short)12);
        Configuration.getInstance().setTileDownloadThreads((short)4);
        Configuration.getInstance().setTileFileSystemThreads((short)4);

        mvMapa.setTileSource(TileSourceFactory.MAPNIK);

        // mirar si la app esta offline, y si no, usar lo cargado
        boolean isOffline = ((MainActivity) requireActivity()).estaOffline();
        mvMapa.setUseDataConnection(!isOffline);

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

        Marker marker = crearMarkerConTexto(requireContext(), mvMapa, CENTRO_GASTEIZ, "Vitoria-Gasteiz", R.drawable.ic_mi_ubicacion);
        mvMapa.getOverlays().add(marker);
        mvMapa.invalidate();

        /*
        Marker marker = new Marker(mvMapa);
        marker.setPosition(CENTRO_GASTEIZ);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        Drawable iconoModerno = ContextCompat.getDrawable(requireContext(), R.drawable.ic_mi_ubicacion);
        marker.setIcon(iconoModerno);
        marker.setTitle("Vitoria-Gasteiz");
        mvMapa.getOverlays().add(marker);
        centrarMapaEnGasteiz();
        */
        // esto es solo para que se vea en el mapa.
        // hay que hacerlo cuando el usuario lo especifique
        /*
        Thread hiloAndar = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            cargarAndar();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("TiempoEjecucion" + "cargarAndar tarda: " + duration + " ms");
        });
        hiloAndar.start();

        Thread hiloBicis = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            cargarCarrilesBici();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("TiempoEjecucion" + "cargarCarrilesBici tarda: " + duration + " ms");
        });
        hiloBicis.start();

        Thread hiloBuses = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            cargarBuses();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("TiempoEjecucion" + "cargarCarrilesBici tarda: " + duration + " ms");
        });
        hiloBuses.start();
        */

    }

    private Marker crearMarkerConTexto(Context context, MapView mapView, GeoPoint posicion, String textoLabel, int iconoResId) {

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        // crear el texto bonito
        TextView texto = new TextView(context);
        texto.setText(textoLabel);
        texto.setTextColor(Color.BLACK);
        texto.setTextSize(14);
        texto.setTypeface(Typeface.DEFAULT_BOLD);
        texto.setPadding(16, 8, 16, 8);
        texto.setBackground(ContextCompat.getDrawable(context, R.drawable.texto_marker_background));
        texto.setShadowLayer(4, 2, 2, Color.LTGRAY); // sombras
        ImageView icono = new ImageView(context);
        icono.setImageDrawable(ContextCompat.getDrawable(context, iconoResId));

        layout.addView(texto);
        layout.addView(icono);

        // medir y dibujar en el bitmap
        layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        layout.layout(0, 0, layout.getMeasuredWidth(), layout.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(layout.getMeasuredWidth(), layout.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        layout.draw(canvas);

        Drawable drawableFinal = new BitmapDrawable(context.getResources(), bitmap);

        Marker marker = new Marker(mapView);
        marker.setPosition(posicion);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(drawableFinal);

        return marker;
    }


    private void centrarMapaEnGasteiz() {
        mvMapa.getController().animateTo(CENTRO_GASTEIZ);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        binding = null;
    }

    @Override
    public void actualizarTextos() {
        System.out.println("MapaFrag: actualizarTextos");
        tvMapa.setText("");

        // checkear si ha cambiado el estado
        boolean isOffline = ((MainActivity) requireActivity()).estaOffline();
        mvMapa.setUseDataConnection(!isOffline);
        if (isOffline) {
            ToastPersonalizado.showToast(requireContext(), getString(R.string.mapa_sin_conexion));
        }
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

    private void calcularYMostrarRutaBici(GeoPoint destino) {

        List<GeoPoint> ruta = GrafosSingleton.calcRutaBici(destino, PUNTO_UNIVERSIDAD);

        if (ruta == null || ruta.isEmpty()) return;

        if (rutaActual != null) {
            mvMapa.getOverlays().remove(rutaActual);
        }

        rutaActual = new Polyline();
        rutaActual.setPoints(ruta);
        rutaActual.setColor(ContextCompat.getColor(requireContext(), R.color.purple_500));
        rutaActual.setWidth(11f);

        mvMapa.getOverlays().add(rutaActual);
        mvMapa.invalidate();
    }

    private void calcularYMostrarRutaBus(GeoPoint destino) {

        RutaInfo rutaInfo = GrafosSingleton.calcRutaBus(destino, PUNTO_UNIVERSIDAD);
        List<GeoPoint> ruta = rutaInfo.getPuntos();

        if (ruta == null || ruta.isEmpty()) return;

        if (rutaActual != null) {
            mvMapa.getOverlays().remove(rutaActual);
        }
        if (markerOrigen != null) {
            mvMapa.getOverlays().remove(markerOrigen);
            mvMapa.getOverlays().remove(markerDestino);
        }

        rutaActual = new Polyline();
        rutaActual.setPoints(ruta);
        rutaActual.setColor(ContextCompat.getColor(requireContext(), R.color.purple_200));
        rutaActual.setWidth(11f);

        mvMapa.getOverlays().add(rutaActual);

        markerOrigen = crearMarkerConTexto(
                requireContext(),
                mvMapa,
                rutaInfo.getEstacionOrigen(),
                getString(R.string.origen) + rutaInfo.getNombreOrigen() + "\n" + getString(R.string.salida_a) + rutaInfo.getProximoBus(),
                R.drawable.ic_bus
        );
        mvMapa.getOverlays().add(markerOrigen);

        ToastPersonalizado.showToast(getContext(), getString(R.string.bus_sale_a) + rutaInfo.getProximoBus());
        markerDestino = crearMarkerConTexto(
                requireContext(),
                mvMapa,
                rutaInfo.getEstacionDestino(),
                getString(R.string.destino) + rutaInfo.getNombreDestino() + "\n" + getString(R.string.llegada_a) + rutaInfo.getTiempoEstimado(),
                R.drawable.ic_bus
        );
        mvMapa.getOverlays().add(markerDestino);

        mvMapa.invalidate();
    }

    private void calcularYMostrarRutaTranvia(GeoPoint destino) {

        List<GeoPoint> ruta = GrafosSingleton.calcRutaTranvia(destino, PUNTO_UNIVERSIDAD);

        if (ruta == null || ruta.isEmpty()) return;

        if (rutaActual != null) {
            mvMapa.getOverlays().remove(rutaActual);
        }

        rutaActual = new Polyline();
        rutaActual.setPoints(ruta);
        rutaActual.setColor(ContextCompat.getColor(requireContext(), R.color.purple_200));
        rutaActual.setWidth(11f);

        mvMapa.getOverlays().add(rutaActual);
        mvMapa.invalidate();
    }

    /*
    private void cargarCarrilesBici() {
        try {
            InputStream is = requireContext().getAssets().open("viasciclistas23.geojson");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject geojson = new JSONObject(json);
            JSONArray features = geojson.getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                if (feature.isNull("geometry")) continue;
                JSONObject geometry = feature.getJSONObject("geometry");
                String tipo = geometry.getString("type");

                if (tipo.equals("LineString")) {
                    JSONArray coords = geometry.getJSONArray("coordinates");

                    Polyline polyline = new Polyline();
                    polyline.setColor(ContextCompat.getColor(requireContext(), R.color.teal_700));
                    polyline.setWidth(4.0f);

                    for (int j = 0; j < coords.length(); j++) {
                        JSONArray punto = coords.getJSONArray(j);
                        double lon = punto.getDouble(0);
                        double lat = punto.getDouble(1);
                        polyline.addPoint(new GeoPoint(lat, lon));
                    }

                    mvMapa.getOverlays().add(polyline);
                }
            }

            mvMapa.invalidate(); // refresca el mapa
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

        private void cargarBuses() {
            try {
                InputStream is = requireContext().getAssets().open("rutas_buses.geojson");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String json = new String(buffer, StandardCharsets.UTF_8);

                JSONObject geojson = new JSONObject(json);
                JSONArray features = geojson.getJSONArray("features");

                for (int i = 0; i < features.length(); i++) {
                    JSONObject feature = features.getJSONObject(i);
                    if (feature.isNull("geometry")) continue;
                    JSONObject geometry = feature.getJSONObject("geometry");
                    String tipo = geometry.getString("type");

                    if (tipo.equals("LineString")) {
                        JSONArray coords = geometry.getJSONArray("coordinates");

                        Polyline polyline = new Polyline();
                        polyline.setColor(ContextCompat.getColor(requireContext(), R.color.purple_200));
                        polyline.setWidth(4.0f);

                        for (int j = 0; j < coords.length(); j++) {
                            JSONArray punto = coords.getJSONArray(j);
                            double lon = punto.getDouble(0);
                            double lat = punto.getDouble(1);
                            polyline.addPoint(new GeoPoint(lat, lon));
                        }

                        mvMapa.getOverlays().add(polyline);
                    }
                }

                mvMapa.invalidate(); // refresca el mapa
            } catch (JSONException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void cargarAndar() {
            try {
                InputStream is = requireContext().getAssets().open("mapaandando_utf8.geojson");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String json = new String(buffer, StandardCharsets.UTF_8);

                JSONObject geojson = new JSONObject(json);
                JSONArray features = geojson.getJSONArray("features");

                for (int i = 0; i < features.length(); i++) {
                    JSONObject feature = features.getJSONObject(i);
                    if (feature.isNull("geometry")) continue;
                    JSONObject geometry = feature.getJSONObject("geometry");
                    String tipo = geometry.getString("type");

                    if (tipo.equals("LineString")) {
                        JSONArray coords = geometry.getJSONArray("coordinates");

                        Polyline polyline = new Polyline();
                        polyline.setColor(ContextCompat.getColor(requireContext(), R.color.purple_200));
                        polyline.setWidth(4.0f);

                        for (int j = 0; j < coords.length(); j++) {
                            JSONArray punto = coords.getJSONArray(j);
                            double lon = punto.getDouble(0);
                            double lat = punto.getDouble(1);
                            polyline.addPoint(new GeoPoint(lat, lon));
                        }

                        mvMapa.getOverlays().add(polyline);
                    }
                }

                mvMapa.invalidate(); // refresca el mapa
            } catch (JSONException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        */
    private void calcularYMostrarRutaAndando(GeoPoint destino) {

        List<GeoPoint> ruta = GrafosSingleton.calcRutaAndar(destino, PUNTO_UNIVERSIDAD);

        if (ruta == null || ruta.isEmpty()) return;

        if (rutaActual != null) {
            mvMapa.getOverlays().remove(rutaActual);
        }

        rutaActual = new Polyline();
        rutaActual.setPoints(ruta);
        rutaActual.setColor(ContextCompat.getColor(requireContext(), R.color.purple_200));
        rutaActual.setWidth(10f);

        mvMapa.getOverlays().add(rutaActual);
        mvMapa.invalidate();
    }
    private void mostrarOpcionesRuta(View view) {
        System.out.println("MFragment: mostrarOpcionesRuta");
        Context wrapper = new ContextThemeWrapper(getContext(), R.style.Widget_App_PopupMenu);
        PopupMenu popupMenu = new PopupMenu(wrapper, view);
        popupMenu.getMenuInflater().inflate(R.menu.opciones_ruta_menu, popupMenu.getMenu());
        

        try {
            @SuppressLint("DiscouragedPrivateApi") Field field = PopupMenu.class.getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuPopupHelper = field.get(popupMenu);
            Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
            Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
            setForceShowIcon.invoke(menuPopupHelper, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.opcion_caminar) {
                calcularRutaOpcion("andar");
                return true;
            } else if (itemId == R.id.opcion_bici) {
                calcularRutaOpcion("bici");
                return true;
            } else if (itemId == R.id.opcion_bus) {
                calcularRutaOpcion("bus");
                return true;
            }
            else if (itemId == R.id.opcion_tranvia){
                calcularRutaOpcion("tranvia");
                return true;
            }
            return false;
        });
    }

    private void calcularRutaOpcion(String tipo) {
        System.out.println("MFragment: calcularRuta: " + tipo);
        if (tipo.equals("andar")) {
            if (GrafosSingleton.getProcesoAndar() == 1)
            {
                ToastPersonalizado.showToast(requireContext(), getString(R.string.cargando_grafos));
            }
            else {
                progressDialog = new ProgressDialog(requireContext());
                progressDialog.setMessage(getString(R.string.calc_andando));
                progressDialog.setCancelable(false);
                progressDialog.show();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // TODO: cambiar ubicacion a la actual.
                    calcularYMostrarRutaAndando(CENTRO_GASTEIZ);
                    progressDialog.dismiss();
                }, 100);
            }

        }
        else if (tipo.equals("bici")) {
            if (GrafosSingleton.getProcesoBici() == 1)
            {
                ToastPersonalizado.showToast(requireContext(), getString(R.string.cargando_grafos));
            }
            else {
                progressDialog = new ProgressDialog(requireContext());
                progressDialog.setMessage(getString(R.string.calc_bici));
                progressDialog.setCancelable(false);
                progressDialog.show();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // TODO: cambiar ubicacion a la actual.
                    calcularYMostrarRutaBici(CENTRO_GASTEIZ);
                    progressDialog.dismiss();
                }, 100);
            }
        }
        else if (tipo.equals("bus")) {

            progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage(getString(R.string.calc_bus));
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // TODO: cambiar ubicacion a la actual.
                calcularYMostrarRutaBus(CENTRO_GASTEIZ);
                progressDialog.dismiss();
            }, 100);
        }
        else if (tipo.equals("tranvia")) {

            progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage(getString(R.string.calc_tranvia));
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // TODO: cambiar ubicacion a la actual.
                calcularYMostrarRutaTranvia(CENTRO_GASTEIZ);
                progressDialog.dismiss();
            }, 100);
        }
    }
}