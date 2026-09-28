package com.example.localcash.modelo;

// Perfil local, se crea en el primer uso
public class Perfil {
    private long id;
    private String nombre;
    private String pinRespaldo;
    private boolean biometriaActiva;

    public Perfil(long id, String nombre, String pinRespaldo, boolean biometriaActiva) {
        this.id = id;
        this.nombre = nombre;
        this.pinRespaldo = pinRespaldo;
        this.biometriaActiva = biometriaActiva;
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPinRespaldo() {
        return pinRespaldo;
    }

    public boolean isBiometriaActiva() {
        return biometriaActiva;
    }
}
