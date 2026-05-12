package com.example.theringprivate;

// Fragmento donde el usuario consulta sus datos de perfil y puede cambiar la contraseña.
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private String currentUserEmailSafe = "";

    // El layout del perfil ya contiene toda la información visible al usuario.
    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Botón atrás con comportamiento adaptado al historial de fragmentos.
        ImageView btnBackProfile = view.findViewById(R.id.btnBackProfile);
        if (btnBackProfile != null) {
            btnBackProfile.setOnClickListener(v -> {
                if (getActivity() != null) {
                    if (getActivity().getSupportFragmentManager().getBackStackEntryCount() > 1) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    } else if (getActivity() instanceof HomeActivity) {
                        ((HomeActivity) getActivity()).cerrarFragmentoAnimado();
                    }
                }
            });
        }

        TextView tvDni = view.findViewById(R.id.tvDni);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvNombreReal = view.findViewById(R.id.tvNombreReal);

        // Acceso al flujo de cambio de contraseña desde el propio perfil.
        MaterialButton btnChangePass = view.findViewById(R.id.btnChangePassProfile);

        // Leemos el usuario actual para poder mostrar sus datos reales.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String rawEmail = user.getEmail() != null ? user.getEmail() : "";
            String emailText = (rawEmail.isEmpty() || rawEmail.endsWith("@thering.local")) ? getString(R.string.profile_not_linked) : rawEmail;
            if (tvEmail != null) tvEmail.setText(emailText);
            if (tvNombreReal != null) tvNombreReal.setText(user.getDisplayName() != null ? user.getDisplayName() : getString(R.string.loading));
            currentUserEmailSafe = rawEmail.replace(".", "_");
        }

        // Referencia directa al nodo de perfil del usuario autenticado.
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("usuarios").child(user != null ? user.getUid() : currentUserEmailSafe);

        // Escuchamos cambios en el perfil para refrescar nombre y DNI en tiempo real.
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                // Obtenemos el nombre y apellidos almacenados en la base de datos (nuevo formato).
                String name = snapshot.child("name").getValue(String.class);
                String surname = snapshot.child("surname").getValue(String.class);
                String nombreCompleto = "";
                if (name != null) {
                    nombreCompleto = name + (surname != null ? " " + surname : "");
                }

                if (nombreCompleto.isEmpty() && user != null) nombreCompleto = user.getDisplayName();
                if (nombreCompleto == null || nombreCompleto.isEmpty()) nombreCompleto = getString(R.string.user_label);

                // También mostramos el DNI o el email si es necesario.
                String dniBD = snapshot.child("dni").getValue(String.class);
                if (dniBD == null) dniBD = getString(R.string.socio_label);

                if (tvNombreReal != null) tvNombreReal.setText(nombreCompleto);
                if (tvDni != null) tvDni.setText(dniBD);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        // Botón para cambiar la contraseña sin salir de la pantalla de perfil.
        if (btnChangePass != null) {
            btnChangePass.setOnClickListener(v -> mostrarDialogoVerificarYCambiarPass());
        }
    }

    // Flujo de recuperación de contraseña por correo, igual que en la pantalla de login.
    private void mostrarDialogoVerificarYCambiarPass() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_forgot_password);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View layoutStep1 = dialog.findViewById(R.id.layoutResetStep1);
        View layoutStep2 = dialog.findViewById(R.id.layoutResetStep2);
        TextInputEditText etDni = dialog.findViewById(R.id.etRecoverDni);
        TextInputEditText etEmail = dialog.findViewById(R.id.etRecoverEmail);
        MaterialButton btnVerify = dialog.findViewById(R.id.btnVerifyIdentity);
        MaterialButton btnChange = dialog.findViewById(R.id.btnChangePasswordNow);

        if (layoutStep2 != null) layoutStep2.setVisibility(View.GONE);
        if (etDni != null && etDni.getParent() instanceof View) {
            ((View) etDni.getParent()).setVisibility(View.GONE);
        }
        if (btnVerify != null) btnVerify.setText(getString(R.string.btn_send_reset_email));

        // Solo pedimos correo válido y delegamos el reset en Firebase Auth.
        if (btnVerify != null) {
            btnVerify.setOnClickListener(v -> {
                String emailInput = safeText(etEmail);

                if (emailInput.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.error_email_vacio), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                    Toast.makeText(requireContext(), getString(R.string.error_email_invalido), Toast.LENGTH_SHORT).show();
                    return;
                }

                btnVerify.setEnabled(false);
                FirebaseAuth.getInstance().sendPasswordResetEmail(emailInput).addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    btnVerify.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), getString(R.string.msg_reset_email_enviado), Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.error_procesar), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        if (btnChange != null) {
            btnChange.setVisibility(View.GONE);
        }

        if (layoutStep1 != null) layoutStep1.setVisibility(View.VISIBLE);
        dialog.show();
    }

    // Devuelve texto seguro de un campo editable o una cadena vacía si el campo no contiene nada.
    private String safeText(TextInputEditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
