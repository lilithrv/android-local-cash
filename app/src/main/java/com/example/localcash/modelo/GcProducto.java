package com.example.localcash.modelo;

// Producto o servicio dentro de un gasto compartido
public class GcProducto {
    private long id;
    private long gastoId;
    private String nombre;
    private double precio;

    public GcProducto(long id, long gastoId, String nombre, double precio) {
        this.id = id;
        this.gastoId = gastoId;
        this.nombre = nombre;
        this.precio = precio;
    }

    public long getId() {
        return id;
    }

    public long getGastoId() {
        return gastoId;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPrecio() {
        return precio;
    }
}
