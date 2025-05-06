package com.example.unigoapp;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.unigoapp.databinding.ActivityMainBinding;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    // se ha usado la plantilla de la navigation view.

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String idioma = getSharedPreferences("Ajustes", MODE_PRIVATE).
                getString("Idioma", "es");

        ImageButton btnIdioma = new ImageButton(this);
        btnIdioma.setBackgroundColor(Color.TRANSPARENT);
        btnIdioma.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        btnIdioma.setLayoutParams(new Toolbar.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END
        ));
        // botones de idioma
        switch (idioma) {
            case "eu":
                btnIdioma.setImageResource(R.drawable.ic_flag_eu);
                break;
            case "en":
                btnIdioma.setImageResource(R.drawable.ic_flag_en);
                break;
            default:
                btnIdioma.setImageResource(R.drawable.ic_flag_es);
                break;
        }

        setLocale(idioma, false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.addView(btnIdioma);

        btnIdioma.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, btnIdioma);
            popup.getMenuInflater().inflate(R.menu.idioma_menu, popup.getMenu());

            // mostrar iconos en el popup
            try {
                java.lang.reflect.Field mFieldPopup = popup.getClass().getDeclaredField("mPopup");
                mFieldPopup.setAccessible(true);
                Object mPopup = mFieldPopup.get(popup);
                mPopup.getClass().getDeclaredMethod("setForceShowIcon", boolean.class)
                        .invoke(mPopup, true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            popup.setOnMenuItemClickListener(item -> {
                String lang = "es";
                int idiomaPopup = item.getItemId();
                if (idiomaPopup == R.id.idioma_eu) {
                    lang = "eu";
                }
                else if (idiomaPopup == R.id.idioma_en)
                {
                    lang = "en";
                }
                setLocale(lang, true);
                return true;
            });

            popup.show();
        });

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_mapa, R.id.navigation_perfil)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.idioma_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.idioma_es) {
            setLocale("es", true);
            return true;
        } else if (id == R.id.idioma_eu) {
            setLocale("eu", true);
            return true;
        } else if (id == R.id.idioma_en) {
            setLocale("en", true);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private void setLocale(String codigoIdioma, boolean recrearActividad) {
        getSharedPreferences("Ajustes", MODE_PRIVATE)
                .edit()
                .putString("Idioma", codigoIdioma)
                .apply();

        Locale locale = new Locale(codigoIdioma);
        Locale.setDefault(locale);
        
        Configuration config = new Configuration();
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        // eliminar el recreate y hacer que se recarguen solo los textos.
        if (recrearActividad) {
            new Handler(Looper.getMainLooper()).post(this::recreate);
        }
    }

}