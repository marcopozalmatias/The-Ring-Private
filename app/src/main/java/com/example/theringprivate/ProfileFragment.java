package com.example.theringprivate;

// Fragmento donde el usuario consulta sus datos de perfil y puede cambiar la contraseña.
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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

    // URL de la base de datos usada para leer el perfil.
    private final String DB_URL = "https://the-ring-private-default-rtdb.europe-west1.firebasedatabase.app/";
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
        DatabaseReference database = FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(currentUserEmailSafe).child("perfil");

        // Escuchamos cambios en el perfil para refrescar nombre y DNI en tiempo real.
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                // Obtenemos el nombre visible almacenado en la base de datos.
                String nombre = snapshot.child("nombreReal").getValue(String.class);
                if (nombre == null && user != null) nombre = user.getDisplayName();
                if (nombre == null) nombre = getString(R.string.user_label);

                // También mostramos el DNI que figura en el perfil.
                String dniBD = snapshot.child("dni").getValue(String.class);
                if (dniBD == null) dniBD = getString(R.string.socio_label);

                if (tvNombreReal != null) tvNombreReal.setText(nombre);
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

    // Flujo de verificación de identidad y cambio de contraseña.
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
        TextInputEditText etNewPass = dialog.findViewById(R.id.etNewPassword);
        MaterialButton btnChange = dialog.findViewById(R.id.btnChangePasswordNow);

        // Primer paso: verificar DNI y correo.
        if (btnVerify != null) {
            btnVerify.setOnClickListener(v -> {
                String dniInput = safeText(etDni).toUpperCase();
                String emailInput = safeText(etEmail);

                if (dniInput.isEmpty() || emailInput.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.error_datos_incompletos), Toast.LENGTH_SHORT).show();
                    return;
                }

                String emailSafe = emailInput.replace(".", "_");
                FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(emailSafe).child("perfil").get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                String dniDB = snapshot.child("dni").getValue(String.class);
                                if (dniInput.equals(dniDB)) {
                                    if (layoutStep1 != null) layoutStep1.setVisibility(View.GONE);
                                    if (layoutStep2 != null) layoutStep2.setVisibility(View.VISIBLE);
                                } else {
                                    Toast.makeText(requireContext(), getString(R.string.error_datos_no_coinciden), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(requireContext(), getString(R.string.error_usuario_no_encontrado), Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }

        // Segundo paso: si el usuario es válido, permitimos actualizar la contraseña.
        if (btnChange != null) {
            btnChange.setOnClickListener(v -> {
                String newPass = safeText(etNewPass);
                if (newPass.length() < 6) {
                    Toast.makeText(requireContext(), getString(R.string.min_6_chars), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    user.updatePassword(newPass).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), getString(R.string.pass_changed_success), Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.reauth_needed), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
        dialog.show();
    }

    // Devuelve texto seguro de un campo editable o una cadena vacía si el campo no contiene nada.
    private String safeText(TextInputEditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
