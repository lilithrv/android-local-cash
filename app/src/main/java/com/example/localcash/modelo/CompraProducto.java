package com.example.localcash.modelo;

// Producto dentro del carrito de una compra, con precio estimado y precio real
public class CompraProducto {
    private long id;
    private long compraId;
    private String nombre;
    private double precioEstimado;
    private double precioReal;

    public CompraProducto(long id, long compraId, String nombre,
                          double precioEstimado, double precioReal) {
        this.id = id;
        this.compraId = compraId;
        this.nombre = nombre;
        this.precioEstimado = precioEstimado;
        this.precioReal = precioReal;
    }

    public long getId() {
        return id;
    }

    public long getCompraId() {
        return compraId;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPrecioEstimado() {
        return precioEstimado;
    }

    public double getPrecioReal() {
        return precioReal;
    }

    // El precio real se edita en pantalla al marcar la compra como realizada
    public void setPrecioReal(double precioReal) {
        this.precioReal = precioReal;
    }
}
