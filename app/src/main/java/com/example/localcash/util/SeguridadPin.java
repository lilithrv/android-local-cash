package com.example.localcash.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// Protege el PIN de respaldo: no se guarda en texto plano, sino un hash con sal (salt)

public final class SeguridadPin {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2_sha256";
    private static final int ITERACIONES = 120_000;
    private static final int LONGITUD_BITS = 256;
    private static final int SAL_BYTES = 16;

    private SeguridadPin() {
    }

    // Genera el texto a guardar: "pbkdf2_sha256$iteraciones$salBase64$hashBase64".
    public static String hash(String pin) {
        byte[] sal = new byte[SAL_BYTES];
        new SecureRandom().nextBytes(sal);
        byte[] hash = pbkdf2(pin.toCharArray(), sal, ITERACIONES);
        return PREFIJO + "$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(sal) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    // Compara el PIN escrito contra el hash almacenado.
    public static boolean verificar(String pin, String almacenado) {
        if (pin == null || !esHash(almacenado)) {
            return false;
        }
        try {
            String[] partes = almacenado.split("\\$");
            // partes = [prefijo, iteraciones, sal, hash]
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = pbkdf2(pin.toCharArray(), sal, iteraciones);
            // Comparación en tiempo constante (evita filtrar información por el tiempo de respuesta).
            return MessageDigest.isEqual(esperado, calculado);
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Indica si un valor almacenado ya está hasheado con este esquema.
    public static boolean esHash(String almacenado) {
        return almacenado != null && almacenado.startsWith(PREFIJO + "$");
    }

    private static byte[] pbkdf2(char[] pin, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin, sal, iteraciones, LONGITUD_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // No debería ocurrir en API 26+; si ocurre, es un error de programación/entorno.
            throw new RuntimeException("No se pudo calcular el hash del PIN", e);
        }
    }
}
