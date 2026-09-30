package com.example.localcash.util;

import java.util.ArrayList;
import java.util.List;

// Paginador genérico reutilizable por las listas: guarda la lista completa y expone
// la "página" visible actual con navegación anterior/siguiente. Módulo transversal.
public class Paginador<T> {

    // Cantidad de ítems por página en todas las listas.
    public static final int TAM_PAGINA = 6;

    private final int tamPagina;
    private List<T> datos = new ArrayList<>();
    private int pagina = 0;

    public Paginador() {
        this(TAM_PAGINA);
    }

    public Paginador(int tamPagina) {
        this.tamPagina = tamPagina;
    }

    // Reemplaza los datos completos y ajusta la página al rango válido (conserva la página si puede)
    public void setDatos(List<T> nuevos) {
        this.datos = (nuevos != null) ? nuevos : new ArrayList<>();
        clamp();
    }

    // Sublista de la página actual
    public List<T> paginaActual() {
        if (datos.isEmpty()) {
            return new ArrayList<>();
        }
        int desde = pagina * tamPagina;
        int hasta = Math.min(desde + tamPagina, datos.size());
        return new ArrayList<>(datos.subList(desde, hasta));
    }

    public int totalPaginas() {
        if (datos.isEmpty()) {
            return 1;
        }
        return (datos.size() + tamPagina - 1) / tamPagina;
    }

    // Número de página para mostrar al usuario (base 1)
    public int paginaHumana() {
        return pagina + 1;
    }

    public boolean hayAnterior() {
        return pagina > 0;
    }

    public boolean haySiguiente() {
        return pagina < totalPaginas() - 1;
    }

    public void anterior() {
        if (hayAnterior()) {
            pagina--;
        }
    }

    public void siguiente() {
        if (haySiguiente()) {
            pagina++;
        }
    }

    private void clamp() {
        if (pagina >= totalPaginas()) {
            pagina = totalPaginas() - 1;
        }
        if (pagina < 0) {
            pagina = 0;
        }
    }
}
