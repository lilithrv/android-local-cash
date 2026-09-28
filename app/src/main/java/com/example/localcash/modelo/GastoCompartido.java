package com.example.localcash.modelo;

public class GastoCompartido {
    private long id;
    private String titulo;
    private double montoTotal;
    private String fecha;

    public GastoCompartido(long id, String titulo, double montoTotal, String fecha) {
        this.id = id;
        this.titulo = titulo;
        this.montoTotal = montoTotal;
        this.fecha = fecha;
    }

    public long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public double getMontoTotal() {
        return montoTotal;
    }

    public String getFecha() {
        return fecha;
    }

    @Override
    public String toString() {
        return titulo;
    }
}
