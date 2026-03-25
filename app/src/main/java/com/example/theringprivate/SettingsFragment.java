package com.example.theringprivate;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private final String DB_URL = "https://the-ring-private-default-rtdb.europe-west1.firebasedatabase.app/";

    public SettingsFragment() {
        super(R.layout.activity_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnHomeBack = view.findViewById(R.id.btnHomeBack);
        if (btnHomeBack != null) {
            btnHomeBack.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).cerrarFragmentoAnimado();
                }
            });
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String nombreCompleto = user.getDisplayName();
            if (nombreCompleto == null || nombreCompleto.isEmpty()) nombreCompleto = getString(R.string.user_label);
            TextView txtUserName = view.findViewById(R.id.txtUserName);
            if (txtUserName != null) txtUserName.setText(nombreCompleto);

            String rawEmail = user.getEmail() != null ? user.getEmail() : "";
            TextView txtUserEmail = view.findViewById(R.id.txtUserEmail);
            if (txtUserEmail != null) {
                if (rawEmail.isEmpty() || rawEmail.endsWith("@thering.local")) {
                    txtUserEmail.setText(getString(R.string.profile_not_linked));
                    txtUserEmail.setTextColor(Color.parseColor("#FF4C4C"));
                } else {
                    txtUserEmail.setText(rawEmail);
                    txtUserEmail.setTextColor(Color.parseColor("#CCCCCC"));
                }
            }
        }

        View btnEditProfileHeader = view.findViewById(R.id.btnEditProfileHeader);
        if (btnEditProfileHeader != null) {
            btnEditProfileHeader.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.fragment_container, new ProfileFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        View optionChangePassword = view.findViewById(R.id.optionChangePassword);
        if (optionChangePassword != null) {
            optionChangePassword.setOnClickListener(v -> mostrarDialogoVerificarYCambiarPass());
        }

        // Nuevas opciones internas de Idioma y Apariencia
        View optionInternalLang = view.findViewById(R.id.optionInternalLang);
        if (optionInternalLang != null) {
            optionInternalLang.setOnClickListener(v -> mostrarDialogoIdiomasInterno());
        }

        View optionInternalTheme = view.findViewById(R.id.optionInternalTheme);
        if (optionInternalTheme != null) {
            optionInternalTheme.setOnClickListener(v -> mostrarDialogoTemasInterno());
        }

        View optionRates = view.findViewById(R.id.optionRates);
        if (optionRates != null) {
            optionRates.setOnClickListener(v -> mostrarTextoLegal(getString(R.string.texto_tarifas_horarios_titulo), getString(R.string.texto_tarifas_horarios)));
        }

        View optionTerms = view.findViewById(R.id.optionTerms);
        if (optionTerms != null) {
            optionTerms.setOnClickListener(v -> mostrarTextoLegal(getString(R.string.texto_terminos_titulo), getString(R.string.texto_terminos)));
        }

        View optionPrivacy = view.findViewById(R.id.optionPrivacy);
        if (optionPrivacy != null) {
            optionPrivacy.setOnClickListener(v -> mostrarTextoLegal(getString(R.string.ajuste_aviso_legal), getString(R.string.texto_aviso_legal)));
        }

        View optionHelp = view.findViewById(R.id.optionHelp);
        if (optionHelp != null) {
            optionHelp.setOnClickListener(v -> mostrarTextoLegal(getString(R.string.help_support_title), getString(R.string.help_support_msg)));
        }

        View optionFaq = view.findViewById(R.id.optionFaq);
        if (optionFaq != null) {
            optionFaq.setOnClickListener(v -> mostrarTextoLegal(getString(R.string.texto_faq_titulo), getString(R.string.texto_faq)));
        }

        View optionLogoutItem = view.findViewById(R.id.optionLogoutItem);
        if (optionLogoutItem != null) {
            optionLogoutItem.setOnClickListener(v -> mostrarDialogoCerrarSesion());
        }

        View optionDeleteAccount = view.findViewById(R.id.optionDeleteAccount);
        if (optionDeleteAccount != null) {
            optionDeleteAccount.setOnClickListener(v -> mostrarDialogoEliminarCuenta());
        }
    }

    private void mostrarDialogoIdiomasInterno() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.activity_settings_dropdown);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        dialog.findViewById(R.id.btnLangEs).setOnClickListener(v -> { cambiarIdioma("es"); dialog.dismiss(); });
        dialog.findViewById(R.id.btnLangEn).setOnClickListener(v -> { cambiarIdioma("en"); dialog.dismiss(); });
        
        // Ocultar partes innecesarias
        View themeSection = dialog.findViewById(R.id.btnThemeLight).getParent().getParent() instanceof LinearLayout ? (View)dialog.findViewById(R.id.btnThemeLight).getParent().getParent() : null;
        if (themeSection != null) themeSection.setVisibility(View.GONE);
        View btnMas = dialog.findViewById(R.id.btnMas);
        if (btnMas != null) btnMas.setVisibility(View.GONE);
        
        dialog.show();
    }

    private void mostrarDialogoTemasInterno() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.activity_settings_dropdown);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        dialog.findViewById(R.id.btnThemeLight).setOnClickListener(v -> { cambiarTema(false); dialog.dismiss(); });
        dialog.findViewById(R.id.btnThemeDark).setOnClickListener(v -> { cambiarTema(true); dialog.dismiss(); });
        
        // Ocultar partes innecesarias
        View langSection = dialog.findViewById(R.id.tvLangTitle).getParent() instanceof LinearLayout ? (View)dialog.findViewById(R.id.tvLangTitle).getParent() : null;
        if (langSection != null) langSection.setVisibility(View.GONE);
        View btnMas = dialog.findViewById(R.id.btnMas);
        if (btnMas != null) btnMas.setVisibility(View.GONE);
        
        dialog.show();
    }

    private void cambiarIdioma(String lang) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefs.edit().putString("My_Lang", lang).apply();

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(getResources().getConfiguration());
        config.setLocale(locale);
        requireContext().createConfigurationContext(config);

        requireActivity().recreate();
    }

    private void cambiarTema(boolean dark) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("DarkMode", dark).apply();
        AppCompatDelegate.setDefaultNightMode(dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
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

    private void mostrarDialogoCerrarSesion() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_logout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        MaterialButton btnCancelLogout = dialog.findViewById(R.id.btnCancelLogout);
        MaterialButton btnConfirmLogout = dialog.findViewById(R.id.btnConfirmLogout);

        if (btnCancelLogout != null) btnCancelLogout.setOnClickListener(v -> dialog.dismiss());
        if (btnConfirmLogout != null) {
            btnConfirmLogout.setOnClickListener(v -> {
                dialog.dismiss();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }
        dialog.show();
    }

    private void mostrarDialogoEliminarCuenta() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_delete_account);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextInputEditText etUser = dialog.findViewById(R.id.etDeleteUser);
        TextInputEditText etPass = dialog.findViewById(R.id.etDeletePassword);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelDelete);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirmDelete);
        View btnOlvidado = dialog.findViewById(R.id.btnOlvidadoPassDelete);

        if (btnOlvidado != null) {
            btnOlvidado.setOnClickListener(v -> {
                dialog.dismiss();
                mostrarDialogoVerificarYCambiarPass();
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String inputUser = etUser != null ? etUser.getText().toString().trim() : "";
                String inputPass = etPass != null ? etPass.getText().toString().trim() : "";

                if (inputUser.isEmpty() || inputPass.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.error_datos_incompletos), Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    if (inputUser.contains("@")) {
                        verificarYConfirmarEliminacion(user, inputUser, inputPass, dialog);
                    } else {
                        String dni = inputUser.toUpperCase();
                        FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").child(dni).get().addOnSuccessListener(snapshot -> {
                            String correoReal = snapshot.getValue(String.class);
                            if (correoReal != null) {
                                verificarYConfirmarEliminacion(user, correoReal, inputPass, dialog);
                            } else {
                                Toast.makeText(requireContext(), getString(R.string.error_usuario_no_encontrado), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }
        dialog.show();
    }

    private void verificarYConfirmarEliminacion(FirebaseUser user, String email, String pass, Dialog dialogAnterior) {
        user.reauthenticate(EmailAuthProvider.getCredential(email, pass)).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                dialogAnterior.dismiss();
                mostrarDialogoFinalConfirmacionEliminar(user, email);
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_credenciales), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoFinalConfirmacionEliminar(FirebaseUser user, String email) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_final_delete_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        MaterialButton btnFinalCancelarDelete = dialog.findViewById(R.id.btnFinalCancelarDelete);
        MaterialButton btnFinalAceptoDelete = dialog.findViewById(R.id.btnFinalAceptoDelete);

        if (btnFinalCancelarDelete != null) btnFinalCancelarDelete.setOnClickListener(v -> dialog.dismiss());
        if (btnFinalAceptoDelete != null) {
            btnFinalAceptoDelete.setOnClickListener(v -> {
                String emailSafe = email.replace(".", "_");
                FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(emailSafe).removeValue().addOnCompleteListener(task -> {
                    user.delete().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Toast.makeText(requireContext(), "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            Intent intent = new Intent(requireContext(), MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    });
                });
            });
        }
        dialog.show();
    }

    private void mostrarTextoLegal(String titulo, String contenido) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_fullscreen_legal, null);
        TextView txtTituloLegal = dialogView.findViewById(R.id.txtTituloLegal);
        TextView txtContenidoLegal = dialogView.findViewById(R.id.txtContenidoLegal);
        ImageView btnCerrarCruceta = dialogView.findViewById(R.id.btnCerrarCruceta);
        Button btnCerrarAbajo = dialogView.findViewById(R.id.btnCerrarAbajo);

        if (txtTituloLegal != null) txtTituloLegal.setText(titulo);
        if (txtContenidoLegal != null) txtContenidoLegal.setText(contenido);
        if (btnCerrarCruceta != null) btnCerrarCruceta.setOnClickListener(v -> dialog.dismiss());
        if (btnCerrarAbajo != null) btnCerrarAbajo.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        dialog.setContentView(dialogView);
        dialog.show();
    }
}
