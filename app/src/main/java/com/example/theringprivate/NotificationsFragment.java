package com.example.theringprivate;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Adaptador que pinta cada notificación y cambia entre modo lectura y modo edición.
class NotificacionesAdapter extends RecyclerView.Adapter<NotificacionesAdapter.ViewHolder> {

    // Lista actual que se muestra en pantalla.
    private List<Notificacion> lista;
    // Acción que se ejecuta al tocar una notificación en modo normal.
    private final OnItemClickListener onItemClickListener;
    // Notifica cuando cambian las selecciones de borrado múltiple.
    private final OnSelectionChangedListener onSelectionChangedListener;
    // Controla si la lista está en modo de selección múltiple.
    public boolean isEditMode = false;
    // IDs seleccionados para eliminar en lote.
    public final Set<String> selectedIds = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(Notificacion notif);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged();
    }

    // El adaptador recibe la lista y los callbacks necesarios para abrir o seleccionar elementos.
    public NotificacionesAdapter(List<Notificacion> lista, OnItemClickListener onItemClickListener, OnSelectionChangedListener onSelectionChangedListener) {
        this.lista = lista;
        this.onItemClickListener = onItemClickListener;
        this.onSelectionChangedListener = onSelectionChangedListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View root;
        public ImageView imgIcono, imgArrow;
        public TextView tvTitulo, tvTiempo;
        public CheckBox cbNotificacion;

        // Capturamos las vistas del item para poder rellenarlas y reaccionar al toque.
        public ViewHolder(View view) {
            super(view);
            root = view;
            imgIcono = view.findViewById(R.id.imgIconoNotificacion);
            tvTitulo = view.findViewById(R.id.tvTituloNotificacion);
            tvTiempo = view.findViewById(R.id.tvTiempoNotificacion);
            cbNotificacion = view.findViewById(R.id.cbNotificacion);
            imgArrow = view.findViewById(R.id.imgArrow);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notificacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notificacion notif = lista.get(position);
        // El título se muestra tal cual llega desde el modelo.
        holder.tvTitulo.setText(notif.getTitulo());
        // Formateamos la hora para que el usuario entienda cuándo se publicó.
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        holder.tvTiempo.setText(sdf.format(new Date(notif.getTimestamp())));

        // El icono cambia según el tipo para dar pistas visuales rápidas.
        if ("mensaje".equals(notif.getTipo())) {
            holder.imgIcono.setImageResource(android.R.drawable.sym_action_chat);
            holder.imgIcono.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4287f5")));
        } else {
            holder.imgIcono.setImageResource(android.R.drawable.ic_dialog_info);
            holder.imgIcono.setImageTintList(ColorStateList.valueOf(Color.parseColor("#A30000")));
        }

        // Atenuamos ligeramente las notificaciones ya leídas para diferenciarlas.
        if (!notif.isLeida()) {
            holder.tvTitulo.setAlpha(1.0f);
            holder.root.setAlpha(1.0f);
        } else {
            holder.tvTitulo.setAlpha(0.5f);
            holder.root.setAlpha(0.7f);
        }

        // En modo edición enseñamos casillas; en modo normal enseñamos el chevrón de detalle.
        if (isEditMode) {
            holder.cbNotificacion.setVisibility(View.VISIBLE);
            holder.imgArrow.setVisibility(View.GONE);
            holder.cbNotificacion.setOnCheckedChangeListener(null);
            holder.cbNotificacion.setChecked(selectedIds.contains(notif.getId()));
            holder.cbNotificacion.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedIds.add(notif.getId());
                else selectedIds.remove(notif.getId());
                onSelectionChangedListener.onSelectionChanged();
            });
            holder.root.setOnClickListener(v -> holder.cbNotificacion.setChecked(!holder.cbNotificacion.isChecked()));
        } else {
            holder.cbNotificacion.setVisibility(View.GONE);
            holder.imgArrow.setVisibility(View.VISIBLE);
            holder.root.setOnClickListener(v -> onItemClickListener.onItemClick(notif));
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    // Sustituye la lista interna por otra nueva y refresca toda la vista.
    public void actualizarLista(List<Notificacion> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    // Marca o desmarca todas las notificaciones visibles en el filtro actual.
    public void seleccionarTodo(boolean seleccionar, List<Notificacion> listaFiltrada) {
        if (seleccionar) {
            for (Notificacion n : listaFiltrada) selectedIds.add(n.getId());
        } else {
            selectedIds.clear();
        }
        notifyDataSetChanged();
        onSelectionChangedListener.onSelectionChanged();
    }
}

// Fragmento que muestra la bandeja de notificaciones con filtros, modo edición y borrado múltiple.
public class NotificationsFragment extends Fragment {

    // URL de la base de datos usada para leer y escribir notificaciones del usuario.
    private final String DB_URL = "https://the-ring-private-default-rtdb.europe-west1.firebasedatabase.app/";
    // Referencia al nodo de notificaciones del usuario autenticado.
    private DatabaseReference database;
    // Correo transformado para usarlo como clave en la base de datos.
    private String currentUserEmailSafe = "";

    // Lista principal que muestra el historial de notificaciones.
    private RecyclerView recycler;
    // Vista que aparece cuando no hay notificaciones para mostrar.
    private LinearLayout layoutEmptyState;
    // Adaptador de la lista de notificaciones.
    private NotificacionesAdapter adapter;
    // Todo lo que llega de Firebase sin filtrar.
    private final List<Notificacion> listaNotificacionesTodas = new ArrayList<>();
    // Lista ya filtrada y ordenada para la interfaz.
    private List<Notificacion> listaNotificaciones = new ArrayList<>();
    // IDs de notificaciones eliminadas por este usuario para ocultarlas de forma persistente.
    private final Set<String> notificacionesEliminadas = new HashSet<>();
    // Filtro activo: todas, alertas o mensajes.
    private String filtroActual = "Todas";

    private ImageView btnToggleEditMode, btnDeleteSelected;
    private CheckBox cbSelectAll;
    private TextView tvTituloHeader;
    private View scrollFiltros;

    // El fragmento carga directamente el layout de notificaciones.
    public NotificationsFragment() {
        super(R.layout.fragment_notifications);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Referencias visuales principales del panel.
        btnToggleEditMode = view.findViewById(R.id.btnToggleEditMode);
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected);
        cbSelectAll = view.findViewById(R.id.cbSelectAll);
        tvTituloHeader = view.findViewById(R.id.tvTituloHeader);
        scrollFiltros = view.findViewById(R.id.scrollFiltros);

        // Botón atrás: si estamos editando, salimos de edición; si no, volvemos al contenido anterior.
        view.findViewById(R.id.btnBackNotif).setOnClickListener(v -> {
            if (adapter.isEditMode) {
                salirModoEdicion();
            } else {
                if (getActivity() instanceof HomeActivity) {
                    if (getActivity().getSupportFragmentManager().getBackStackEntryCount() > 1) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        ((HomeActivity) getActivity()).cerrarFragmentoAnimado();
                    }
                }
            }
        });

        // Alterna entre modo lectura y modo edición.
        btnToggleEditMode.setOnClickListener(v -> {
            if (!adapter.isEditMode) entrarModoEdicion();
            else salirModoEdicion();
        });

        // Borra en lote las notificaciones que el usuario haya marcado.
        btnDeleteSelected.setOnClickListener(v -> {
            if (adapter.selectedIds.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona al menos una notificación", Toast.LENGTH_SHORT).show();
            } else {
                mostrarDialogoBorrarSeleccionadas();
            }
        });

        // La casilla global marca o desmarca todas las notificaciones visibles.
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.seleccionarTodo(isChecked, obtenerListaFiltrada()));

        TextView filterTodas = view.findViewById(R.id.filterTodas);
        TextView filterAlertas = view.findViewById(R.id.filterAlertas);
        TextView filterMensajes = view.findViewById(R.id.filterMensajes);

        // Reutilizamos un único listener para actualizar el filtro visual y lógico.
        View.OnClickListener filtroClickListener = v -> {
            filterTodas.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2A2A2A")));
            filterTodas.setTextColor(Color.parseColor("#AAAAAA"));
            filterAlertas.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2A2A2A")));
            filterAlertas.setTextColor(Color.parseColor("#AAAAAA"));
            filterMensajes.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2A2A2A")));
            filterMensajes.setTextColor(Color.parseColor("#AAAAAA"));

            TextView btnActivo = (TextView) v;
            btnActivo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A30000")));
            btnActivo.setTextColor(Color.WHITE);

            if (v.getId() == R.id.filterTodas) filtroActual = "Todas";
            else if (v.getId() == R.id.filterAlertas) filtroActual = "alerta";
            else if (v.getId() == R.id.filterMensajes) filtroActual = "mensaje";

            actualizarUI();
        };

        filterTodas.setOnClickListener(filtroClickListener);
        filterAlertas.setOnClickListener(filtroClickListener);
        filterMensajes.setOnClickListener(filtroClickListener);

        // Preparamos la lista y el estado vacío.
        recycler = view.findViewById(R.id.recyclerNotificaciones);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificacionesAdapter(listaNotificaciones, this::mostrarNotificacionDetalle, this::actualizarContadorSeleccion);
        recycler.setAdapter(adapter);

        // Solo cargamos datos si realmente hay una sesión activa.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserEmailSafe = user.getEmail().replace(".", "_");
            DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(currentUserEmailSafe);
            database = userRef.child("notificaciones");

            userRef.child("notificacionesEliminadas").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    notificacionesEliminadas.clear();
                    for (DataSnapshot data : snapshot.getChildren()) {
                        notificacionesEliminadas.add(data.getKey());
                    }
                    actualizarUI();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            database.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) return;
                    listaNotificacionesTodas.clear();
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Notificacion notif = data.getValue(Notificacion.class);
                        if (notif != null) {
                            notif.setId(data.getKey());
                            listaNotificacionesTodas.add(notif);
                        }
                    }
                    actualizarUI();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    // Activa selección múltiple y oculta los filtros para centrarse en el borrado.
    private void entrarModoEdicion() {
        adapter.isEditMode = true;
        adapter.selectedIds.clear();
        adapter.notifyDataSetChanged();

        tvTituloHeader.setText("Seleccionar");
        btnToggleEditMode.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnDeleteSelected.setVisibility(View.VISIBLE);
        cbSelectAll.setVisibility(View.VISIBLE);
        cbSelectAll.setChecked(false);
        scrollFiltros.setVisibility(View.GONE);
    }

    // Devuelve la interfaz a su estado normal de lectura.
    private void salirModoEdicion() {
        adapter.isEditMode = false;
        adapter.selectedIds.clear();
        adapter.notifyDataSetChanged();

        tvTituloHeader.setText("Notificaciones");
        btnToggleEditMode.setImageResource(android.R.drawable.ic_menu_edit);
        btnDeleteSelected.setVisibility(View.GONE);
        cbSelectAll.setVisibility(View.GONE);
        scrollFiltros.setVisibility(View.VISIBLE);
    }

    // Actualiza el título superior según cuántos elementos se han marcado.
    private void actualizarContadorSeleccion() {
        int count = adapter.selectedIds.size();
        tvTituloHeader.setText(count > 0 ? count + " seleccionadas" : "Seleccionar");

        List<Notificacion> filtrada = obtenerListaFiltrada();
        boolean todosSeleccionados = count == filtrada.size() && count > 0;
        if (cbSelectAll.isChecked() != todosSeleccionados) {
            cbSelectAll.setOnCheckedChangeListener(null);
            cbSelectAll.setChecked(todosSeleccionados);
            cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.seleccionarTodo(isChecked, filtrada));
        }
    }

    // Reconstruye la lista visible teniendo en cuenta filtros y notificaciones ya eliminadas.
    private void actualizarUI() {
        listaNotificaciones.clear();
        for (Notificacion n : listaNotificacionesTodas) {
            if (n != null && n.getId() != null && !notificacionesEliminadas.contains(n.getId())) {
                listaNotificaciones.add(n);
            }
        }
        Collections.sort(listaNotificaciones, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        List<Notificacion> listaFiltrada = obtenerListaFiltrada();
        adapter.actualizarLista(listaFiltrada);
        if (listaFiltrada.isEmpty()) {
            recycler.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (adapter.isEditMode) salirModoEdicion();
        } else {
            recycler.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    // Devuelve solo las notificaciones que cumplen el filtro activo.
    private List<Notificacion> obtenerListaFiltrada() {
        if ("Todas".equals(filtroActual)) return listaNotificaciones;
        List<Notificacion> filtrada = new ArrayList<>();
        for (Notificacion n : listaNotificaciones) {
            if (filtroActual.equals(n.getTipo())) filtrada.add(n);
        }
        return filtrada;
    }

    // Abre el detalle de una notificación concreta y la marca como leída.
    private void mostrarNotificacionDetalle(Notificacion notif) {
        database.child(notif.getId()).child("leida").setValue(true);

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_dark_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvDialogMessage);
        MaterialButton btnOk = dialog.findViewById(R.id.btnConfirm);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);

        tvTitle.setText(notif.getTitulo());
        tvMsg.setText(notif.getMensaje());
        btnOk.setText("Cerrar");
        btnCancel.setVisibility(View.GONE);

        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Diálogo de confirmación para borrar varias notificaciones a la vez.
    private void mostrarDialogoBorrarSeleccionadas() {
        int count = adapter.selectedIds.size();
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_dark_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvDialogMessage);
        tvTitle.setText("Borrar Notificaciones");
        tvMsg.setText("¿Estás seguro de que quieres borrar las " + count + " notificaciones seleccionadas?");

        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            List<String> idsABorrar = new ArrayList<>(adapter.selectedIds);
            for (String id : idsABorrar) {
                database.child(id).removeValue();
                FirebaseDatabase.getInstance(DB_URL).getReference("Usuarios").child(currentUserEmailSafe).child("notificacionesEliminadas").child(id).setValue(true);
            }
            Toast.makeText(requireContext(), "Eliminadas " + count + " notificaciones", Toast.LENGTH_SHORT).show();
            salirModoEdicion();
            dialog.dismiss();
        });
        dialog.show();
    }
}
