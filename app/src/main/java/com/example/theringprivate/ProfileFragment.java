package com.example.theringprivate;

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

import java.util.Random;

public class ProfileFragment extends Fragment {

    private final String DB_URL = "https://the-ring-private-default-rtdb.europe-west1.firebasedatabase.app/";
    private DatabaseReference database;
    private String currentUserEmailSafe = "";

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // BOTÓN ATRÁS INTELIGENTE
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
        TextView tvApodo = view.findViewById(R.id.tvApodo);

        ImageView btnEditApodo = view.findViewById(R.id.btnEditApodo);
        MaterialButton btnChangePass = view.findViewById(R.id.btnChangePassProfile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String rawEmail = user.getEmail() != null ? user.getEmail() : "";
            String emailText = (rawEmail.isEmpty() || rawEmail.endsWith("@thering.local")) ? getString(R.string.profile_not_linked) : rawEmail;
            if (tvEmail != null) tvEmail.setText(emailText);
            if (tvNombreReal != null) tvNombreReal.setText(user.getDisplayName() != null ? user.getDisplayName() : getString(R.string.loading));
            currentUserEmailSafe = rawEmail.replace(".", "_");
        }

        database = FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(currentUserEmailSafe).child("perfil");

        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                String nombre = snapshot.child("nombreReal").getValue(String.class);
                if (nombre == null && user != null) nombre = user.getDisplayName();
                if (nombre == null) nombre = getString(R.string.user_label);

                String dniBD = snapshot.child("dni").getValue(String.class);
                if (dniBD == null) dniBD = getString(R.string.socio_label);

                String apodo = snapshot.child("apodo").getValue(String.class);

                if (tvNombreReal != null) tvNombreReal.setText(nombre);
                if (tvDni != null) tvDni.setText(dniBD);

                if (apodo == null || apodo.isEmpty()) {
                    String apodoGenerado = generarApodoAutomatico(nombre);
                    database.child("apodo").setValue(apodoGenerado);
                    if (tvApodo != null) tvApodo.setText(apodoGenerado);
                } else {
                    if (tvApodo != null) tvApodo.setText(apodo);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        if (btnEditApodo != null) {
            btnEditApodo.setOnClickListener(v -> {
                String apodoActual = tvApodo != null ? tvApodo.getText().toString() : "";
                mostrarDialogoEditarApodo(apodoActual);
            });
        }
        
        if (btnChangePass != null) {
            btnChangePass.setOnClickListener(v -> mostrarDialogoVerificarYCambiarPass());
        }
    }

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

        if (btnVerify != null) {
            btnVerify.setOnClickListener(v -> {
                String dniInput = etDni != null ? etDni.getText().toString().trim().toUpperCase() : "";
                String emailInput = etEmail != null ? etEmail.getText().toString().trim() : "";

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

        if (btnChange != null) {
            btnChange.setOnClickListener(v -> {
                String newPass = etNewPass != null ? etNewPass.getText().toString().trim() : "";
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

    private void mostrarDialogoEditarApodo(String apodoActual) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_edit_apodo);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextInputEditText etInput = dialog.findViewById(R.id.etInput);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSave);

        if (etInput != null) etInput.setText(apodoActual);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String nuevoApodo = etInput != null ? etInput.getText().toString().trim().toLowerCase() : "";
                if (nuevoApodo.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.apodo_empty_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (nuevoApodo.equals(apodoActual.toLowerCase())) {
                    dialog.dismiss();
                    return;
                }

                DatabaseReference apodosRef = FirebaseDatabase.getInstance(DB_URL).getReference("Apodos");
                apodosRef.child(nuevoApodo).get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Toast.makeText(requireContext(), getString(R.string.apodo_in_use), Toast.LENGTH_SHORT).show();
                    } else {
                        apodosRef.child(apodoActual.toLowerCase()).removeValue();
                        apodosRef.child(nuevoApodo).setValue(currentUserEmailSafe);
                        database.child("apodo").setValue(nuevoApodo);
                        dialog.dismiss();
                        Toast.makeText(requireContext(), getString(R.string.apodo_updated), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
        dialog.show();
    }

    private String generarApodoAutomatico(String nombreReal) {
        Random random = new Random();
        int num = random.nextInt(90) + 10;
        String prefix = getString(R.string.socio_prefix);
        if (getString(R.string.user_label).equals(nombreReal) || nombreReal == null || nombreReal.trim().isEmpty()) return prefix + num;
        
        String[] parts = nombreReal.split(" ");
        StringBuilder iniciales = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                iniciales.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return iniciales.toString() + num;
    }
}