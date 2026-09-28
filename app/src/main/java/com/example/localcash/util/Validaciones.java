package com.example.localcash.util;

import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;

public class Validaciones {

    // Devuelve true si el campo está vacío
    public static boolean esVacio(EditText campo) {
        return campo.getText().toString().trim().isEmpty();
    }

    public static boolean esEmailInvalido(EditText campo) {
        String email = campo.getText().toString().trim();
        return !Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Convierte el texto de un EditText a double; devuelve 0 si está vacío o mal escrito
    public static double aDouble(EditText campo) {
        String texto = campo.getText().toString().trim();
        if (TextUtils.isEmpty(texto)) {
            return 0;
        }
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
