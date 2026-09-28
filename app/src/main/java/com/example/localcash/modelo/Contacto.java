package com.example.localcash.modelo;

// Contacto de la agenda local. Se vincula a compras y deudas.
public class Contacto {
    private long id;
    private String nombre;
    private String telefono;
    private String email;

    public Contacto(long id, String nombre, String telefono, String email) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
