package com.example.theringprivate;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private final String DB_URL = "https://the-ring-private-default-rtdb.europe-west1.firebasedatabase.app/";
    private CheckBox cbTerms;
    private TextInputLayout tilEmail; // Subido a miembro para acceder desde realizarRegistro

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("My_Lang", "es");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(locale);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- Aplicar Tema Guardado ---
        SharedPreferences prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DarkMode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // 1. Habilitar Edge-to-Edge para aprovechar toda la pantalla
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        pedirPermisosNotificaciones();

        ScrollView scrollRegister = findViewById(R.id.scrollRegister);
        
        // 2. TRUCO PARA EL TECLADO Y BOTONES:
        if (scrollRegister != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollRegister, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                int paddingBottom = Math.max(systemBars.bottom, ime.bottom);
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, paddingBottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        // --- Lógica del Botón de Idioma ---
        LinearLayout btnChangeLang = findViewById(R.id.btnChangeLangReg);
        TextView tvFlag = findViewById(R.id.tvCurrentFlagReg);
        String currentLang = getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("My_Lang", "es");
        if (tvFlag != null) tvFlag.setText(currentLang.equals("es") ? "🇬🇧 EN" : "🇪🇸 ES");
        if (btnChangeLang != null) btnChangeLang.setOnClickListener(v -> mostrarDialogoIdiomas());

        TextInputLayout tilNombre = findViewById(R.id.tilRegNombre);
        TextInputLayout tilApellidos = findViewById(R.id.tilRegApellidos);
        TextInputLayout tilDni = findViewById(R.id.tilRegDni);
        tilEmail = findViewById(R.id.tilRegEmail);
        TextInputLayout tilPassword = findViewById(R.id.tilRegPassword);

        TextInputEditText etNombre = findViewById(R.id.etRegNombre);
        TextInputEditText etApellidos = findViewById(R.id.etRegApellidos);
        TextInputEditText etDni = findViewById(R.id.etRegDni);
        TextInputEditText etEmail = findViewById(R.id.etRegEmail);
        TextInputEditText etPassword = findViewById(R.id.etRegPassword);
        cbTerms = findViewById(R.id.cbTerms);
        TextView tvTermsLink = findViewById(R.id.tvTermsLink);
        TextView tvTermsError = findViewById(R.id.tvTermsError);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);
        ImageView btnBackToLogin = findViewById(R.id.btnBackToLogin);

        if (btnBackToLogin != null) btnBackToLogin.setOnClickListener(v -> finish());
        if (etNombre != null) setupCapitalizeWatcher(etNombre);
        if (etApellidos != null) setupCapitalizeWatcher(etApellidos);
        if (tvTermsLink != null) tvTermsLink.setOnClickListener(v -> mostrarTextoLegal(getString(R.string.settings_terms), getString(R.string.texto_terminos)));

        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                resetErrors(tilNombre, tilApellidos, tilDni, tilEmail, tilPassword);
                if (tvTermsError != null) tvTermsError.setVisibility(View.GONE);

                String nombre = etNombre != null ? etNombre.getText().toString().trim() : "";
                String apellidos = etApellidos != null ? etApellidos.getText().toString().trim() : "";
                String dni = etDni != null ? etDni.getText().toString().trim().toUpperCase() : "";
                String email = etEmail != null ? etEmail.getText().toString().trim() : "";
                String password = etPassword != null ? etPassword.getText().toString().trim() : "";
                String apodoBase = nombre.replace(" ", "").toLowerCase();

                boolean isValid = true;
                if (nombre.isEmpty()) { if (tilNombre != null) tilNombre.setError(getString(R.string.error_obligatorio)); isValid = false; }
                if (apellidos.isEmpty()) { if (tilApellidos != null) tilApellidos.setError(getString(R.string.error_obligatorio)); isValid = false; }
                if (!dni.matches("^[0-9]{8}[A-Z]$")) { if (tilDni != null) tilDni.setError(getString(R.string.error_dni_invalido)); isValid = false; }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { if (tilEmail != null) tilEmail.setError(getString(R.string.error_email_invalido)); isValid = false; }
                if (password.length() < 6) { if (tilPassword != null) tilPassword.setError(getString(R.string.min_6_chars)); isValid = false; }
                if (cbTerms != null && !cbTerms.isChecked()) { if (tvTermsError != null) tvTermsError.setVisibility(View.VISIBLE); isValid = false; }

                if (!isValid) return;
                
                // Comprobar si el DNI ya existe antes de procesar
                FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").child(dni).get().addOnCompleteListener(dniTask -> {
                    if (dniTask.isSuccessful() && dniTask.getResult().exists()) {
                         if (tilDni != null) tilDni.setError(getString(R.string.error_datos_no_coinciden)); // O un error más específico si existiera
                         Toast.makeText(this, "Este DNI ya está registrado", Toast.LENGTH_SHORT).show();
                    } else {
                         generarApodoUnico(apodoBase, apodoFinal -> realizarRegistro(email, password, nombre, apellidos, dni, apodoFinal));
                    }
                });
            });
        }
    }

    private void mostrarDialogoIdiomas() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_settings_dropdown);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.findViewById(R.id.btnLangEs).setOnClickListener(v -> { cambiarIdioma("es"); dialog.dismiss(); });
        dialog.findViewById(R.id.btnLangEn).setOnClickListener(v -> { cambiarIdioma("en"); dialog.dismiss(); });
        View themeSec = dialog.findViewById(R.id.tvThemeTitle).getParent().getParent() instanceof View ? (View)dialog.findViewById(R.id.tvThemeTitle).getParent().getParent() : null;
        if (themeSec != null) themeSec.setVisibility(View.GONE);
        if (dialog.findViewById(R.id.btnMas) != null) dialog.findViewById(R.id.btnMas).setVisibility(View.GONE);
        dialog.show();
    }

    private void cambiarIdioma(String lang) {
        getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().putString("My_Lang", lang).apply();
        recreate();
    }

    private void generarApodoUnico(String base, ApodoCallback callback) {
        FirebaseDatabase.getInstance(DB_URL).getReference("Apodos").child(base).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    generarApodoUnico(base + (int)(Math.random() * 100), callback);
                } else {
                    callback.onApodoGenerated(base);
                }
            } else {
                callback.onApodoGenerated(base);
            }
        });
    }

    private void realizarRegistro(String email, String pass, String nom, String ape, String dni, String apodo) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String emailSafe = email.replace(".", "_");
                Map<String, Object> perfil = new HashMap<>();
                perfil.put("nombreReal", nom + " " + ape); perfil.put("dni", dni);
                perfil.put("correo", email); perfil.put("apodo", apodo); perfil.put("rol", "user");
                Map<String, Object> userData = new HashMap<>(); userData.put("perfil", perfil);
                FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
                db.getReference("Usuarios").child(emailSafe).setValue(userData).addOnCompleteListener(dbTask -> {
                    db.getReference("Apodos").child(apodo).setValue(emailSafe);
                    db.getReference("MapeoDNI").child(dni).setValue(email);
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                });
            } else {
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    if (tilEmail != null) tilEmail.setError(getString(R.string.error_email_existe));
                } else {
                    Toast.makeText(this, getString(R.string.error_procesar), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void pedirPermisosNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void setupCapitalizeWatcher(TextInputEditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isFormatting || s == null || s.length() == 0) return;
                isFormatting = true;
                String input = s.toString(); String[] parts = input.split(" "); StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (!parts[i].isEmpty()) {
                        formatted.append(Character.toUpperCase(parts[i].charAt(0)));
                        if (parts[i].length() > 1) formatted.append(parts[i].substring(1).toLowerCase());
                    }
                    if (i < parts.length - 1) formatted.append(" ");
                }
                if (input.endsWith(" ")) formatted.append(" ");
                if (!formatted.toString().equals(input)) {
                    int pos = editText.getSelectionStart(); editText.setText(formatted.toString());
                    editText.setSelection(Math.min(pos, formatted.length()));
                }
                isFormatting = false;
            }
        });
    }

    private void resetErrors(TextInputLayout... layouts) { for (TextInputLayout l : layouts) if (l != null) l.setError(null); }

    private void mostrarTextoLegal(String titulo, String contenido) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_fullscreen_legal, null);
        TextView txtTituloLegal = view.findViewById(R.id.txtTituloLegal);
        TextView txtContenidoLegal = view.findViewById(R.id.txtContenidoLegal);
        ImageView btnCerrarCruceta = view.findViewById(R.id.btnCerrarCruceta);
        Button btnCerrarAbajo = view.findViewById(R.id.btnCerrarAbajo);

        if (txtTituloLegal != null) txtTituloLegal.setText(titulo);
        if (txtContenidoLegal != null) txtContenidoLegal.setText(contenido);
        
        if (btnCerrarCruceta != null) btnCerrarCruceta.setOnClickListener(v -> dialog.dismiss());
        
        if (btnCerrarAbajo != null) {
            btnCerrarAbajo.setOnClickListener(v -> {
                if (cbTerms != null) cbTerms.setChecked(true);
                dialog.dismiss();
            });
        }

        dialog.setContentView(view);
        dialog.show();
    }

    private interface ApodoCallback { void onApodoGenerated(String apodo); }
}
