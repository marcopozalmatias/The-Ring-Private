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

import java.util.Locale;

// Pantalla de entrada de la aplicación donde se inicia sesión, se cambia el idioma y se abre el manual.
public class MainActivity extends AppCompatActivity {

    // Servicio de autenticación de Firebase para iniciar sesión con correo y contraseña.
    private FirebaseAuth auth;
    // URL base de la Realtime Database usada por la app.
    private final String DB_URL = "https://the-ring-private-default-rtdb.europe-west1.firebasedatabase.app/";

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
        // Si ya existe una sesión activa, saltamos directamente a la pantalla principal.
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                String input = safeText(etUser);
                String password = safeText(etPassword);

                if (input.isEmpty()) {
                    if (tilDni != null) tilDni.setError(getString(R.string.error_dni_vacio));
                    return;
                }
                if (password.isEmpty()) {
                    if (tilPassword != null) tilPassword.setError(getString(R.string.error_pass_vacio));
                    return;
                }
                
                // Si el usuario escribe un correo, usamos ese dato directamente.
                if (input.contains("@")) {
                    iniciarSesionFirebase(input, password, tilDni, tilPassword);
                } else {
                    // Si escribe DNI, primero lo convertimos al correo real guardado en la base de datos.
                    String dni = input.toUpperCase();
                    FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").child(dni).get().addOnSuccessListener(snapshot -> {
                        String correoReal = snapshot.getValue(String.class);
                        if (correoReal == null) correoReal = dni + "@thering.local";
                        iniciarSesionFirebase(correoReal, password, tilDni, tilPassword);
                    }).addOnFailureListener(e -> iniciarSesionFirebase(dni + "@thering.local", password, tilDni, tilPassword));
                }
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
        FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").get().addOnSuccessListener(snapshot -> {
            for (var data : snapshot.getChildren()) {
                String dni = data.getKey();
                String email = data.getValue(String.class);
                if (dni == null) continue;
                if (email == null || email.isEmpty()) {
                    FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").child(dni).removeValue();
                    continue;
                }

                String emailSafe = email.replace(".", "_");
                FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(emailSafe).child("perfil").get().addOnSuccessListener(perfil -> {
                    if (!perfil.exists()) {
                        FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").child(dni).removeValue();
                    }
                }).addOnFailureListener(e -> FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").child(dni).removeValue());
            }
        });
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

        final String[] emailConfirmado = {""};

        // Primer paso: comprobación de identidad con DNI y correo.
        if (btnVerify != null) {
            btnVerify.setOnClickListener(v -> {
                String dniInput = safeText(etDni).toUpperCase();
                String emailInput = safeText(etEmail);
                if (dniInput.isEmpty() || emailInput.isEmpty()) {
                    Toast.makeText(this, getString(R.string.error_datos_incompletos), Toast.LENGTH_SHORT).show();
                    return;
                }
                String emailSafe = emailInput.replace(".", "_");
                FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(emailSafe).child("perfil").get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            String dniDB = snapshot.child("dni").getValue(String.class);
                            if (dniInput.equals(dniDB)) {
                                emailConfirmado[0] = emailInput;
                                if (layoutStep1 != null) layoutStep1.setVisibility(View.GONE);
                                if (layoutStep2 != null) layoutStep2.setVisibility(View.VISIBLE);
                            } else {
                                Toast.makeText(this, getString(R.string.error_datos_no_coinciden), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.error_usuario_no_encontrado), Toast.LENGTH_SHORT).show();
                        }
                    });
            });
        }

        // Segundo paso: envío del correo de reseteo una vez confirmada la identidad.
        if (btnChange != null) {
            btnChange.setOnClickListener(v -> auth.sendPasswordResetEmail(emailConfirmado[0]).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, getString(R.string.msg_reset_email_enviado), Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, getString(R.string.error_procesar), Toast.LENGTH_SHORT).show();
                }
            }));
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

        if (txtTituloLegal != null) TranslationHelper.translateTextView(txtTituloLegal, resIdTitulo);
        if (txtContenidoLegal != null) TranslationHelper.translateTextView(txtContenidoLegal, resIdContenido);
        if (btnCerrarCruceta != null) btnCerrarCruceta.setOnClickListener(v -> dialog.dismiss());
        if (btnCerrarAbajo != null) btnCerrarAbajo.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }
}
