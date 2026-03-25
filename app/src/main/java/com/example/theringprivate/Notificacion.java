package com.example.theringprivate;

public class Notificacion {
    private String id;
    private String titulo;
    private String mensaje;
    private String tipo;
    private long timestamp;
    private boolean leida;

    public Notificacion() {
        // Constructor vacío para Firebase
    }

    public Notificacion(String id, String titulo, String mensaje, String tipo, long timestamp, boolean leida) {
        this.id = id;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.timestamp = timestamp;
        this.leida = leida;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
}