package com.example.theringprivate;

// Modelo de datos que representa una notificación guardada en Firebase.
public class Notificacion {
    // Identificador único de la notificación dentro de la base de datos.
    private String id;
    // Título breve que verá el usuario en la lista de notificaciones.
    private String titulo;
    // Mensaje completo que se mostrará al abrir el detalle.
    private String mensaje;
    // Tipo lógico de la notificación para poder filtrarla o pintarla con iconos distintos.
    private String tipo;
    // Marca temporal que permite ordenar las notificaciones por antigüedad.
    private long timestamp;
    // Estado que indica si el usuario ya abrió o revisó la notificación.
    private boolean leida;

    // Constructor vacío exigido por Firebase para poder reconstruir el objeto desde la base de datos.
    public Notificacion() {
        // No se inicializa nada aquí porque Firebase rellenará los campos automáticamente.
    }

    // Constructor completo para crear una notificación desde código con todos sus valores.
    public Notificacion(String id, String titulo, String mensaje, String tipo, long timestamp, boolean leida) {
        this.id = id;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.timestamp = timestamp;
        this.leida = leida;
    }

    // Devuelve el identificador de la notificación.
    public String getId() { return id; }
    // Guarda el identificador de la notificación.
    public void setId(String id) { this.id = id; }
    // Devuelve el título de la notificación.
    public String getTitulo() { return titulo; }
    // Guarda el título de la notificación.
    public void setTitulo(String titulo) { this.titulo = titulo; }
    // Devuelve el mensaje completo de la notificación.
    public String getMensaje() { return mensaje; }
    // Guarda el mensaje completo de la notificación.
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    // Devuelve el tipo de la notificación para filtrado o presentación.
    public String getTipo() { return tipo; }
    // Guarda el tipo de la notificación.
    public void setTipo(String tipo) { this.tipo = tipo; }
    // Devuelve el momento en que se creó o publicó la notificación.
    public long getTimestamp() { return timestamp; }
    // Guarda el momento temporal asociado a la notificación.
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    // Indica si la notificación ya fue leída.
    public boolean isLeida() { return leida; }
    // Marca la notificación como leída o no leída.
    public void setLeida(boolean leida) { this.leida = leida; }
}