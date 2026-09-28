package com.example.localcash.modelo;

// Movimiento (ingreso o egreso) de una cuenta, con geolocalización opcional
public class Movimiento {
    public static final String TIPO_INGRESO = "ingreso";
    public static final String TIPO_EGRESO = "egreso";

    private long id;
    private long cuentaId;
    private String tipo;
    private double monto;
    private String descripcion;
    private String fecha;
    private double lat;
    private double lon;
    private boolean tieneGeo;

    public Movimiento(long id, long cuentaId, String tipo, double monto, String descripcion,
                      String fecha, double lat, double lon, boolean tieneGeo) {
        this.id = id;
        this.cuentaId = cuentaId;
        this.tipo = tipo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.lat = lat;
        this.lon = lon;
        this.tieneGeo = tieneGeo;
    }

    public long getId() {
        return id;
    }

    public long getCuentaId() {
        return cuentaId;
    }

    public String getTipo() {
        return tipo;
    }

    public double getMonto() {
        return monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getFecha() {
        return fecha;
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

    public boolean esIngreso() {
        return TIPO_INGRESO.equals(tipo);
    }
}
