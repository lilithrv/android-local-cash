package com.example.localcash.modelo;

// Cuenta financiera
public class Cuenta {
    private long id;
    private String nombre;
    private String tipo;
    private double saldoInicial;
    // Saldo calculado = saldoInicial + ingresos - egresos
    private double saldoActual;

    public Cuenta(long id, String nombre, String tipo, double saldoInicial) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.saldoInicial = saldoInicial;
        this.saldoActual = saldoInicial;
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public double getSaldoInicial() {
        return saldoInicial;
    }

    public double getSaldoActual() {
        return saldoActual;
    }

    public void setSaldoActual(double saldoActual) {
        this.saldoActual = saldoActual;
    }

    @Override
    public String toString() {
        return nombre + " (" + tipo + ")";
    }
}
