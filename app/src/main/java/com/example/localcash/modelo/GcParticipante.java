package com.example.localcash.modelo;

// Participante de un gasto compartido y el monto (deuda) que le corresponde
public class GcParticipante {
    private long id;
    private long gastoId;
    private long contactoId;
    private String contactoNombre;
    private double montoAsignado;
    private boolean pagado;

    public GcParticipante(long id, long gastoId, long contactoId, String contactoNombre,
                          double montoAsignado, boolean pagado) {
        this.id = id;
        this.gastoId = gastoId;
        this.contactoId = contactoId;
        this.contactoNombre = contactoNombre;
        this.montoAsignado = montoAsignado;
        this.pagado = pagado;
    }

    public long getId() {
        return id;
    }

    public long getGastoId() {
        return gastoId;
    }

    public long getContactoId() {
        return contactoId;
    }

    public String getContactoNombre() {
        return contactoNombre;
    }

    public double getMontoAsignado() {
        return montoAsignado;
    }

    public void setMontoAsignado(double montoAsignado) {
        this.montoAsignado = montoAsignado;
    }

    public boolean isPagado() {
        return pagado;
    }

    public void setPagado(boolean pagado) {
        this.pagado = pagado;
    }
}
