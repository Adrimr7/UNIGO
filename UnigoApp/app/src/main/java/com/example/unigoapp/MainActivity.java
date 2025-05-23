package com.example.unigoapp;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.unigoapp.utils.GrafosSingleton;
import com.example.unigoapp.utils.ToastPersonalizado;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.unigoapp.databinding.ActivityMainBinding;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private ImageButton btnIdioma;
    private boolean estaOffline = true;
    private Handler handlerConexion;
    private static final long INTERVALO_COMPROBAR_CONEXION = 2000; // 2 segs


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hilo andar
        long tiempoInicio = System.currentTimeMillis();
        new Thread(() -> {
            GrafosSingleton.getGrafoAndar(this);
            long endTime = System.currentTimeMillis();
            System.out.println("TiempoEjecucion: Grafo ANDAR cargados en " + (endTime - tiempoInicio) + " ms");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                ToastPersonalizado.showToast(getApplicationContext(), getString(R.string.ya_se_pueden_ver_rutas_andando));
            }, 47000);
        }).start();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        comprobarConexionInternet();
        empezarComprobacionesConexion();

        // hilo tranvia, buses y bici
        new Thread(() -> {
            GrafosSingleton.getGrafoTranvia(this);
            long endTime3 = System.currentTimeMillis();
            System.out.println("TiempoEjecucion: Grafo TRANVIA en " + (endTime3 - tiempoInicio) + " ms");

            // se cargan desde binario, por tanto no es necesario comprobar sus tiempos
            GrafosSingleton.getGrafoBuses(this);
            GrafosSingleton.getGrafoBici(this);
        }).start();

        // cargar idioma
        String idioma = getSharedPreferences("Ajustes", MODE_PRIVATE).getString("Idioma", "es");
        setLocale(idioma, false);

        cargarToolbar(idioma);
        cargarNavigation();

        actualizarFragmentos();
        actualizarNavbar();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // aplicar el idioma antes de que se creen las vistas
        String idioma = newBase.getSharedPreferences("Ajustes", MODE_PRIVATE)
                .getString("Idioma", "es");
        super.attachBaseContext(actualizarContextoLocale(newBase, idioma));
    }

    public void actualizarContextoLocaleParaFragments(Context context, String codigoIdioma) {
        System.out.println("MainActivity: actualizarContextoLocale");
        setLocale(codigoIdioma, true);

        actualizarFragmentos();
        actualizarTituloToolbar();
        actualizarNavbar();
    }

    public Context actualizarContextoLocale(Context context, String codigoIdioma) {
        System.out.println("MainActivity: actualizarContextoLocale");
        Locale locale = new Locale(codigoIdioma);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);

    }

    private void cargarToolbar(String idioma) {
        Toolbar toolbar = binding.toolbar;
        if (toolbar == null) {
            toolbar = findViewById(R.id.toolbar);
            if (toolbar == null) {
                throw new IllegalStateException("Toolbar no encontrado en el layout");
            }
        }

        setSupportActionBar(toolbar);

        // config btnIdioma
        btnIdioma = new ImageButton(this);
        btnIdioma.setBackgroundColor(Color.TRANSPARENT);
        btnIdioma.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        btnIdioma.setLayoutParams(new Toolbar.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END
        ));
        btnIdioma.setPadding(0,0,24,0);

        toolbar.addView(btnIdioma);
        actualizarBotonIdioma(idioma);
    }

    private void actualizarBotonIdioma(String codigoIdioma) {
        if (codigoIdioma.equals("eu")) {
            btnIdioma.setImageResource(R.drawable.ic_flag_eu);
        } else if (codigoIdioma.equals("en")) {
            btnIdioma.setImageResource(R.drawable.ic_flag_en);
        } else {
            btnIdioma.setImageResource(R.drawable.ic_flag_es);
        }
    }

    private void cargarNavigation() {
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_mapa, R.id.navigation_perfil)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            actualizarTituloToolbar();

            // actualizar textos
            new Handler().postDelayed(() -> {
                if (obtenerFragmentActual() instanceof UpdatableFragment) {
                    ((UpdatableFragment) obtenerFragmentActual()).actualizarTextos();
                }
            }, 100); // pequeno delay
        });
    }

    private Fragment obtenerFragmentActual() {
        return getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main)
                .getChildFragmentManager()
                .getPrimaryNavigationFragment();
    }

    private void setLocale(String codigoIdioma, boolean act) {

        // funcion de setLocale. Fuente: docs de android.
        getSharedPreferences("Ajustes", MODE_PRIVATE)
                .edit()
                .putString("Idioma", codigoIdioma)
                .apply();

        Locale locale = new Locale(codigoIdioma);
        Locale.setDefault(locale);

        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());

        actualizarContextoLocale(this, codigoIdioma);

        if (act) {
            new Handler(Looper.getMainLooper()).post(() -> {
                actualizarBotonIdioma(codigoIdioma);
                actualizarNavbar();
                actualizarFragmentos();
            });
        }

    }

    private void actualizarFragmentos() {
        System.out.println("MainActivity: actualizarFragmentos");
        // obtener el fragment actual y actualizar sus textos
        Fragment frag = obtenerFragmentActual();
        if (frag instanceof UpdatableFragment) {
            ((UpdatableFragment) frag).actualizarTextos();
        }
    }

    private void actualizarNavbar() {
        System.out.println("MainActivity: actualizarNavbar");
        BottomNavigationView navView = findViewById(R.id.nav_view);
        Menu menu = navView.getMenu();
        menu.findItem(R.id.navigation_home).setTitle(R.string.titulo_home);
        menu.findItem(R.id.navigation_mapa).setTitle(R.string.titulo_mapa);
        menu.findItem(R.id.navigation_perfil).setTitle(R.string.titulo_perfil);

        actualizarTituloToolbar();

    }

    private void actualizarTituloToolbar() {
        // hay que hacer una funcion separada porque el action bar funciona
        // de una manera un poco curiosa
        System.out.println("MainActivity: actualizarTituloToolbar");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            // no hacemos nada mas porque ya no se usa el titulo
            // ahora esta el logo
        }
    }

    private void comprobarConexionInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo redActiva = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = redActiva != null && redActiva.isConnectedOrConnecting();
        if (!estaOffline && isConnected) {
            System.out.println("INTERNET: Hay conexion");
        }
        else if (estaOffline && !isConnected) {
            System.out.println("INTERNET: Sigue sin haber conexion.");
        }
        else if (!isConnected) {
            estaOffline = true;
            ToastPersonalizado.showToast(this,"Se ha perdido la conexión a Internet.");
        }
        else {
            estaOffline = false;
            ToastPersonalizado.showToast(this, "Se ha recuperado la conexión a Internet.");
        }
    }

    private void empezarComprobacionesConexion() {
        handlerConexion = new Handler(Looper.getMainLooper());
        handlerConexion.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean estabaOffline = estaOffline;
                comprobarConexionInternet();
                if (estabaOffline != estaOffline) {
                    // actualizar fragmentos porque no hay conexion
                    actualizarFragmentos();
                }
                handlerConexion.postDelayed(this, INTERVALO_COMPROBAR_CONEXION);
            }
        }, INTERVALO_COMPROBAR_CONEXION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handlerConexion != null) {
            handlerConexion.removeCallbacksAndMessages(null);
        }
    }

    public boolean estaOffline() {
        return estaOffline;
    }

    // interfaz para definir como son los fragments
    public interface UpdatableFragment {
        void actualizarTextos();
    }
}