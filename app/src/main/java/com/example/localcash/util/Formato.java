package com.example.localcash.util;

import java.util.Locale;

// Formato regional de la app. Se usa el locale de Chile (es-CL) para que el separador
// de miles sea punto (500.000) en todos los montos, sin depender del idioma del dispositivo.
public class Formato {

    public static final Locale CL = new Locale("es", "CL");

    private Formato() {
    }
}
