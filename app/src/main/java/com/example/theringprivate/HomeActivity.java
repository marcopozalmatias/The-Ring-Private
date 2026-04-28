package com.example.theringprivate;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONException;
import org.json.JSONObject;

// Pantalla principal tras el login, donde conviven el QR, las notificaciones y la navegación a perfiles y ajustes.
public class HomeActivity extends AppCompatActivity {

    // URL de la base de datos compartida por toda la app.
    private final String DB_URL = "https://the-ring-private-default-rtdb.europe-west1.firebasedatabase.app/";
    // Nodo global desde el que se replican avisos de dirección a cada usuario.
    private final String GLOBAL_NOTIFICATIONS_PATH = "NotificacionesGlobal";
    // Tiempo entre refrescos del QR para que el contenido cambie sin intervención manual.
    private static final long QR_REFRESH_MS = 15000L;
    // Vida útil de cada token temporal asociado al QR.
    private static final long QR_TOKEN_TTL_MS = 60000L;
    // Capa visual que anima la apertura y cierre de la pantalla del QR.
    private SoftRevealFrameLayout layoutQrOverlay;
    // Botón flotante que abre el QR personal del usuario.
    private FloatingActionButton fabQr;
    // Máscara que se usa para animar la apertura y cierre de los fragmentos laterales.
    private SoftRevealFrameLayout fragmentContainerMask;
    // Recycler principal donde se pintan las notificaciones visibles en la portada.
    private RecyclerView rvNotifHome;
    // Adaptador interno específico para la lista reducida de notificaciones de inicio.
    private NotificacionesHomeAdapter adapterNotifHome;
    // Copia en memoria de todas las notificaciones que llegan desde la base de datos del usuario.
    private final List<Notificacion> listaNotificacionesTodas = new ArrayList<>();
    // Lista visible ya filtrada y ordenada para la UI.
    private List<Notificacion> listaNotificaciones = new ArrayList<>();
    // Identificadores de notificaciones que el usuario ha eliminado de forma permanente.
    private final Set<String> notificacionesEliminadas = new HashSet<>();
    // Botón flotante de contacto rápido con soporte por WhatsApp.
    private View fabWhatsapp;
    // Último botón pulsado para centrar correctamente la animación de salida.
    private View ultimoBotonPulsado = null;
    // Handler principal que reprograma el refresco periódico del QR.
    private Handler qrHandler = new Handler(Looper.getMainLooper());
    // Indica si la pantalla del QR está abierta para decidir si se sigue refrescando.
    private boolean isQrVisible = false;
    // Correo actual transformado para poder usarse como clave en Firebase.
    private String currentUserEmailSafe = "";
    // Valor aleatorio que cambia en cada apertura del QR para invalidar capturas antiguas.
    private String qrSessionNonce = "";

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
        // Aplicamos el tema almacenado antes de mostrar cualquier elemento visual.
        SharedPreferences prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DarkMode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Ajustamos márgenes con insets para respetar barras del sistema y navegación.
        View mainCoordinator = findViewById(R.id.main_coordinator);
        if (mainCoordinator != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainCoordinator, (v, insets) -> {
                var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        layoutQrOverlay = findViewById(R.id.layoutQrOverlay);
        fabQr = findViewById(R.id.fabQr);
        Button btnCerrarQr = findViewById(R.id.btnCerrarQr);
        fragmentContainerMask = findViewById(R.id.fragment_container);
        rvNotifHome = findViewById(R.id.rvNotificacionesHome);
        fabWhatsapp = findViewById(R.id.fabWhatsapp);

        // Preparamos la lista de notificaciones, el acceso a WhatsApp y la carga del usuario.
        setupNotificationsHome();
        setupDraggableWhatsapp();
        cargarDatosUsuario();

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                if (fabQr != null) fabQr.hide();
                if (fabWhatsapp != null) fabWhatsapp.setVisibility(View.GONE);
            } else {
                if (fabQr != null) fabQr.show();
                if (fabWhatsapp != null) fabWhatsapp.setVisibility(View.VISIBLE);
            }
        });

        // El botón superior abre el pequeño menú de ajustes rápidos.
        View btnAjustesTop = findViewById(R.id.btnAjustesTop);
        if (btnAjustesTop != null) btnAjustesTop.setOnClickListener(this::mostrarDesplegableAjustesHome);
        
        // Navegación hacia el perfil personal.
        View btnNavPerfil = findViewById(R.id.btnNavPerfil);
        if (btnNavPerfil != null) {
            btnNavPerfil.setOnClickListener(v -> lanzarAnimacionGota(v, () -> 
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFragment()).addToBackStack(null).commit()));
        }

        // Navegación hacia el documento de normas.
        View btnNavNormas = findViewById(R.id.btnNavNormas);
        if (btnNavNormas != null) {
            btnNavNormas.setOnClickListener(v -> lanzarAnimacionGota(v, () -> 
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new NormasFragment()).addToBackStack(null).commit()));
        }

        // El botón flotante del QR abre la vista y dispara su generación inmediata.
        if (fabQr != null) {
            fabQr.setOnClickListener(v -> {
                abrirPantallaQrConOndaDifuminada();
                iniciarGeneracionQR();
            });
        }

        // Cerramos la vista del QR con la misma animación suave con la que se abrió.
        if (btnCerrarQr != null) btnCerrarQr.setOnClickListener(v -> cerrarPantallaQrConOndaDifuminada());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    cerrarFragmentoAnimado();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // Muestra el desplegable rápido de ajustes desde la portada.
    private void mostrarDesplegableAjustesHome(View anchor) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_settings_dropdown);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.gravity = Gravity.TOP | Gravity.END;
            params.x = 60;
            params.y = 160;
            dialog.getWindow().setAttributes(params);
        }

        SharedPreferences prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("My_Lang", "es");
        TextView tvLangTitle = dialog.findViewById(R.id.tvLangTitle);
        if (tvLangTitle != null) {
            tvLangTitle.setText(lang.equals("es") ? "Idioma (Language)" : "Language (Idioma)");
        }

        // El desplegable permite cambiar idioma y tema sin entrar al panel completo.
        View btnLangEs = dialog.findViewById(R.id.btnLangEs);
        if (btnLangEs != null) btnLangEs.setOnClickListener(v -> { cambiarIdioma("es"); dialog.dismiss(); });
        
        View btnLangEn = dialog.findViewById(R.id.btnLangEn);
        if (btnLangEn != null) btnLangEn.setOnClickListener(v -> { cambiarIdioma("en"); dialog.dismiss(); });
        
        View btnThemeLight = dialog.findViewById(R.id.btnThemeLight);
        if (btnThemeLight != null) btnThemeLight.setOnClickListener(v -> { cambiarTema(false); dialog.dismiss(); });
        
        View btnThemeDark = dialog.findViewById(R.id.btnThemeDark);
        if (btnThemeDark != null) btnThemeDark.setOnClickListener(v -> { cambiarTema(true); dialog.dismiss(); });
        
        View btnMas = dialog.findViewById(R.id.btnMas);
        if (btnMas != null) {
            btnMas.setOnClickListener(v -> {
                dialog.dismiss();
                lanzarAnimacionGota(anchor, () -> getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment()).addToBackStack(null).commit());
            });
        }

        dialog.show();
    }

    // Cambia el idioma global de la app y reconstruye la actividad actual.
    private void cambiarIdioma(String lang) {
        getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().putString("My_Lang", lang).apply();
        recreate();
    }

    // Guarda el tema claro u oscuro y aplica el modo nocturno de inmediato.
    private void cambiarTema(boolean dark) {
        getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().putBoolean("DarkMode", dark).apply();
        AppCompatDelegate.setDefaultNightMode(dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    // Inicializa el RecyclerView que muestra las notificaciones resumidas en la portada.
    private void setupNotificationsHome() {
        if (rvNotifHome != null) {
            rvNotifHome.setLayoutManager(new LinearLayoutManager(this));
            adapterNotifHome = new NotificacionesHomeAdapter(listaNotificaciones, this::eliminarNotificacionPersistente);
            rvNotifHome.setAdapter(adapterNotifHome);
        }
    }

    // Permite arrastrar el acceso rápido a WhatsApp sin perder la capacidad de pulsarlo.
    private void setupDraggableWhatsapp() {
        if (fabWhatsapp != null) {
            fabWhatsapp.setOnTouchListener(new View.OnTouchListener() {
                float dX, dY, startX, startY;
                final float clickThreshold = 10f;

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    View parent = (View) view.getParent();
                    if (parent == null) return false;

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getRawX();
                            startY = event.getRawY();
                            dX = view.getX() - event.getRawX();
                            dY = view.getY() - event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float newX = event.getRawX() + dX;
                            float newY = event.getRawY() + dY;

                            // Limitar movimiento dentro de los bordes del padre (pantalla)
                            int maxX = parent.getWidth() - view.getWidth();
                            int maxY = parent.getHeight() - view.getHeight();

                            newX = Math.max(0, Math.min(newX, maxX));
                            newY = Math.max(0, Math.min(newY, maxY));

                            view.setX(newX);
                            view.setY(newY);
                            break;
                        case MotionEvent.ACTION_UP:
                            float endX = event.getRawX();
                            float endY = event.getRawY();
                            double distance = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
                            if (distance < clickThreshold) {
                                abrirWhatsappAdmin();
                            } else {
                                if (fabQr != null) {
                                    float qrX = fabQr.getX() + fabQr.getWidth() / 2f;
                                    float qrY = fabQr.getY() + fabQr.getHeight() / 2f;
                                    float distToQr = (float) Math.hypot(view.getX() + view.getWidth()/2f - qrX, view.getY() + view.getHeight()/2f - qrY);
                                    if (distToQr < 150) {
                                        float bounceX = (view.getX() < qrX) ? qrX - 200 : qrX + 200;
                                        // Asegurar que el rebote no lo saque de la pantalla
                                        bounceX = Math.max(0, Math.min(bounceX, parent.getWidth() - view.getWidth()));
                                        view.animate().x(bounceX).setDuration(300).start();
                                    }
                                }
                            }
                            break;
                    }
                    return true;
                }
            });
        }
    }

    // Abre WhatsApp con el número y el mensaje definidos por la aplicación.
    private void abrirWhatsappAdmin() {
        String numero = "34600000000";
        String mensaje = getString(R.string.whatsapp_admin_msg);
        Uri uri = Uri.parse("https://api.whatsapp.com/send?phone=" + numero + "&text=" + mensaje);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    // Carga el perfil y las notificaciones del usuario conectado.
    private void cargarDatosUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        currentUserEmailSafe = user.getEmail().replace(".", "_");
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(currentUserEmailSafe);

        // Escuchamos la lista de notificaciones eliminadas para que el filtrado sea persistente por cuenta.
        ref.child("notificacionesEliminadas").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificacionesEliminadas.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    notificacionesEliminadas.add(data.getKey());
                }
                actualizarListaNotificacionesHome();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Escuchamos el nodo de notificaciones propio de este usuario.
        ref.child("notificaciones").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaNotificacionesTodas.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Notificacion n = data.getValue(Notificacion.class);
                    if (n != null) {
                        n.setId(data.getKey());
                        listaNotificacionesTodas.add(n);
                    }
                }
                actualizarListaNotificacionesHome();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Sincronizamos las notificaciones globales con el usuario solo si aún no las ha borrado.
        sincronizarNotificacionesGlobales(ref);
    }

    // Replica cada notificación global a la carpeta de este usuario respetando lo que ya haya borrado.
    private void sincronizarNotificacionesGlobales(DatabaseReference userRef) {
        FirebaseDatabase.getInstance(DB_URL).getReference(GLOBAL_NOTIFICATIONS_PATH).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Notificacion notif = data.getValue(Notificacion.class);
                    String notifId = data.getKey();
                    if (notif == null || notifId == null) continue;
                    notif.setId(notifId);

                    userRef.child("notificacionesEliminadas").child(notifId).get().addOnSuccessListener(deletedSnap -> {
                        if (deletedSnap.exists()) return;
                        userRef.child("notificaciones").child(notifId).get().addOnSuccessListener(localSnap -> {
                            if (!localSnap.exists()) {
                                userRef.child("notificaciones").child(notifId).setValue(notif);
                            }
                        });
                    });
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Cruza las notificaciones actuales con las eliminadas y actualiza la vista.
    private void actualizarListaNotificacionesHome() {
        listaNotificaciones.clear();
        for (Notificacion n : listaNotificacionesTodas) {
            if (n != null && n.getId() != null && !notificacionesEliminadas.contains(n.getId())) {
                listaNotificaciones.add(n);
            }
        }
        Collections.sort(listaNotificaciones, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        if (adapterNotifHome != null) adapterNotifHome.actualizar(listaNotificaciones);
        View tvNoNotif = findViewById(R.id.tvNoNotifHome);
        if (tvNoNotif != null) tvNoNotif.setVisibility(listaNotificaciones.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Elimina la notificación solo para esta cuenta y marca su ID como eliminado de forma persistente.
    private void eliminarNotificacionPersistente(Notificacion notif) {
        if (notif == null || notif.getId() == null || currentUserEmailSafe.isEmpty()) return;
        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(currentUserEmailSafe);
        userRef.child("notificaciones").child(notif.getId()).removeValue();
        userRef.child("notificacionesEliminadas").child(notif.getId()).setValue(true);
    }

    // Inicia una nueva sesión QR: cada apertura genera un nonce distinto y un token nuevo.
    private void iniciarGeneracionQR() {
        // Nueva sesion QR: al cerrar/abrir se invalida la anterior y cambia el contenido.
        qrSessionNonce = UUID.randomUUID().toString();
        isQrVisible = true;
        generarYMostrarQR();
        qrHandler.postDelayed(qrRunnable, QR_REFRESH_MS);
    }

    // Tarea repetitiva que refresca el QR mientras la pantalla siga abierta.
    private final Runnable qrRunnable = new Runnable() {
        @Override public void run() {
            if (isQrVisible) {
                generarYMostrarQR();
                qrHandler.postDelayed(this, QR_REFRESH_MS);
            }
        }
    };

    // Construye el código QR con los datos del perfil del usuario y lo pinta en pantalla.
    private void generarYMostrarQR() {
        ImageView imgQr = findViewById(R.id.imgQrCodePantalla);
        ProgressBar progressQr = findViewById(R.id.progressQr);
        if (imgQr != null) imgQr.animate().alpha(0.2f).setDuration(200).start();
        if (progressQr != null) progressQr.setVisibility(View.VISIBLE);

        // Si todavía no conocemos al usuario, no tiene sentido generar el QR.
        if (currentUserEmailSafe.isEmpty()) {
            if (progressQr != null) progressQr.setVisibility(View.GONE);
            return;
        }

        // Leemos TODO el perfil para incluir todos los datos del cliente en el QR.
        FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(currentUserEmailSafe).child("perfil")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        if (progressQr != null) progressQr.setVisibility(View.GONE);
                        return;
                    }

                    // Generamos metadatos temporales para que el QR sea válido solo durante un intervalo corto.
                    long issuedAt = System.currentTimeMillis();
                    long expiresAt = issuedAt + QR_TOKEN_TTL_MS;
                    String qrToken = UUID.randomUUID().toString();

                    // Empaquetamos todos los datos del perfil dentro del contenido del QR.
                    String qrData = construirPayloadQr(snapshot, qrToken, issuedAt, expiresAt);
                    guardarTokenQr(qrToken, issuedAt, expiresAt);

                    Bitmap bitmap = generarQR(qrData);
                    if (isQrVisible && imgQr != null) {
                        imgQr.setImageBitmap(bitmap);
                        imgQr.animate().alpha(1f).setDuration(400).start();
                    }
                    if (progressQr != null) progressQr.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (progressQr != null) progressQr.setVisibility(View.GONE);
                });
    }

    // Construye el texto final en JSON para incluir todos los datos del perfil y metadatos de validez.
    private String construirPayloadQr(DataSnapshot perfilSnapshot, String qrToken, long issuedAt, long expiresAt) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("token", qrToken);
            payload.put("session", qrSessionNonce);
            payload.put("issuedAt", issuedAt);
            payload.put("expiresAt", expiresAt);
            payload.put("emailSafe", currentUserEmailSafe);

            Object perfilRaw = perfilSnapshot.getValue();
            if (perfilRaw != null) {
                // Si hay estructuras anidadas en perfil, también se serializan completas.
                payload.put("perfil", new JSONObject((Map<?, ?>) perfilRaw));
            } else {
                payload.put("perfil", new JSONObject());
            }
            return payload.toString();
        } catch (JSONException | ClassCastException e) {
            // Fallback legible para escáneres simples si el JSON no pudiera construirse.
            StringBuilder fallback = new StringBuilder();
            fallback.append("token=").append(qrToken).append("\n")
                    .append("session=").append(qrSessionNonce).append("\n")
                    .append("issuedAt=").append(issuedAt).append("\n")
                    .append("expiresAt=").append(expiresAt).append("\n")
                    .append("emailSafe=").append(currentUserEmailSafe).append("\n");
            for (DataSnapshot child : perfilSnapshot.getChildren()) {
                String key = child.getKey();
                if (key == null) continue;
                Object value = child.getValue();
                fallback.append(key).append("=").append(value != null ? String.valueOf(value) : "").append("\n");
            }
            return fallback.toString().trim();
        }
    }

    /**
     * Persiste un token efimero para poder invalidarlo al cerrar QR y facilitar validaciones externas.
     */
    // Guarda el token efímero del QR para poder invalidarlo cuando el usuario cierre la pantalla.
    private void guardarTokenQr(String qrToken, long issuedAt, long expiresAt) {
        if (currentUserEmailSafe.isEmpty()) return;

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("token", qrToken);
        tokenData.put("session", qrSessionNonce);
        tokenData.put("issuedAt", issuedAt);
        tokenData.put("expiresAt", expiresAt);

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("TokensQR")
                .child(currentUserEmailSafe)
                .setValue(tokenData);
    }

    // Convierte el texto final en un bitmap de código QR.
    private Bitmap generarQR(String datos) {
        try {
            return new BarcodeEncoder().createBitmap(new MultiFormatWriter().encode(datos, BarcodeFormat.QR_CODE, 500, 500));
        } catch (Exception e) { return null; }
    }

    // Ejecuta una animación circular desde el botón pulsado y luego cambia de pantalla.
    private void lanzarAnimacionGota(View btn, Runnable accion) {
        ultimoBotonPulsado = btn;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        accion.run();
        if (fragmentContainerMask != null) {
            fragmentContainerMask.post(() -> {
                int[] btnLoc = new int[2]; btn.getLocationInWindow(btnLoc);
                int[] conLoc = new int[2]; ((View)fragmentContainerMask.getParent()).getLocationInWindow(conLoc);
                float cx = btnLoc[0] + btn.getWidth() / 2f - conLoc[0];
                float cy = btnLoc[1] + btn.getHeight() / 2f - conLoc[1];
                float radius = (float) Math.hypot(fragmentContainerMask.getWidth(), fragmentContainerMask.getHeight()) * 2f;
                fragmentContainerMask.setCenterX(cx); fragmentContainerMask.setCenterY(cy);
                fragmentContainerMask.setRevealRadius(0f);
                ValueAnimator anim = ValueAnimator.ofFloat(0f, radius).setDuration(850);
                anim.setInterpolator(new AccelerateDecelerateInterpolator());
                anim.addUpdateListener(a -> fragmentContainerMask.setRevealRadius((float) a.getAnimatedValue()));
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) {
                        fragmentContainerMask.setRevealRadius(-1f);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                });
                fragmentContainerMask.setVisibility(View.VISIBLE);
                anim.start();
            });
        }
    }

    // Cierra el fragmento activo usando una animación inversa a la de apertura.
    public void cerrarFragmentoAnimado() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        if (ultimoBotonPulsado == null) {
            getSupportFragmentManager().popBackStack();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            return;
        }
        if (fragmentContainerMask != null) {
            fragmentContainerMask.post(() -> {
                int[] btnLoc = new int[2]; ultimoBotonPulsado.getLocationInWindow(btnLoc);
                int[] conLoc = new int[2]; ((View)fragmentContainerMask.getParent()).getLocationInWindow(conLoc);
                float cx = btnLoc[0] + ultimoBotonPulsado.getWidth() / 2f - conLoc[0];
                float cy = btnLoc[1] + ultimoBotonPulsado.getHeight() / 2f - conLoc[1];
                float radius = (float) Math.hypot(fragmentContainerMask.getWidth(), fragmentContainerMask.getHeight()) * 2f;
                fragmentContainerMask.setCenterX(cx); fragmentContainerMask.setCenterY(cy);
                ValueAnimator anim = ValueAnimator.ofFloat(radius, 0f).setDuration(650);
                anim.setInterpolator(new AccelerateDecelerateInterpolator());
                anim.addUpdateListener(a -> fragmentContainerMask.setRevealRadius((float) a.getAnimatedValue()));
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) {
                        fragmentContainerMask.setVisibility(View.INVISIBLE);
                        getSupportFragmentManager().popBackStack();
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                });
                anim.start();
            });
        }
    }

    // Abre la pantalla del QR con la misma animación circular usada en los fragmentos.
    private void abrirPantallaQrConOndaDifuminada() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        if (layoutQrOverlay != null) {
            layoutQrOverlay.setVisibility(View.VISIBLE);
            layoutQrOverlay.setCenterX(fabQr.getX() + fabQr.getWidth() / 2f);
            layoutQrOverlay.setCenterY(fabQr.getY() + fabQr.getHeight() / 2f);
            float radius = (float) Math.hypot(layoutQrOverlay.getWidth(), layoutQrOverlay.getHeight()) * 2f;
            ValueAnimator anim = ValueAnimator.ofFloat(0f, radius).setDuration(850);
            anim.addUpdateListener(a -> layoutQrOverlay.setRevealRadius((float) a.getAnimatedValue()));
            anim.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    layoutQrOverlay.setRevealRadius(-1f);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            });
            anim.start();
        }
    }

    // Cierra el QR, invalida su token y limpia el estado para obligar a crear uno nuevo la próxima vez.
    private void cerrarPantallaQrConOndaDifuminada() {
        // Al cerrar, invalidamos token y sesion para forzar QR nuevo en la siguiente apertura.
        if (!currentUserEmailSafe.isEmpty()) FirebaseDatabase.getInstance(DB_URL).getReference("TokensQR").child(currentUserEmailSafe).removeValue();
        isQrVisible = false;
        qrSessionNonce = "";
        qrHandler.removeCallbacks(qrRunnable);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        if (layoutQrOverlay != null) {
            float radius = (float) Math.hypot(layoutQrOverlay.getWidth(), layoutQrOverlay.getHeight()) * 2f;
            ValueAnimator anim = ValueAnimator.ofFloat(radius, 0f).setDuration(650);
            anim.addUpdateListener(a -> layoutQrOverlay.setRevealRadius((float) a.getAnimatedValue()));
            anim.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    layoutQrOverlay.setVisibility(View.INVISIBLE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            });
            anim.start();
        }
    }

    // ADAPTADOR INTERNO PARA NOTIFICACIONES EN HOME
    public static class NotificacionesHomeAdapter extends RecyclerView.Adapter<NotificacionesHomeAdapter.ViewHolder> {
        private List<Notificacion> lista;
        private OnDeleteListener listener;

        public interface OnDeleteListener { void onDelete(Notificacion notif); }

        public NotificacionesHomeAdapter(List<Notificacion> lista, OnDeleteListener listener) {
            this.lista = lista;
            this.listener = listener;
        }

        public void actualizar(List<Notificacion> nuevaLista) {
            this.lista = nuevaLista;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notificacion_home, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Notificacion n = lista.get(position);
            TranslationHelper.translateTextView(holder.titulo, n.getTitulo());
            TranslationHelper.translateTextView(holder.texto, n.getMensaje());
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(n));
        }

        @Override public int getItemCount() { return lista.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView titulo, texto;
            ImageView btnDelete;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                titulo = itemView.findViewById(R.id.tvTituloNotifHome);
                texto = itemView.findViewById(R.id.tvTextoNotifHome);
                btnDelete = itemView.findViewById(R.id.btnDeleteNotifHome);
            }
        }
    }
}
