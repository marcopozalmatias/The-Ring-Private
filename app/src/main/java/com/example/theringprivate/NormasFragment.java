package com.example.theringprivate;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class NormasFragment extends Fragment {

    public NormasFragment() {
        super(R.layout.layout_fullscreen_legal);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView txtTituloLegal = view.findViewById(R.id.txtTituloLegal);
        TextView txtContenidoLegal = view.findViewById(R.id.txtContenidoLegal);
        ImageView btnCerrarCruceta = view.findViewById(R.id.btnCerrarCruceta);
        Button btnCerrarAbajo = view.findViewById(R.id.btnCerrarAbajo);

        if (txtTituloLegal != null) txtTituloLegal.setText("NORMAS DEL CLUB");
        if (txtContenidoLegal != null) txtContenidoLegal.setText(getString(R.string.texto_normas));

        View.OnClickListener clickVolver = v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).cerrarFragmentoAnimado();
            } else if (isAdded()) {
                getParentFragmentManager().popBackStack();
            }
        };

        if (btnCerrarCruceta != null) btnCerrarCruceta.setOnClickListener(clickVolver);
        if (btnCerrarAbajo != null) btnCerrarAbajo.setOnClickListener(clickVolver);
    }
}