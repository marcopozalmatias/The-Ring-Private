package com.example.theringprivate;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import android.util.Patterns;
import android.util.Log;

import java.util.Locale;

// Pantalla de entrada de la aplicación donde se inicia sesión, se cambia el idioma y se abre el manual.
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    // Servicio de autenticación de Firebase para iniciar sesión con correo y contraseña.
    private FirebaseAuth auth;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Leemos el idioma guardado antes de crear la vista para que toda la pantalla nazca localizada.
        SharedPreferences prefs = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("My_Lang", "es");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(locale);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    protected void onStart() {
        try {
            // Si ya existe una sesión activa, saltamos directamente a la pantalla principal.
            super.onStart();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en onStart", e);
            Toast.makeText(this, "Error al verificar sesión", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            // Aplicamos el tema antes de inflar la vista para evitar un cambio visual brusco.
            SharedPreferences prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
            boolean isDarkMode = prefs.getBoolean("DarkMode", false);
            AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            // Inicializamos Firebase Auth y saneamos mapeos de DNI que puedan haber quedado huérfanos.
            auth = FirebaseAuth.getInstance();
            limpiarMapeosDniHuerfanos();

            View rootLayout = findViewById(R.id.layoutLogin);
            View cardLogin = findViewById(R.id.cardLogin);
            View ivLogo = findViewById(R.id.ivLogo);

            // Ajuste dinámico para que el teclado no tape el formulario de acceso.
            if (rootLayout != null) {
                rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    Rect r = new Rect();
                    rootLayout.getWindowVisibleDisplayFrame(r);
                    int screenHeight = rootLayout.getRootView().getHeight();
                    int keypadHeight = screenHeight - r.bottom;
                    if (keypadHeight > screenHeight * 0.15) {
                        float offset = -keypadHeight / 2f;
                        if (cardLogin != null) cardLogin.setTranslationY(offset);
                        if (ivLogo != null) ivLogo.setTranslationY(offset);
                    } else {
                        if (cardLogin != null) cardLogin.setTranslationY(0);
                        if (ivLogo != null) ivLogo.setTranslationY(0);
                    }
                });
            }

            TextInputLayout tilDni = findViewById(R.id.tilDni);
            TextInputLayout tilPassword = findViewById(R.id.tilPassword);
            TextInputEditText etUser = findViewById(R.id.etDni);
            TextInputEditText etPassword = findViewById(R.id.etPassword);
            MaterialButton btnLogin = findViewById(R.id.btnLogin);
            TextView tvRegister = findViewById(R.id.tvRegister);
            TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
            LinearLayout btnChangeLang = findViewById(R.id.btnChangeLangLogin);
            LinearLayout btnManualLogin = findViewById(R.id.btnManualLogin);
            TextView tvFlag = findViewById(R.id.tvCurrentFlag);

            // Lógica para mostrar el idioma CONTRARIO
            String currentLang = getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("My_Lang", "es");
            if (tvFlag != null) {
                tvFlag.setText(currentLang.equals("es") ? "🇬🇧 EN" : "🇪🇸 ES");
            }

            // El botón de login desencadena la validación y el acceso a Firebase.
            if (btnLogin != null) {
                btnLogin.setOnClickListener(v -> {
                    // Limpiamos cualquier error previo antes de volver a validar.
                    if (tilDni != null) tilDni.setError(null);
                    if (tilPassword != null) tilPassword.setError(null);

                    // Leemos y normalizamos la información introducida por el usuario.
                    String email = safeText(etUser);
                    String password = safeText(etPassword);

                    if (email.isEmpty()) {
                        if (tilDni != null) tilDni.setError(getString(R.string.error_dni_vacio));
                        return;
                    }
                    if (password.isEmpty()) {
                        if (tilPassword != null) tilPassword.setError(getString(R.string.error_pass_vacio));
                        return;
                    }

                    // Inicio de sesión directo con correo y contraseña.
                    iniciarSesionFirebase(email, password, tilDni, tilPassword);
                });
            }

            // Acceso a la pantalla de registro.
            if (tvRegister != null) tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
            // Recuperación de contraseña desde la misma pantalla de login.
            if (tvForgotPassword != null) tvForgotPassword.setOnClickListener(v -> mostrarDialogoRecuperacionInterna());
            // Selector de idioma en la pantalla inicial.
            if (btnChangeLang != null) btnChangeLang.setOnClickListener(v -> mostrarDialogoIdiomas());
            // Botón de manual para que un usuario nuevo aprenda a usar la app sin salir de login.
            if (btnManualLogin != null) {
                btnManualLogin.setOnClickListener(v -> mostrarTextoLegal(R.string.texto_manual_usuario_titulo, R.string.texto_manual_usuario));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en onCreate", e);
            Toast.makeText(this, "Error de inicialización: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // Muestra el selector de idioma desde la pantalla de login.
    private void mostrarDialogoIdiomas() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_settings_dropdown);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        // Conectamos los dos idiomas soportados por la interfaz.
        dialog.findViewById(R.id.btnLangEs).setOnClickListener(v -> { cambiarIdioma("es"); dialog.dismiss(); });
        dialog.findViewById(R.id.btnLangEn).setOnClickListener(v -> { cambiarIdioma("en"); dialog.dismiss(); });
        
        View themeSection = dialog.findViewById(R.id.tvThemeTitle).getParent().getParent() instanceof LinearLayout ? (View)dialog.findViewById(R.id.tvThemeTitle).getParent().getParent() : null;
        if (themeSection != null) themeSection.setVisibility(View.GONE);
        View btnMas = dialog.findViewById(R.id.btnMas);
        if (btnMas != null) btnMas.setVisibility(View.GONE);
        
        dialog.show();
    }

    // Guarda el idioma elegido y reconstruye la actividad para refrescar textos.
    private void cambiarIdioma(String lang) {
        getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().putString("My_Lang", lang).apply();
        recreate();
    }

    // Limpia mapeos de DNI que apunten a correos ya inexistentes en Usuarios.
    private void limpiarMapeosDniHuerfanos() {
        try {
            limpiarMapeosDniEnNodo("MapeoDNI");
            limpiarMapeosDniEnNodo("MapeoDocumentos");
        } catch (Exception e) {
            Log.e(TAG, "Error al limpiar mapeos DNI", e);
        }
    }

    private void limpiarMapeosDniEnNodo(String nodo) {
        try {
            FirebaseDatabase.getInstance().getReference(nodo).get().addOnSuccessListener(snapshot -> {
                try {
                    for (var data : snapshot.getChildren()) {
                        String dni = data.getKey();
                        String email = data.getValue(String.class);
                        if (dni == null) continue;
                        if (email == null || email.isEmpty()) {
                            FirebaseDatabase.getInstance().getReference(nodo).child(dni).removeValue();
                            continue;
                        }

                        FirebaseDatabase.getInstance().getReference("usuarios").orderByChild("email").equalTo(email).get().addOnSuccessListener(perfil -> {
                            if (!perfil.exists()) {
                                FirebaseDatabase.getInstance().getReference(nodo).child(dni).removeValue();
                            }
                        }).addOnFailureListener(e -> FirebaseDatabase.getInstance().getReference(nodo).child(dni).removeValue());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error procesando nodo " + nodo, e);
                }
            }).addOnFailureListener(e -> Log.e(TAG, "Error al obtener nodo " + nodo, e));
        } catch (Exception e) {
            Log.e(TAG, "Error en limpiarMapeosDniEnNodo", e);
        }
    }

    // Extrae texto seguro de un campo evitando nulos.
    private String safeText(TextInputEditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    // Intenta iniciar sesión y, si falla, marca los campos con el error correspondiente.
    private void iniciarSesionFirebase(String email, String password, TextInputLayout tilDni, TextInputLayout tilPassword) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            } else {
                if (tilDni != null) tilDni.setError(getString(R.string.error_credenciales));
                if (tilPassword != null) tilPassword.setError(" ");
            }
        });
    }

    // Diálogo de recuperación de contraseña integrado en la pantalla de login.
    private void mostrarDialogoRecuperacionInterna() {
        Dialog dialog = new Dialog(this);
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

        // En la pantalla de acceso no pedimos reautenticación: solo enviamos el enlace de recuperación al correo.
        if (layoutStep2 != null) layoutStep2.setVisibility(View.GONE);
        if (etDni != null && etDni.getParent() instanceof View) {
            ((View) etDni.getParent()).setVisibility(View.GONE);
        }
        if (btnVerify != null) btnVerify.setText(getString(R.string.btn_send_reset_email));

        // Primer paso: validar el correo y enviar el enlace de recuperación directamente desde Firebase Auth.
        if (btnVerify != null) {
            btnVerify.setOnClickListener(v -> {
                String emailInput = safeText(etEmail);
                if (emailInput.isEmpty()) {
                    Toast.makeText(this, getString(R.string.error_email_vacio), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                    Toast.makeText(this, getString(R.string.error_email_invalido), Toast.LENGTH_SHORT).show();
                    return;
                }

                btnVerify.setEnabled(false);
                auth.sendPasswordResetEmail(emailInput).addOnCompleteListener(task -> {
                    btnVerify.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, getString(R.string.msg_reset_email_enviado), Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(this, getString(R.string.error_procesar), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        // El segundo paso se mantiene en el layout compartido para los flujos autenticados, pero no se usa aquí.
        if (btnChange != null) {
            btnChange.setVisibility(View.GONE);
        }
        dialog.show();
    }

    // Abre textos legales y el manual en pantalla completa con traducción automática.
    private void mostrarTextoLegal(int resIdTitulo, int resIdContenido) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_fullscreen_legal, new FrameLayout(this), false);
        TextView txtTituloLegal = view.findViewById(R.id.txtTituloLegal);
        TextView txtContenidoLegal = view.findViewById(R.id.txtContenidoLegal);
        ImageView btnCerrarCruceta = view.findViewById(R.id.btnCerrarCruceta);
        Button btnCerrarAbajo = view.findViewById(R.id.btnCerrarAbajo);

        if (txtTituloLegal != null) {
            String titulo = getString(resIdTitulo);
            txtTituloLegal.setText(Html.fromHtml(titulo, Html.FROM_HTML_MODE_COMPACT));
        }
        if (txtContenidoLegal != null) {
            String contenido = getString(resIdContenido);
            txtContenidoLegal.setText(Html.fromHtml(contenido, Html.FROM_HTML_MODE_COMPACT));
        }
        if (btnCerrarCruceta != null) btnCerrarCruceta.setOnClickListener(v -> dialog.dismiss());
        if (btnCerrarAbajo != null) btnCerrarAbajo.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }
}
