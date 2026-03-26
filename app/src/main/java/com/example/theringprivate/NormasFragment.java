package com.example.theringprivate;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class NormasFragment extends Fragment {

    public NormasFragment() {
        // No cargamos el layout directamente como fragmento para poder usar el Dialog consistente
        super(R.layout.fragment_vacio_animacion); 
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Al crear el fragmento, mostramos el diálogo que tiene la estructura perfecta y consistente
        mostrarNormasEstiloLegal();
    }

    private void mostrarNormasEstiloLegal() {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_fullscreen_legal, null);
        
        TextView txtTituloLegal = dialogView.findViewById(R.id.txtTituloLegal);
        TextView txtContenidoLegal = dialogView.findViewById(R.id.txtContenidoLegal);
        ImageView btnCerrarCruceta = dialogView.findViewById(R.id.btnCerrarCruceta);
        Button btnCerrarAbajo = dialogView.findViewById(R.id.btnCerrarAbajo);

        if (txtTituloLegal != null) txtTituloLegal.setText("NORMAS DEL CLUB");
        if (txtContenidoLegal != null) txtContenidoLegal.setText(getString(R.string.texto_normas));

        View.OnClickListener clickCerrar = v -> {
            dialog.dismiss();
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).cerrarFragmentoAnimado();
            } else if (isAdded()) {
                getParentFragmentManager().popBackStack();
            }
        };

        if (btnCerrarCruceta != null) btnCerrarCruceta.setOnClickListener(clickCerrar);
        if (btnCerrarAbajo != null) btnCerrarAbajo.setOnClickListener(clickCerrar);

        if (dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        dialog.setContentView(dialogView);
        dialog.show();
    }
}