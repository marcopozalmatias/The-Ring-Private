package com.example.theringprivate;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CreatePasswordRequest;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.exceptions.CreateCredentialException;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Pantalla de alta de usuario donde se recogen los datos, se validan y se guardan en Firebase.
public class RegisterActivity extends AppCompatActivity {

    // Instancia de Firebase Auth para crear nuevas cuentas y leer el usuario autenticado.
    private FirebaseAuth auth;
    // URL base de la Realtime Database usada por la aplicación.
    private final String DB_URL = "https://the-ring-private-default-rtdb.europe-west1.firebasedatabase.app/";
    // Casilla que obliga al usuario a aceptar las condiciones antes de registrarse.
    private CheckBox cbTerms;
    // Referencia al contenedor del correo para poder mostrar errores desde varios pasos del flujo.
    private TextInputLayout tilEmail;

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
        // Leemos el tema guardado antes de inflar la interfaz para evitar parpadeos visuales.
        SharedPreferences prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DarkMode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // Activamos Edge-to-Edge para que el formulario aproveche toda la altura disponible.
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializamos Firebase y pedimos permisos de notificaciones si el sistema lo requiere.
        auth = FirebaseAuth.getInstance();
        pedirPermisosNotificaciones();

        // Ajustamos los márgenes con insets para que el teclado no tape campos ni botones.
        ScrollView scrollRegister = findViewById(R.id.scrollRegister);
        
        // El listener de insets compensa el espacio del teclado y de las barras del sistema.
        if (scrollRegister != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollRegister, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                int paddingBottom = Math.max(systemBars.bottom, ime.bottom);
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, paddingBottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        // Bloque superior de idioma: actualiza la bandera y abre el selector de idioma.
        LinearLayout btnChangeLang = findViewById(R.id.btnChangeLangReg);
        LinearLayout btnManualReg = findViewById(R.id.btnManualReg);
        TextView tvFlag = findViewById(R.id.tvCurrentFlagReg);
        String currentLang = getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("My_Lang", "es");
        if (tvFlag != null) tvFlag.setText(currentLang.equals("es") ? "🇬🇧 EN" : "🇪🇸 ES");
        if (btnChangeLang != null) btnChangeLang.setOnClickListener(v -> mostrarDialogoIdiomas());

        // Referencias a los campos del formulario de registro para validarlos uno por uno.
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

        // El botón de regreso cierra esta pantalla y vuelve al inicio de sesión.
        if (btnBackToLogin != null) btnBackToLogin.setOnClickListener(v -> finish());
        // Capitalizamos nombre y apellidos para mantener una presentación homogénea.
        if (etNombre != null) setupCapitalizeWatcher(etNombre);
        if (etApellidos != null) setupCapitalizeWatcher(etApellidos);
        // El enlace de términos abre el documento legal y obliga a la aceptación al cerrarlo.
        if (tvTermsLink != null) {
            tvTermsLink.setOnClickListener(v -> mostrarTextoLegal(R.string.texto_terminos_titulo, R.string.texto_terminos, true));
        }
        // El manual de usuario se puede consultar desde el registro para guiar a usuarios nuevos.
        if (btnManualReg != null) {
            btnManualReg.setOnClickListener(v -> mostrarTextoLegal(R.string.texto_manual_usuario_titulo, R.string.texto_manual_usuario, false));
        }

        // El botón principal valida todos los campos y solo después intenta crear la cuenta.
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                // Limpiamos errores anteriores para que cada intento empiece desde cero.
                resetErrors(tilNombre, tilApellidos, tilDni, tilEmail, tilPassword);
                if (tvTermsError != null) tvTermsError.setVisibility(View.GONE);

                // Capturamos los datos que el usuario ha escrito en el formulario.
                String nombre = safeText(etNombre);
                String apellidos = safeText(etApellidos);
                String dni = safeText(etDni).toUpperCase();
                String email = safeText(etEmail);
                String password = safeText(etPassword);

                // Cada validación individual marca el formulario como inválido si falla.
                boolean isValid = true;
                if (nombre.isEmpty()) { if (tilNombre != null) tilNombre.setError(getString(R.string.error_obligatorio)); isValid = false; }
                if (apellidos.isEmpty()) { if (tilApellidos != null) tilApellidos.setError(getString(R.string.error_obligatorio)); isValid = false; }
                if (!dni.matches("^[0-9]{8}[A-Z]$")) { if (tilDni != null) tilDni.setError(getString(R.string.error_dni_invalido)); isValid = false; }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { if (tilEmail != null) tilEmail.setError(getString(R.string.error_email_invalido)); isValid = false; }
                if (!esPasswordSegura(password)) { if (tilPassword != null) tilPassword.setError(getString(R.string.error_password_segura)); isValid = false; }
                if (cbTerms != null && !cbTerms.isChecked()) { if (tvTermsError != null) tvTermsError.setVisibility(View.VISIBLE); isValid = false; }

                if (!isValid) return;
                
                // Comprobamos si el DNI ya existe antes de tocar Auth para evitar registros duplicados.
                FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").child(dni).get().addOnCompleteListener(dniTask -> {
                    if (!dniTask.isSuccessful() || !dniTask.getResult().exists()) {
                        // Si no hay mapeo o la consulta falla, seguimos con el alta normal.
                        realizarRegistro(email, password, nombre, apellidos, dni);
                        return;
                    }

                    // Si el DNI apunta a un correo vacío, limpiamos el dato corrupto y continuamos.
                    String correoMapeado = dniTask.getResult().getValue(String.class);
                    if (correoMapeado == null || correoMapeado.isEmpty()) {
                        FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").child(dni).removeValue();
                        realizarRegistro(email, password, nombre, apellidos, dni);
                        return;
                    }

                    // Convertimos el correo mapeado al formato de clave usado dentro de Firebase Realtime Database.
                    String emailSafeMapeado = correoMapeado.replace(".", "_");
                    FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(emailSafeMapeado).child("perfil").get().addOnSuccessListener(perfilSnapshot -> {
                        // Si el perfil existe de verdad, el DNI está ocupado y bloqueamos el alta.
                        if (perfilSnapshot.exists()) {
                            if (tilDni != null) tilDni.setError(getString(R.string.error_dni_ya_registrado));
                            Toast.makeText(this, getString(R.string.error_dni_ya_registrado), Toast.LENGTH_SHORT).show();
                        } else {
                            // Si el perfil no existe, el mapeo quedó huérfano y se puede reutilizar el DNI.
                            FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").child(dni).removeValue();
                            realizarRegistro(email, password, nombre, apellidos, dni);
                        }
                    }).addOnFailureListener(e -> {
                        // Si la comprobación del perfil falla, preferimos limpiar y permitir el registro.
                        FirebaseDatabase.getInstance(DB_URL).getReference("MapeoDNI").child(dni).removeValue();
                        realizarRegistro(email, password, nombre, apellidos, dni);
                    });
                });
            });
        }
    }

    // Abre el selector de idioma usando el mismo desplegable reutilizable del resto de pantallas.
    private void mostrarDialogoIdiomas() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_settings_dropdown);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Cada botón cambia el idioma y cierra el selector.
        dialog.findViewById(R.id.btnLangEs).setOnClickListener(v -> { cambiarIdioma("es"); dialog.dismiss(); });
        dialog.findViewById(R.id.btnLangEn).setOnClickListener(v -> { cambiarIdioma("en"); dialog.dismiss(); });
        // Ocultamos la sección de tema y el botón extra para que aquí solo aparezca lo relativo al idioma.
        View themeSec = dialog.findViewById(R.id.tvThemeTitle).getParent().getParent() instanceof View ? (View)dialog.findViewById(R.id.tvThemeTitle).getParent().getParent() : null;
        if (themeSec != null) themeSec.setVisibility(View.GONE);
        if (dialog.findViewById(R.id.btnMas) != null) dialog.findViewById(R.id.btnMas).setVisibility(View.GONE);
        dialog.show();
    }

    // Guarda el idioma elegido y reconstruye la actividad para que se refresquen los textos.
    private void cambiarIdioma(String lang) {
        getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().putString("My_Lang", lang).apply();
        recreate();
    }

    // Crea la cuenta en Firebase Authentication y guarda el perfil base en Realtime Database.
    private void realizarRegistro(String email, String pass, String nom, String ape, String dni) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Guardamos el correo en formato seguro para usarlo como clave de nodo.
                String emailSafe = email.replace(".", "_");
                Map<String, Object> perfil = new HashMap<>();
                // El perfil agrupa los datos visibles y necesarios del usuario recién creado.
                perfil.put("nombreReal", nom + " " + ape); perfil.put("dni", dni);
                perfil.put("correo", email); perfil.put("rol", "user");
                Map<String, Object> userData = new HashMap<>(); userData.put("perfil", perfil);
                FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
                db.getReference("Usuarios").child(emailSafe).setValue(userData).addOnCompleteListener(dbTask -> {
                    // Asociamos el DNI con el correo para permitir el inicio de sesión por DNI.
                    db.getReference("MapeoDNI").child(dni).setValue(email);
                    // Intentamos guardar credenciales en el sistema de autocompletado del dispositivo.
                    solicitarGuardadoCredenciales(email, pass, () -> {
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    });
                });
            } else {
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    // Si el correo ya existe, avisamos justo en el campo de email.
                    if (tilEmail != null) tilEmail.setError(getString(R.string.error_email_existe));
                } else {
                    Toast.makeText(this, getString(R.string.error_procesar), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Pide el permiso POST_NOTIFICATIONS únicamente en Android 13 o superior.
    private void pedirPermisosNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    // Normaliza el contenido de un campo de texto para que nombre y apellidos queden capitalizados.
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

    // Limpia todos los mensajes de error visibles en los contenedores del formulario.
    private void resetErrors(TextInputLayout... layouts) { for (TextInputLayout l : layouts) if (l != null) l.setError(null); }

    // Devuelve el texto actual del campo o una cadena vacía si el campo no existe.
    private String safeText(TextInputEditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    // Comprueba que la contraseña cumpla la política mínima exigida por la app.
    private boolean esPasswordSegura(String password) {
        return password != null
                && password.length() >= 6
                && password.matches(".*[A-Z].*")
                && password.matches(".*[^A-Za-z0-9].*");
    }

    // Pide al sistema Android que recuerde las credenciales del usuario si el dispositivo lo permite.
    private void solicitarGuardadoCredenciales(String email, String password, Runnable onDone) {
        try {
            CredentialManager credentialManager = CredentialManager.create(this);
            CreatePasswordRequest request = new CreatePasswordRequest(email, password);
            credentialManager.createCredentialAsync(
                    this,
                    request,
                    new CancellationSignal(),
                    ContextCompat.getMainExecutor(this),
                    new CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>() {
                        @Override
                        public void onResult(CreateCredentialResponse result) {
                            onDone.run();
                        }

                        @Override
                        public void onError(CreateCredentialException e) {
                            onDone.run();
                        }
                    }
            );
        } catch (Exception e) {
            onDone.run();
        }
    }

    // Abre un documento legal a pantalla completa y opcionalmente marca la aceptación de términos al cerrar.
    private void mostrarTextoLegal(int resIdTitulo, int resIdContenido, boolean aceptarTerminosAlCerrar) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_fullscreen_legal, null, false);
        TextView txtTituloLegal = view.findViewById(R.id.txtTituloLegal);
        TextView txtContenidoLegal = view.findViewById(R.id.txtContenidoLegal);
        ImageView btnCerrarCruceta = view.findViewById(R.id.btnCerrarCruceta);
        Button btnCerrarAbajo = view.findViewById(R.id.btnCerrarAbajo);

        if (txtTituloLegal != null) TranslationHelper.translateTextView(txtTituloLegal, resIdTitulo);
        if (txtContenidoLegal != null) TranslationHelper.translateTextView(txtContenidoLegal, resIdContenido);

        if (btnCerrarCruceta != null) btnCerrarCruceta.setOnClickListener(v -> dialog.dismiss());
        
        if (btnCerrarAbajo != null) {
            btnCerrarAbajo.setOnClickListener(v -> {
                // Solo en términos y condiciones marcamos la casilla como aceptada al salir por el botón inferior.
                if (aceptarTerminosAlCerrar && cbTerms != null) cbTerms.setChecked(true);
                dialog.dismiss();
            });
        }

        dialog.setContentView(view);
        dialog.show();
    }

}
