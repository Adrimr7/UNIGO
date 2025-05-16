package com.example.unigoapp.interfaz.perfil;

import static android.content.Context.MODE_PRIVATE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.unigoapp.MainActivity;
import com.example.unigoapp.R;
import com.example.unigoapp.databinding.FragmentPerfilBinding;

public class PerfilFragment extends Fragment implements MainActivity.UpdatableFragment {

    private FragmentPerfilBinding binding;
    private TextView tvIdioma;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        System.out.println("PerfilFrag: onCreateView");

        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        tvIdioma = binding.tvIdioma;
        binding.btnCastellano.setOnClickListener(v -> actualizarIdioma("es"));
        binding.btnEnglish.setOnClickListener(v -> actualizarIdioma("en"));
        binding.btnEuskera.setOnClickListener(v -> actualizarIdioma("eu"));

        return root;
    }

    private void actualizarIdioma(String codIdioma) {
        System.out.println("PFragment: Actualizar idioma");

        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.getSharedPreferences("Ajustes", MODE_PRIVATE)
                .edit()
                .putString("Idioma", codIdioma)
                .apply();
        mainActivity.actualizarContextoLocaleParaFragments(mainActivity, codIdioma);
        actualizarTextos();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void actualizarTextos() {
        System.out.println("PerfilFrag: actualizarTextos");
        tvIdioma.setText(R.string.cambiar_idioma);
    }
}