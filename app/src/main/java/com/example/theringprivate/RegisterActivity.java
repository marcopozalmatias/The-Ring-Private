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
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Pantalla de alta de usuario donde se recogen los datos, se validan y se guardan en Firebase.
public class RegisterActivity extends AppCompatActivity {

    // Clave del guardado temporal del formulario para no perderlo al cambiar idioma o fallar validaciones.
    private static final String PREF_REGISTER_DRAFT = "RegisterDraft";
    private static final String KEY_REGISTER_PASSPORT_COUNTRY = "passportCountry";

    // Instancia de Firebase Auth para crear nuevas cuentas y leer el usuario autenticado.
    private FirebaseAuth auth;
    // Casilla que obliga al usuario a aceptar las condiciones antes de registrarse.
    private CheckBox cbTerms;
    // Referencia al contenedor del correo para poder mostrar errores desde varios pasos del flujo.
    private TextInputLayout tilEmail;
    // Botón de registro y barra de progreso para feedback visual.
    private MaterialButton btnRegister;
    private ProgressBar pbRegister;

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
        TextInputLayout tilDocType = findViewById(R.id.tilDocType);
        TextInputLayout tilDni = findViewById(R.id.tilRegDni);
        TextInputLayout tilPassportCountry = findViewById(R.id.tilPassportCountry);
        tilEmail = findViewById(R.id.tilRegEmail);
        TextInputLayout tilPassword = findViewById(R.id.tilRegPassword);

         // Configurar el dropdown de tipo de documento
          com.google.android.material.textfield.MaterialAutoCompleteTextView actvDocType = findViewById(R.id.actvDocType);
         if (actvDocType != null) {
             String[] docTypes = new String[]{getString(R.string.doc_dni_nie), getString(R.string.doc_pasaporte)};
             ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, docTypes);
             actvDocType.setAdapter(adapter);
             actvDocType.setText(getString(R.string.doc_dni_nie), false);
         }

        com.google.android.material.textfield.MaterialAutoCompleteTextView actvPassportCountry = findViewById(R.id.actvPassportCountry);
        configurarSelectorPaisesPasaporte(actvPassportCountry, Locale.getDefault());

        TextInputEditText etNombre = findViewById(R.id.etRegNombre);
        TextInputEditText etApellidos = findViewById(R.id.etRegApellidos);
        TextInputEditText etDni = findViewById(R.id.etRegDni);
        TextInputEditText etEmail = findViewById(R.id.etRegEmail);
        TextInputEditText etPassword = findViewById(R.id.etRegPassword);

        if (actvDocType != null) {
            actvDocType.setOnItemClickListener((parent, view, position, id) -> actualizarCampoDocumentoSegunTipo(etDni, tilPassportCountry, actvPassportCountry, actvDocType.getText() != null ? actvDocType.getText().toString() : ""));
        }
        cbTerms = findViewById(R.id.cbTerms);
        TextView tvTermsLink = findViewById(R.id.tvTermsLink);
        TextView tvTermsError = findViewById(R.id.tvTermsError);
        btnRegister = findViewById(R.id.btnRegister);
        pbRegister = findViewById(R.id.pbRegister);
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
                resetErrors(tilNombre, tilApellidos, tilDocType, tilDni, tilPassportCountry, tilEmail, tilPassword);
                if (tvTermsError != null) tvTermsError.setVisibility(View.GONE);

                // Capturamos los datos que el usuario ha escrito en el formulario.
                String nombre = safeText(etNombre);
                String apellidos = safeText(etApellidos);
                String tipoDoc = safeTextAutocomplete(actvDocType);
                String documento = normalizarDocumento(safeText(etDni));
                String paisPasaporte = codigoPaisPasaporte(actvPassportCountry);
                actualizarCampoDocumentoSegunTipo(etDni, tilPassportCountry, actvPassportCountry, tipoDoc);
                final String email = safeText(etEmail);
                final String password = safeText(etPassword);
                String finalDocKey = claveDocumento(tipoDoc, documento);

                // Cada validación individual marca el formulario como inválido si falla.
                boolean isValid = true;
                if (nombre.isEmpty()) { if (tilNombre != null) tilNombre.setError(getString(R.string.error_obligatorio)); isValid = false; }
                if (apellidos.isEmpty()) { if (tilApellidos != null) tilApellidos.setError(getString(R.string.error_obligatorio)); isValid = false; }
                if (tipoDoc.isEmpty()) { if (tilDocType != null) tilDocType.setError(getString(R.string.error_obligatorio)); isValid = false; }

                String errorDoc = validarDocumentoConTipo(documento, tipoDoc);
                if (errorDoc != null) {
                    if (tilDni != null) tilDni.setError(errorDoc);
                    isValid = false;
                }

                String errorPais = validarPaisPasaporte(tipoDoc, paisPasaporte);
                if (errorPais != null) {
                    if (tilPassportCountry != null) tilPassportCountry.setError(errorPais);
                    isValid = false;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { if (tilEmail != null) tilEmail.setError(getString(R.string.error_email_invalido)); isValid = false; }
                if (!esPasswordSegura(password)) {
                    if (tilPassword != null) {
                        tilPassword.setError(getString(R.string.error_password_segura));
                    }
                    isValid = false;
                }
                if (cbTerms != null && !cbTerms.isChecked()) { if (tvTermsError != null) tvTermsError.setVisibility(View.VISIBLE); isValid = false; }

                if (!isValid) {
                    guardarBorradorRegistro();
                    return;
                }

                // Mostramos progreso y bloqueamos el botón para evitar clics dobles y dar feedback.
                if (pbRegister != null) pbRegister.setVisibility(View.VISIBLE);
                if (btnRegister != null) {
                    btnRegister.setEnabled(false);
                    btnRegister.setText("");
                }

                // Iniciamos el proceso de registro. La validación de duplicados se hará
                // dentro de realizarRegistroCompleto tras la autenticación en Firebase.
                realizarRegistroCompleto(email, password, nombre, apellidos, documento, tipoDoc, finalDocKey, paisPasaporte);
            });
        }

        // Recuperamos el último borrador guardado para que un cambio de idioma no vacíe el formulario.
        restaurarBorradorRegistro(tilPassportCountry, actvPassportCountry);

         // Guardamos el contenido a medida que el usuario escribe para no perderlo si cambia el idioma.
           configurarGuardadoBorrador(etNombre, etApellidos, actvDocType, etDni, actvPassportCountry, etEmail, etPassword);

         actualizarCampoDocumentoSegunTipo(etDni, tilPassportCountry, actvPassportCountry, actvDocType != null && actvDocType.getText() != null ? actvDocType.getText().toString() : getString(R.string.doc_dni_nie));
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
        guardarBorradorRegistro();
        getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().putString("My_Lang", lang).apply();
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(getResources().getConfiguration());
        config.setLocale(locale);

        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    // Crea la cuenta en Firebase Authentication y guarda el perfil únicamente en el nodo "usuarios".
    private void realizarRegistroCompleto(String email, String pass, String nom, String ape, String doc, String tipoDoc, String docKey, String paisPasaporteCode) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                com.google.firebase.auth.FirebaseUser user = task.getResult().getUser();
                if (user == null) {
                    errorRegistro("Error: Usuario nulo tras creación");
                    return;
                }
                String uid = user.getUid();
                FirebaseDatabase db = FirebaseDatabase.getInstance();
                String tipoDocCode = codigoDocumento(tipoDoc);

                // Guardamos solo los 6 campos esenciales en la tabla usuarios
                Map<String, Object> userData = new HashMap<>();
                userData.put("name", nom);
                userData.put("surname", ape);
                userData.put("documento", doc);
                userData.put("tipoDocumento", tipoDocCode);
                userData.put("email", email);
                userData.put("acceptedTerms", true);
                if (getString(R.string.doc_pasaporte).equals(tipoDoc) && paisPasaporteCode != null && !paisPasaporteCode.isEmpty()) {
                    userData.put("paisPasaporteCodigo", paisPasaporteCode);
                    userData.put("paisPasaporte", nombrePaisDesdeCodigo(paisPasaporteCode, Locale.getDefault()));
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("usuarios/" + uid, userData);
                updates.put("MapeoDNI/" + docKey, email);

                // Escribimos todo de forma atómica para que Auth y la base de datos no queden desincronizados.
                db.getReference().updateChildren(updates).addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        limpiarBorradorRegistro();
                        solicitarGuardadoCredenciales(email, pass, () -> {
                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        });
                    } else {
                        user.delete();
                        String errorMsg = dbTask.getException() != null ? dbTask.getException().getMessage() : "Desconocido";
                        android.util.Log.e("RegisterActivity", "Error al guardar en DB: " + errorMsg);
                        errorRegistro(getString(R.string.error_procesar) + " (DB: " + errorMsg + ")");
                    }
                });
            } else {
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    if (tilEmail != null) tilEmail.setError(getString(R.string.error_email_existe));
                    errorRegistro(getString(R.string.error_email_existe));
                } else {
                    android.util.Log.e("RegisterActivity", "Error en Auth: ", task.getException());
                    errorRegistro(getString(R.string.error_procesar));
                }
            }
        });
    }

    // Restablece la interfaz tras un error y muestra un mensaje al usuario.
    private void errorRegistro(String mensaje) {
        if (pbRegister != null) pbRegister.setVisibility(View.GONE);
        if (btnRegister != null) {
            btnRegister.setEnabled(true);
            btnRegister.setText(R.string.register_btn_signup);
        }
        android.util.Log.e("RegisterActivity", "Error Registro: " + mensaje);
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        guardarBorradorRegistro();
        if (mensaje != null && !mensaje.isEmpty()) {
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        }
    }

     // Valida el documento según su tipo (DNI/NIE o Pasaporte).
     private String validarDocumentoConTipo(String documento, String tipo) {
         if (documento == null || documento.isEmpty()) return getString(R.string.error_obligatorio);
         String doc = documento.toUpperCase().trim();

         if (tipo.equals(getString(R.string.doc_dni_nie))) {
             // Validar como DNI
             if (doc.matches("^[0-9]{8}[TRWAGMYFPDXBNJZSQVHLCKE]$")) {
                 String numeros = doc.substring(0, 8);
                 char letra = doc.charAt(8);
                 if (calcularLetraDni(numeros) == letra) return null;
             }
             // Validar como NIE
             if (doc.matches("^[XYZ][0-9]{7}[TRWAGMYFPDXBNJZSQVHLCKE]$")) {
                 String prefijo = doc.substring(0, 1);
                 String resto = doc.substring(1, 8);
                 char letraFinal = doc.charAt(8);
                 String numerosParaCalculo = prefijo.replace("X", "0").replace("Y", "1").replace("Z", "2") + resto;
                 if (calcularLetraDni(numerosParaCalculo) == letraFinal) return null;
             }
             return getString(R.string.error_dni_invalido);
         }

         if (tipo.equals(getString(R.string.doc_pasaporte))) {
             if (doc.matches("^[A-Z0-9]{6,12}$")) return null;
             return getString(R.string.error_pasaporte_invalido);
         }

         return getString(R.string.error_procesar);
     }

    private char calcularLetraDni(String numeros) {
        String letras = "TRWAGMYFPDXBNJZSQVHLCKE";
        try {
            int valor = Integer.parseInt(numeros);
            return letras.charAt(valor % 23);
        } catch (NumberFormatException e) {
            return ' ';
        }
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

     // Guarda el contenido actual del registro en preferencias temporales.
     private void guardarBorradorRegistro() {
         SharedPreferences.Editor editor = getSharedPreferences(PREF_REGISTER_DRAFT, Context.MODE_PRIVATE).edit();
         AutoCompleteTextView actvPassportCountry = findViewById(R.id.actvPassportCountry);
         editor.putString("nombre", textoCampo(R.id.etRegNombre));
         editor.putString("apellidos", textoCampo(R.id.etRegApellidos));
         editor.putString("docType", codigoDocumento(textoCampoActv(R.id.actvDocType)));
         editor.putString("dni", textoCampo(R.id.etRegDni));
         editor.putString(KEY_REGISTER_PASSPORT_COUNTRY, codigoPaisPasaporte(actvPassportCountry));
         editor.putString("email", textoCampo(R.id.etRegEmail));
         editor.putString("password", textoCampo(R.id.etRegPassword));
         editor.putBoolean("terms", cbTerms != null && cbTerms.isChecked());
         editor.apply();
     }

     // Restaura el contenido guardado si el usuario cambió idioma o la actividad se recreó.
      private void restaurarBorradorRegistro(TextInputLayout tilPassportCountry, com.google.android.material.textfield.MaterialAutoCompleteTextView actvPassportCountry) {
         SharedPreferences prefs = getSharedPreferences(PREF_REGISTER_DRAFT, Context.MODE_PRIVATE);
         TextInputEditText etNombre = findViewById(R.id.etRegNombre);
         TextInputEditText etApellidos = findViewById(R.id.etRegApellidos);
         com.google.android.material.textfield.MaterialAutoCompleteTextView actvDocType = findViewById(R.id.actvDocType);
         TextInputEditText etDni = findViewById(R.id.etRegDni);
         TextInputEditText etEmail = findViewById(R.id.etRegEmail);
         TextInputLayout tilPassword = findViewById(R.id.tilRegPassword);
         TextInputEditText etPassword = tilPassword != null ? (TextInputEditText) tilPassword.getEditText() : null;

        if (etNombre != null) {
            etNombre.setText(prefs.getString("nombre", ""));
        }
        if (etApellidos != null) {
            etApellidos.setText(prefs.getString("apellidos", ""));
        }
        if (actvDocType != null) {
            String docTypeCode = prefs.getString("docType", "DNI/NIE");
            String docTypeLabel = etiquetaDocumentoDesdeCodigo(docTypeCode);
            actvDocType.setText(docTypeLabel.isEmpty() ? getString(R.string.doc_dni_nie) : docTypeLabel, false);
        }
        if (etDni != null) etDni.setText(prefs.getString("dni", ""));
        if (actvPassportCountry != null) {
              String paisPasaporte = prefs.getString(KEY_REGISTER_PASSPORT_COUNTRY, "");
              if (!paisPasaporte.isEmpty()) {
                  String nombrePais = nombrePaisDesdeCodigo(paisPasaporte, Locale.getDefault());
                  actvPassportCountry.setText(nombrePais.isEmpty() ? paisPasaporte : nombrePais, false);
                  actvPassportCountry.setTag(paisPasaporte.toUpperCase(Locale.ROOT));
              }
          }
          actualizarCampoDocumentoSegunTipo(etDni, tilPassportCountry, actvPassportCountry, actvDocType != null && actvDocType.getText() != null ? actvDocType.getText().toString() : "");
         if (etEmail != null) etEmail.setText(prefs.getString("email", ""));
         if (etPassword != null) etPassword.setText(prefs.getString("password", ""));
         if (cbTerms != null) cbTerms.setChecked(prefs.getBoolean("terms", false));
     }

     // Engancha los cambios del formulario para que el borrador se vaya guardando automáticamente.
      private void configurarGuardadoBorrador(TextInputEditText etNombre, TextInputEditText etApellidos, com.google.android.material.textfield.MaterialAutoCompleteTextView actvDocType, TextInputEditText etDni, com.google.android.material.textfield.MaterialAutoCompleteTextView actvPassportCountry, TextInputEditText etEmail, TextInputEditText etPassword) {
         TextWatcher watcher = new TextWatcher() {
             @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
             @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
             @Override public void afterTextChanged(Editable s) { guardarBorradorRegistro(); }
         };
          if (etNombre != null) etNombre.addTextChangedListener(watcher);
          if (etApellidos != null) etApellidos.addTextChangedListener(watcher);
          if (actvDocType != null) actvDocType.addTextChangedListener(watcher);
          if (etDni != null) etDni.addTextChangedListener(watcher);
          if (actvPassportCountry != null) actvPassportCountry.addTextChangedListener(watcher);
          if (etEmail != null) etEmail.addTextChangedListener(watcher);
          if (etPassword != null) etPassword.addTextChangedListener(watcher);
          if (cbTerms != null) cbTerms.setOnCheckedChangeListener((buttonView, isChecked) -> guardarBorradorRegistro());
     }

     // Devuelve el texto actual de un campo concreto o vacío si no existe.
     private String textoCampo(int resId) {
         View view = findViewById(resId);
         if (view instanceof TextInputEditText) {
             Editable text = ((TextInputEditText) view).getText();
             return text != null ? text.toString().trim() : "";
         }
         return "";
     }

     // Devuelve el texto de un AutoCompleteTextView o vacío si no existe.
     private String textoCampoActv(int resId) {
         View view = findViewById(resId);
         if (view instanceof AutoCompleteTextView) {
             Editable text = ((AutoCompleteTextView) view).getText();
             return text != null ? text.toString().trim() : "";
         }
         return "";
     }

    // Elimina el borrador temporal una vez el registro termina correctamente.
    private void limpiarBorradorRegistro() {
        getSharedPreferences(PREF_REGISTER_DRAFT, Context.MODE_PRIVATE).edit().clear().apply();
    }

     // Devuelve el texto actual del campo o una cadena vacía si el campo no existe.
     private String safeText(TextInputEditText editText) {
         return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
     }

     // Devuelve el texto del AutoCompleteTextView o una cadena vacía si no existe.
     private String safeTextAutocomplete(AutoCompleteTextView editText) {
         return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
     }

      // Devuelve el código ISO del país seleccionado en el desplegable del pasaporte.
      private String codigoPaisPasaporte(AutoCompleteTextView editText) {
          if (editText == null || editText.getText() == null) return "";
          Object tag = editText.getTag();
          if (tag instanceof String) {
              String code = ((String) tag).trim().toUpperCase(Locale.ROOT);
              if (!code.isEmpty()) return code;
          }

          String textoPais = limpiarNombrePais(editText.getText().toString().trim());
          if (textoPais.isEmpty()) return "";

          String codigo = codigoPaisDesdeNombre(textoPais, Locale.getDefault());
          if (!codigo.isEmpty()) editText.setTag(codigo);
          return codigo;
      }

      // Convierte el nombre visible de un país en su código ISO-3166-1 alpha-2.
      private String codigoPaisDesdeNombre(String nombrePais, Locale locale) {
          if (nombrePais == null || nombrePais.trim().isEmpty()) return "";
          String textoNormalizado = nombrePais.trim();
          if (textoNormalizado.matches("^[A-Za-z]{2}$")) {
              String codigoDirecto = textoNormalizado.toUpperCase(Locale.ROOT);
              if (!new Locale("", codigoDirecto).getDisplayCountry(locale != null ? locale : Locale.getDefault()).trim().isEmpty()) {
                  return codigoDirecto;
              }
          }

          List<CountryOption> paises = obtenerPaisesOrdenados(locale);
          for (CountryOption pais : paises) {
               if (pais.nombre.equalsIgnoreCase(textoNormalizado) || pais.codigo.equalsIgnoreCase(textoNormalizado)) return pais.codigo;
          }
          return "";
      }

      // Obtiene el nombre visible del país a partir de su código ISO-3166-1 alpha-2.
      private String nombrePaisDesdeCodigo(String codigoPais, Locale locale) {
          if (codigoPais == null || codigoPais.trim().isEmpty()) return "";
          Locale paisLocale = new Locale("", codigoPais.trim().toUpperCase(Locale.ROOT));
          String nombre = paisLocale.getDisplayCountry(locale != null ? locale : Locale.getDefault());
          return limpiarNombrePais(nombre);
      }

      // Elimina cualquier alias o nombre alternativo entre paréntesis para que se muestre solo una versión.
      private String limpiarNombrePais(String nombrePais) {
          if (nombrePais == null) return "";
          return nombrePais.replaceAll("\\s*\\([^)]*\\)", "").trim();
      }

      // Genera la lista completa de países ordenada alfabéticamente según el idioma activo.
      private List<CountryOption> obtenerPaisesOrdenados(Locale locale) {
          Locale localeActiva = locale != null ? locale : Locale.getDefault();
          List<CountryOption> paises = new ArrayList<>();
          for (String codigo : Locale.getISOCountries()) {
               String nombre = limpiarNombrePais(new Locale("", codigo).getDisplayCountry(localeActiva));
              if (!nombre.trim().isEmpty()) {
                  paises.add(new CountryOption(codigo.toUpperCase(Locale.ROOT), nombre.trim()));
              }
          }

          Collator collator = Collator.getInstance(localeActiva);
          collator.setStrength(Collator.PRIMARY);
          paises.sort((a, b) -> collator.compare(a.nombre, b.nombre));
          return paises;
      }

      // Prepara el desplegable de países del pasaporte con la lista del idioma actual.
      private void configurarSelectorPaisesPasaporte(com.google.android.material.textfield.MaterialAutoCompleteTextView actvPassportCountry, Locale locale) {
          if (actvPassportCountry == null) return;
          List<CountryOption> paises = obtenerPaisesOrdenados(locale);
          CountryOptionAdapter adapter = new CountryOptionAdapter(this, paises);
          actvPassportCountry.setAdapter(adapter);
          actvPassportCountry.setThreshold(1);
          actvPassportCountry.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
          actvPassportCountry.setOnClickListener(v -> actvPassportCountry.showDropDown());
          actvPassportCountry.setOnFocusChangeListener((v, hasFocus) -> {
              if (hasFocus) actvPassportCountry.showDropDown();
          });
           actvPassportCountry.setOnItemClickListener((parent, view, position, id) -> {
                CountryOption pais = adapter.getItem(position);
                if (pais != null) {
                   actvPassportCountry.setTag(pais.codigo);
                   actvPassportCountry.setText(pais.nombre, false);
              }
              guardarBorradorRegistro();
          });
      }

      // Ajusta la pista del documento y la visibilidad del país según el tipo seleccionado.
       private void actualizarCampoDocumentoSegunTipo(TextInputEditText etDni, TextInputLayout tilPassportCountry, AutoCompleteTextView actvPassportCountry, String tipoDoc) {
          if (etDni == null) return;
          String tipo = tipoDoc != null ? tipoDoc.trim() : "";
          if (tipo.equals(getString(R.string.doc_pasaporte))) {
              etDni.setHint(R.string.register_passport_number_hint);
               if (tilPassportCountry != null) tilPassportCountry.setVisibility(View.VISIBLE);
          } else if (tipo.equals(getString(R.string.doc_dni_nie))) {
              etDni.setHint(R.string.register_document_number_hint);
               if (tilPassportCountry != null) tilPassportCountry.setVisibility(View.GONE);
          } else {
              etDni.setHint(R.string.register_document_number_hint);
               if (tilPassportCountry != null) tilPassportCountry.setVisibility(View.GONE);
          }

           if (actvPassportCountry != null && !tipo.equals(getString(R.string.doc_pasaporte))) {
               actvPassportCountry.setError(null);
           }
           if (tilPassportCountry != null) {
               tilPassportCountry.setError(null);
           }
      }

      // Verifica que el país del pasaporte se haya elegido cuando el tipo de documento lo exige.
      private String validarPaisPasaporte(String tipo, String paisCodigo) {
          if (tipo != null && tipo.trim().equals(getString(R.string.doc_pasaporte))) {
              if (paisCodigo.isEmpty()) return getString(R.string.error_pasaporte_pais_obligatorio);
          }
          return null;
      }

     // Genera una clave única para comprobar duplicados por tipo + número.
     private String claveDocumento(String tipoDoc, String documento) {
          String tipo = tipoDoc != null ? tipoDoc.trim() : "";
          if (tipo.equals(getString(R.string.doc_pasaporte))) {
              tipo = "ID";
          } else {
              tipo = codigoDocumento(tipoDoc);
          }
         String doc = normalizarDocumento(documento);
         return tipo + "_" + doc;
     }

      // Unifica el formato del documento para validación y guardado.
      private String normalizarDocumento(String documento) {
          return documento != null ? documento.trim().replace(" ", "").toUpperCase(Locale.ROOT) : "";
      }

       // Convierte el texto visible del selector en un código estable para la base de datos.
       private String codigoDocumento(String tipoDoc) {
           if (tipoDoc == null) return "";
           String tipo = tipoDoc.trim();
           if (tipo.equals(getString(R.string.doc_dni_nie))) return "DNI/NIE";
           if (tipo.equals(getString(R.string.doc_pasaporte))) return "ID/PASAPORTE";
           return tipo.toUpperCase(Locale.ROOT);
       }

       // Convierte el código estable a la etiqueta visible del idioma activo.
       private String etiquetaDocumentoDesdeCodigo(String codigo) {
            if (codigo == null) return "";
            switch (codigo.trim().toUpperCase(Locale.ROOT)) {
                case "DNI/NIE":
                    return getString(R.string.doc_dni_nie);
                 case "ID/PASAPORTE":
                 case "ID":
                    return getString(R.string.doc_pasaporte);
                default:
                    return codigo.trim();
            }
       }

    // Comprueba que la contraseña cumpla la política mínima exigida por la app.
    private boolean esPasswordSegura(String password) {
        return password != null
                && password.length() >= 6
                && password.matches(".*[A-Z].*")
                && password.matches(".*[^A-Za-z0-9].*");
    }

    // Estructura simple para almacenar el código y el nombre visible de cada país.
    private static final class CountryOption {
        final String codigo;
        final String nombre;

        CountryOption(String codigo, String nombre) {
            this.codigo = codigo;
            this.nombre = nombre;
        }

        @NonNull
        @Override
        public String toString() {
            return nombre;
        }
    }

    // Adaptador filtrable que permite buscar países por nombre o por código ISO.
    private static final class CountryOptionAdapter extends ArrayAdapter<CountryOption> {
        private final List<CountryOption> originalItems;
        private final Filter countryFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint != null ? constraint.toString().trim().toLowerCase(Locale.ROOT) : "";
                List<CountryOption> filtered = new ArrayList<>();
                if (query.isEmpty()) {
                    filtered.addAll(originalItems);
                } else {
                    for (CountryOption item : originalItems) {
                        String nombre = item.nombre.toLowerCase(Locale.ROOT);
                        String codigo = item.codigo.toLowerCase(Locale.ROOT);
                        if (nombre.contains(query) || codigo.contains(query)) {
                            filtered.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                results.count = filtered.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                if (results != null && results.values instanceof List) {
                    //noinspection unchecked
                    addAll((List<CountryOption>) results.values);
                }
                notifyDataSetChanged();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                return resultValue instanceof CountryOption ? ((CountryOption) resultValue).nombre : super.convertResultToString(resultValue);
            }
        };

        CountryOptionAdapter(Context context, List<CountryOption> items) {
            super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(items));
            this.originalItems = new ArrayList<>(items);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return countryFilter;
        }
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
                    new CredentialManagerCallback<>() {
                        @Override
                        public void onResult(CreateCredentialResponse result) {
                            onDone.run();
                        }

                        @Override
                        public void onError(@NonNull CreateCredentialException e) {
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
        ViewGroup root = findViewById(android.R.id.content);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_fullscreen_legal, root, false);
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
