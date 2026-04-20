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
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

// Fragmento que agrupa la configuración del usuario, el acceso a textos legales y las acciones de cuenta.
public class SettingsFragment extends Fragment {

    // URL de la base de datos compartida por el panel de ajustes.
    private final String DB_URL = "https://the-ring-private-default-rtdb.europe-west1.firebasedatabase.app/";

    // El fragmento usa un layout propio donde se muestran los accesos a ajustes y textos legales.
    public SettingsFragment() {
        super(R.layout.activity_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // El botón superior vuelve al contenido anterior usando la animación del contenedor principal.
        View btnHomeBack = view.findViewById(R.id.btnHomeBack);
        if (btnHomeBack != null) {
            btnHomeBack.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).cerrarFragmentoAnimado();
                }
            });
        }

        // Si hay usuario autenticado, mostramos su información base en el encabezado.
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

        // El acceso rápido del encabezado lleva al perfil completo.
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

        // Acción para cambiar contraseña después de verificar identidad.
        View optionChangePassword = view.findViewById(R.id.optionChangePassword);
        if (optionChangePassword != null) {
            optionChangePassword.setOnClickListener(v -> mostrarDialogoVerificarYCambiarPass());
        }

        // Nuevas opciones internas de Idioma y Apariencia
        // Selector de idioma interno del panel de ajustes.
        View optionInternalLang = view.findViewById(R.id.optionInternalLang);
        if (optionInternalLang != null) {
            optionInternalLang.setOnClickListener(v -> mostrarDialogoIdiomasInterno());
        }

        // Selector de tema claro u oscuro.
        View optionInternalTheme = view.findViewById(R.id.optionInternalTheme);
        if (optionInternalTheme != null) {
            optionInternalTheme.setOnClickListener(v -> mostrarDialogoTemasInterno());
        }

        // Apertura del texto legal de tarifas.
        View optionRates = view.findViewById(R.id.optionRates);
        if (optionRates != null) {
            optionRates.setOnClickListener(v -> mostrarTextoLegal(R.string.texto_tarifas_horarios_titulo, R.string.texto_tarifas_horarios));
        }

        // Apertura de términos y condiciones.
        View optionTerms = view.findViewById(R.id.optionTerms);
        if (optionTerms != null) {
            optionTerms.setOnClickListener(v -> mostrarTextoLegal(R.string.texto_terminos_titulo, R.string.texto_terminos));
        }

        // Apertura del aviso legal.
        View optionPrivacy = view.findViewById(R.id.optionPrivacy);
        if (optionPrivacy != null) {
            optionPrivacy.setOnClickListener(v -> mostrarTextoLegal(R.string.ajuste_aviso_legal, R.string.texto_aviso_legal));
        }

        // Apertura de ayuda y soporte.
        View optionHelp = view.findViewById(R.id.optionHelp);
        if (optionHelp != null) {
            optionHelp.setOnClickListener(v -> mostrarTextoLegal(R.string.help_support_title, R.string.help_support_msg));
        }

        // Apertura de preguntas frecuentes.
        View optionFaq = view.findViewById(R.id.optionFaq);
        if (optionFaq != null) {
            optionFaq.setOnClickListener(v -> mostrarTextoLegal(R.string.texto_faq_titulo, R.string.texto_faq));
        }

        // Cierre de sesión con confirmación.
        View optionLogoutItem = view.findViewById(R.id.optionLogoutItem);
        if (optionLogoutItem != null) {
            optionLogoutItem.setOnClickListener(v -> mostrarDialogoCerrarSesion());
        }

        // Eliminación definitiva de la cuenta y de los datos asociados.
        View optionDeleteAccount = view.findViewById(R.id.optionDeleteAccount);
        if (optionDeleteAccount != null) {
            optionDeleteAccount.setOnClickListener(v -> mostrarDialogoEliminarCuenta());
        }

        // Acceso al manual de usuario desde el propio panel de ajustes.
        View optionUserManual = view.findViewById(R.id.optionUserManual);
        if (optionUserManual != null) {
            optionUserManual.setOnClickListener(v -> mostrarTextoLegal(R.string.texto_manual_usuario_titulo, R.string.texto_manual_usuario));
        }
    }

    // Muestra únicamente el bloque de idioma del desplegable reutilizable.
    private void mostrarDialogoIdiomasInterno() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.activity_settings_dropdown);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        dialog.findViewById(R.id.btnLangEs).setOnClickListener(v -> { cambiarIdioma("es"); dialog.dismiss(); });
        dialog.findViewById(R.id.btnLangEn).setOnClickListener(v -> { cambiarIdioma("en"); dialog.dismiss(); });
        
        // Ocultar partes innecesarias
        View layoutSectionTheme = dialog.findViewById(R.id.layoutSectionTheme);
        if (layoutSectionTheme != null) layoutSectionTheme.setVisibility(View.GONE);
        View dividerLang = dialog.findViewById(R.id.dividerLang);
        if (dividerLang != null) dividerLang.setVisibility(View.GONE);
        View dividerTheme = dialog.findViewById(R.id.dividerTheme);
        if (dividerTheme != null) dividerTheme.setVisibility(View.GONE);
        View btnMas = dialog.findViewById(R.id.btnMas);
        if (btnMas != null) btnMas.setVisibility(View.GONE);
        
        dialog.show();
    }

    // Muestra únicamente el bloque de tema del desplegable reutilizable.
    private void mostrarDialogoTemasInterno() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.activity_settings_dropdown);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        dialog.findViewById(R.id.btnThemeLight).setOnClickListener(v -> { cambiarTema(false); dialog.dismiss(); });
        dialog.findViewById(R.id.btnThemeDark).setOnClickListener(v -> { cambiarTema(true); dialog.dismiss(); });
        
        // Ocultar partes innecesarias
        View layoutSectionLang = dialog.findViewById(R.id.layoutSectionLang);
        if (layoutSectionLang != null) layoutSectionLang.setVisibility(View.GONE);
        View dividerLang = dialog.findViewById(R.id.dividerLang);
        if (dividerLang != null) dividerLang.setVisibility(View.GONE);
        View dividerTheme = dialog.findViewById(R.id.dividerTheme);
        if (dividerTheme != null) dividerTheme.setVisibility(View.GONE);
        View btnMas = dialog.findViewById(R.id.btnMas);
        if (btnMas != null) btnMas.setVisibility(View.GONE);
        
        dialog.show();
    }

    // Guarda el idioma seleccionado y recrea el fragmento anfitrión.
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

    // Persiste el tema elegido y aplica el modo visual correcto.
    private void cambiarTema(boolean dark) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("DarkMode", dark).apply();
        AppCompatDelegate.setDefaultNightMode(dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    // Diálogo para verificar identidad antes de permitir el cambio de contraseña.
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

        // Primer paso: validar DNI y correo contra la base de datos.
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

        // Segundo paso: actualizar la contraseña cuando la identidad ya está confirmada.
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

    // Muestra una confirmación explícita antes de cerrar sesión.
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

    // Muestra el diálogo de borrado definitivo de la cuenta.
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

        // Si el usuario no recuerda su contraseña, lo redirigimos al flujo de recuperación.
        if (btnOlvidado != null) {
            btnOlvidado.setOnClickListener(v -> {
                dialog.dismiss();
                mostrarDialogoVerificarYCambiarPass();
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        // La confirmación inicia la comprobación de credenciales y la posterior eliminación.
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String inputUser = safeText(etUser);
                String inputPass = safeText(etPass);

                if (inputUser.isEmpty() || inputPass.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.error_datos_incompletos), Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    // Permitimos borrar usando correo o DNI según lo que escriba el usuario.
                    if (inputUser.contains("@")) {
                        verificarYConfirmarEliminacion(user, inputUser, inputPass, dialog);
                    } else {
                        String dni = inputUser.toUpperCase();
                        FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").child(dni).get().addOnSuccessListener(snapshot -> {
                            String correoReal = snapshot.getValue(String.class);
                            if (correoReal != null) {
                                verificarYConfirmarEliminacion(user, correoReal, inputPass, dialog);
                            } else {
                                // Fallback: usa el email de la sesión actual para permitir borrado aunque MapeoDNI esté roto.
                                String emailSesion = user.getEmail();
                                if (emailSesion != null && !emailSesion.isEmpty()) {
                                    verificarYConfirmarEliminacion(user, emailSesion, inputPass, dialog);
                                } else {
                                    Toast.makeText(requireContext(), getString(R.string.error_usuario_no_encontrado), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            });
        }
        dialog.show();
    }

    // Extrae texto de forma segura desde un campo editable.
    private String safeText(TextInputEditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    // Reautentica al usuario antes de permitir la eliminación de la cuenta.
    private void verificarYConfirmarEliminacion(FirebaseUser user, String email, String pass, Dialog dialogAnterior) {
        user.reauthenticate(EmailAuthProvider.getCredential(email, pass)).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                dialogAnterior.dismiss();
                String canonicalEmail = user.getEmail() != null ? user.getEmail() : email;
                mostrarDialogoFinalConfirmacionEliminar(user, canonicalEmail);
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_credenciales), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Última confirmación antes de borrar perfil, mapeos y credenciales de Auth.
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
                FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(emailSafe).child("perfil").get().addOnSuccessListener(snapshot -> {
                    String dni = snapshot.child("dni").getValue(String.class);

                    var rootRef = FirebaseDatabase.getInstance(DB_URL).getReference();
                    var dniByEmailTask = rootRef.child("MapeoDNI").orderByValue().equalTo(email).get();

                    dniByEmailTask.addOnCompleteListener(queryTask -> {
                        var updates = new java.util.HashMap<String, Object>();
                        updates.put("Usuarios/" + emailSafe, null);
                        updates.put("TokensQR/" + emailSafe, null);

                        if (dni != null && !dni.isEmpty()) updates.put("MapeoDNI/" + dni, null);

                        if (dniByEmailTask.isSuccessful() && dniByEmailTask.getResult() != null) {
                            for (var child : dniByEmailTask.getResult().getChildren()) {
                                if (child.getKey() != null) updates.put("MapeoDNI/" + child.getKey(), null);
                            }
                        }

                        rootRef.updateChildren(updates).addOnCompleteListener(dbDeleteTask -> {
                                // Si falla la eliminación en base de datos no borramos Auth para evitar inconsistencias.
                                if (!dbDeleteTask.isSuccessful()) {
                                Toast.makeText(requireContext(), "Error al borrar datos del usuario", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Solo cuando la base de datos ya está limpia eliminamos la cuenta de Authentication.
                            user.delete().addOnCompleteListener(deleteTask -> {
                                if (deleteTask.isSuccessful()) {
                                    Toast.makeText(requireContext(), "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    Intent intent = new Intent(requireContext(), MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(requireContext(), "Datos eliminados, pero falló Auth. Inicia sesión e inténtalo de nuevo.", Toast.LENGTH_LONG).show();
                                }
                            });
                        });
                    });
                }).addOnFailureListener(e -> {
                    var rootRef = FirebaseDatabase.getInstance(DB_URL).getReference();
                    var updates = new java.util.HashMap<String, Object>();
                    updates.put("Usuarios/" + emailSafe, null);
                    updates.put("TokensQR/" + emailSafe, null);
                    rootRef.child("MapeoDNI").orderByValue().equalTo(email).get().addOnSuccessListener(dniSnap -> {
                        for (var child : dniSnap.getChildren()) {
                            if (child.getKey() != null) updates.put("MapeoDNI/" + child.getKey(), null);
                        }
                        rootRef.updateChildren(updates).addOnCompleteListener(anyDbTask -> user.delete().addOnCompleteListener(deleteTask -> {
                            if (deleteTask.isSuccessful()) {
                                Toast.makeText(requireContext(), "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                Intent intent = new Intent(requireContext(), MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        }));
                    });
                });
            });
        }
        dialog.show();
    }

    // Abre textos legales y manuales con el mismo formato visual que el resto de la app.
    private void mostrarTextoLegal(int resIdTitulo, int resIdContenido) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_fullscreen_legal, new FrameLayout(requireContext()), false);
        TextView txtTituloLegal = dialogView.findViewById(R.id.txtTituloLegal);
        TextView txtContenidoLegal = dialogView.findViewById(R.id.txtContenidoLegal);
        ImageView btnCerrarCruceta = dialogView.findViewById(R.id.btnCerrarCruceta);
        Button btnCerrarAbajo = dialogView.findViewById(R.id.btnCerrarAbajo);

        if (txtTituloLegal != null) TranslationHelper.translateTextView(txtTituloLegal, resIdTitulo);
        if (txtContenidoLegal != null) TranslationHelper.translateTextView(txtContenidoLegal, resIdContenido);
        if (btnCerrarCruceta != null) btnCerrarCruceta.setOnClickListener(v -> dialog.dismiss());
        if (btnCerrarAbajo != null) btnCerrarAbajo.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        dialog.setContentView(dialogView);
        dialog.show();
    }
}
