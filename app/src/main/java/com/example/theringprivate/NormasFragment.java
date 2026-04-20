package com.example.theringprivate;

// Fragmento dedicado a mostrar el texto de normas en una ventana completa reutilizando el mismo estilo legal.
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

// Pantalla que no muestra contenido propio y abre directamente el diálogo de normas.
public class NormasFragment extends Fragment {

    // Construimos el fragmento con un layout vacío porque la lógica visible vive dentro del diálogo.
    public NormasFragment() {
        super(R.layout.fragment_vacio_animacion);
    }

    // Cuando el fragmento ya está dibujado, abrimos el contenido de normas.
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Abrimos la ventana de normas automáticamente para no dejar una pantalla intermedia vacía.
        mostrarNormasEstiloLegal();
    }

    // Construye el diálogo a pantalla completa con la misma plantilla legal que el resto de textos largos.
    private void mostrarNormasEstiloLegal() {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_fullscreen_legal, new android.widget.FrameLayout(requireContext()), false);

        // Recuperamos las piezas visuales que componen el layout legal reutilizable.
        TextView txtTituloLegal = dialogView.findViewById(R.id.txtTituloLegal);
        TextView txtContenidoLegal = dialogView.findViewById(R.id.txtContenidoLegal);
        ImageView btnCerrarCruceta = dialogView.findViewById(R.id.btnCerrarCruceta);
        Button btnCerrarAbajo = dialogView.findViewById(R.id.btnCerrarAbajo);

        // Traducimos o mostramos el título usando el helper común para respetar el idioma activo.
        if (txtTituloLegal != null) {
            TranslationHelper.translateTextView(txtTituloLegal, R.string.menu_normas);
        }
        // Traducimos o mostramos el contenido completo de normas.
        if (txtContenidoLegal != null) {
            TranslationHelper.translateTextView(txtContenidoLegal, R.string.texto_normas);
        }

        // Compartimos la misma acción de cierre para la cruz y el botón inferior.
        View.OnClickListener clickCerrar = v -> {
            dialog.dismiss();
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).cerrarFragmentoAnimado();
            } else if (isAdded()) {
                getParentFragmentManager().popBackStack();
            }
        };

        // Vinculamos el cierre visual superior.
        if (btnCerrarCruceta != null) btnCerrarCruceta.setOnClickListener(clickCerrar);
        // Vinculamos el cierre visual inferior.
        if (btnCerrarAbajo != null) btnCerrarAbajo.setOnClickListener(clickCerrar);

        // Activamos una animación estándar para que la apertura coincida con el resto de diálogos.
        if (dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        dialog.setContentView(dialogView);
        dialog.show();
    }
}