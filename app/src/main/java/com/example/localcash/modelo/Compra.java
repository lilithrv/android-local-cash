package com.example.localcash.modelo;

// Compra tipo carrito
public class Compra {
    public static final String ESTADO_REALIZADA = "realizada";
    public static final String ESTADO_AGENDADA = "agendada";

    private long id;
    private String titulo;
    private double montoEstimado;
    private double montoReal;
    private String fecha;
    private String estado;
    private long contactoId;        // 0 si no está enlazada a un contacto
    private String contactoNombre;  // se llena por join para mostrar en la lista
    private String lugarNombre;     // nombre del lugar (manual o geocodificado); "" si no hay
    private double lat;
    private double lon;
    private boolean tieneGeo;       // true si tiene coordenadas en el mapa
    private long cuentaId;          // 0 si no tiene cuenta asociada

    public Compra(long id, String titulo, double montoEstimado, double montoReal, String fecha,
                  String estado, long contactoId, String contactoNombre,
                  String lugarNombre, double lat, double lon, boolean tieneGeo, long cuentaId) {
        this.id = id;
        this.titulo = titulo;
        this.montoEstimado = montoEstimado;
        this.montoReal = montoReal;
        this.fecha = fecha;
        this.estado = estado;
        this.contactoId = contactoId;
        this.contactoNombre = contactoNombre;
        this.lugarNombre = lugarNombre;
        this.lat = lat;
        this.lon = lon;
        this.tieneGeo = tieneGeo;
        this.cuentaId = cuentaId;
    }

    public long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public double getMontoEstimado() {
        return montoEstimado;
    }

    public double getMontoReal() {
        return montoReal;
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

    public String getLugarNombre() {
        return lugarNombre;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public boolean isTieneGeo() {
        return tieneGeo;
    }

    public long getCuentaId() {
        return cuentaId;
    }

    public boolean esAgendada() {
        return ESTADO_AGENDADA.equals(estado);
    }

    public boolean tieneLugar() {
        return (lugarNombre != null && !lugarNombre.isEmpty()) || tieneGeo;
    }

    // Monto a mostrar en la lista: el real si ya se realizó, el estimado si sigue agendada
    public double getMontoVigente() {
        return esAgendada() ? montoEstimado : montoReal;
    }
}
