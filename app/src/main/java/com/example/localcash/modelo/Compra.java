package com.example.localcash.modelo;

// Compra realizada o agendada, opcionalmente enlazada a un contacto
public class Compra {
    public static final String ESTADO_REALIZADA = "realizada";
    public static final String ESTADO_AGENDADA = "agendada";

    private long id;
    private String titulo;
    private double monto;
    private String fecha;
    private String estado;
    private long contactoId;        // 0 si no está enlazada a un contacto
    private String contactoNombre;  // se llena por join para mostrar en la lista

    public Compra(long id, String titulo, double monto, String fecha, String estado,
                  long contactoId, String contactoNombre) {
        this.id = id;
        this.titulo = titulo;
        this.monto = monto;
        this.fecha = fecha;
        this.estado = estado;
        this.contactoId = contactoId;
        this.contactoNombre = contactoNombre;
    }

    public long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public double getMonto() {
        return monto;
    }

    public String getFecha() {
        return fecha;
    }

    public String getEstado() {
        return estado;
    }

    public long getContactoId() {
        return contactoId;
    }

    public String getContactoNombre() {
        return contactoNombre;
    }

    public boolean esAgendada() {
        return ESTADO_AGENDADA.equals(estado);
    }
}
