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

public class HomeActivity extends AppCompatActivity {

    private final String DB_URL = "https://the-ring-private-default-rtdb.europe-west1.firebasedatabase.app/";
    private final String GLOBAL_NOTIFICATIONS_PATH = "NotificacionesGlobal";
    private final String GLOBAL_NOTIFICATION_ID = "evento_quedada_01";
    private SoftRevealFrameLayout layoutQrOverlay;
    private FloatingActionButton fabQr;
    private SoftRevealFrameLayout fragmentContainerMask;
    private RecyclerView rvNotifHome;
    private NotificacionesHomeAdapter adapterNotifHome;
    private final List<Notificacion> listaNotificacionesTodas = new ArrayList<>();
    private List<Notificacion> listaNotificaciones = new ArrayList<>();
    private final Set<String> notificacionesEliminadas = new HashSet<>();
    private View fabWhatsapp;
    private View ultimoBotonPulsado = null;
    private Handler qrHandler = new Handler(Looper.getMainLooper());
    private boolean isQrVisible = false;
    private String currentUserEmailSafe = "";

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
        SharedPreferences prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DarkMode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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

        View btnAjustesTop = findViewById(R.id.btnAjustesTop);
        if (btnAjustesTop != null) btnAjustesTop.setOnClickListener(this::mostrarDesplegableAjustesHome);
        
        View btnNavPerfil = findViewById(R.id.btnNavPerfil);
        if (btnNavPerfil != null) {
            btnNavPerfil.setOnClickListener(v -> lanzarAnimacionGota(v, () -> 
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFragment()).addToBackStack(null).commit()));
        }

        View btnNavNormas = findViewById(R.id.btnNavNormas);
        if (btnNavNormas != null) {
            btnNavNormas.setOnClickListener(v -> lanzarAnimacionGota(v, () -> 
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new NormasFragment()).addToBackStack(null).commit()));
        }

        if (fabQr != null) {
            fabQr.setOnClickListener(v -> {
                abrirPantallaQrConOndaDifuminada();
                iniciarGeneracionQR();
            });
        }

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

    private void cambiarIdioma(String lang) {
        getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().putString("My_Lang", lang).apply();
        recreate();
    }

    private void cambiarTema(boolean dark) {
        getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().putBoolean("DarkMode", dark).apply();
        AppCompatDelegate.setDefaultNightMode(dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void setupNotificationsHome() {
        if (rvNotifHome != null) {
            rvNotifHome.setLayoutManager(new LinearLayoutManager(this));
            adapterNotifHome = new NotificacionesHomeAdapter(listaNotificaciones, this::eliminarNotificacionPersistente);
            rvNotifHome.setAdapter(adapterNotifHome);
        }
    }

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

    private void abrirWhatsappAdmin() {
        String numero = "34600000000";
        String mensaje = getString(R.string.whatsapp_admin_msg);
        Uri uri = Uri.parse("https://api.whatsapp.com/send?phone=" + numero + "&text=" + mensaje);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    private void cargarDatosUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        currentUserEmailSafe = user.getEmail().replace(".", "_");
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(currentUserEmailSafe);

        asegurarNotificacionGlobalPorDefecto();

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

        sincronizarNotificacionesGlobales(ref);
    }

    private void asegurarNotificacionGlobalPorDefecto() {
        DatabaseReference globalRef = FirebaseDatabase.getInstance(DB_URL).getReference(GLOBAL_NOTIFICATIONS_PATH).child(GLOBAL_NOTIFICATION_ID);
        globalRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Notificacion notif = new Notificacion(
                        GLOBAL_NOTIFICATION_ID,
                        getString(R.string.notif_evento_quedada_titulo),
                        getString(R.string.notif_evento_quedada_msg),
                        "EVENTO",
                        System.currentTimeMillis(),
                        false
                );
                globalRef.setValue(notif);
            }
        });
    }

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

    private void eliminarNotificacionPersistente(Notificacion notif) {
        if (notif == null || notif.getId() == null || currentUserEmailSafe.isEmpty()) return;
        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(currentUserEmailSafe);
        userRef.child("notificaciones").child(notif.getId()).removeValue();
        userRef.child("notificacionesEliminadas").child(notif.getId()).setValue(true);
    }

    private void iniciarGeneracionQR() {
        isQrVisible = true;
        generarYMostrarQR();
        qrHandler.postDelayed(qrRunnable, 15000);
    }

    private final Runnable qrRunnable = new Runnable() {
        @Override public void run() {
            if (isQrVisible) {
                generarYMostrarQR();
                qrHandler.postDelayed(this, 15000);
            }
        }
    };

    private void generarYMostrarQR() {
        ImageView imgQr = findViewById(R.id.imgQrCodePantalla);
        ProgressBar progressQr = findViewById(R.id.progressQr);
        if (imgQr != null) imgQr.animate().alpha(0.2f).setDuration(200).start();
        if (progressQr != null) progressQr.setVisibility(View.VISIBLE);
        
        // Obtener datos del usuario para el QR
        FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(currentUserEmailSafe).child("perfil").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String dni = snapshot.child("dni").getValue(String.class);
                String nombre = snapshot.child("nombreReal").getValue(String.class);
                
                String qrData = "DNI: " + (dni != null ? dni : "N/A") + "\nNombre: " + (nombre != null ? nombre : "N/A");
                
                Bitmap bitmap = generarQR(qrData);
                if (isQrVisible && imgQr != null) {
                    imgQr.setImageBitmap(bitmap);
                    imgQr.animate().alpha(1f).setDuration(400).start();
                    if (progressQr != null) progressQr.setVisibility(View.GONE);
                }
            }
        });
    }

    private Bitmap generarQR(String datos) {
        try {
            return new BarcodeEncoder().createBitmap(new MultiFormatWriter().encode(datos, BarcodeFormat.QR_CODE, 500, 500));
        } catch (Exception e) { return null; }
    }

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

    private void cerrarPantallaQrConOndaDifuminada() {
        if (!currentUserEmailSafe.isEmpty()) FirebaseDatabase.getInstance(DB_URL).getReference("TokensQR").child(currentUserEmailSafe).removeValue();
        isQrVisible = false;
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
