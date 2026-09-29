package com.example.localcash.datos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.localcash.modelo.Perfil;

// Acceso a la tabla perfil
public class PerfilRepositorio {

    private final LocalCashDBHelper helper;

    public PerfilRepositorio(Context context) {
        this.helper = new LocalCashDBHelper(context.getApplicationContext());
    }

    public boolean existePerfil() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + LocalCashDBHelper.T_PERFIL, null);
        int total = 0;
        if (c.moveToFirst()) {
            total = c.getInt(0);
        }
        c.close();
        return total > 0;
    }

    // Crear perfil local en el primer uso. Devuelve el id generado.
    public long crearPerfil(String nombre, String pinRespaldo, boolean biometriaActiva) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.PERFIL_NOMBRE, nombre);
        valores.put(LocalCashDBHelper.PERFIL_PIN, pinRespaldo);
        valores.put(LocalCashDBHelper.PERFIL_BIOMETRIA, biometriaActiva ? 1 : 0);
        return db.insert(LocalCashDBHelper.T_PERFIL, null, valores);
    }

    // Devuelve el perfil guardado o null si aún no existe
    public Perfil obtenerPerfil() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(LocalCashDBHelper.T_PERFIL, null, null, null, null, null,
                LocalCashDBHelper.PERFIL_ID + " ASC", "1");
        Perfil perfil = null;
        if (c.moveToFirst()) {
            perfil = new Perfil(
                    c.getLong(c.getColumnIndexOrThrow(LocalCashDBHelper.PERFIL_ID)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.PERFIL_NOMBRE)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.PERFIL_PIN)),
                    c.getInt(c.getColumnIndexOrThrow(LocalCashDBHelper.PERFIL_BIOMETRIA)) == 1);
        }
        c.close();
        return perfil;
    }

    // Validar PIN respaldo (cuando la biometría no está disponible)
    public boolean validarPin(String pin) {
        Perfil perfil = obtenerPerfil();
        return perfil != null && perfil.getPinRespaldo() != null
                && perfil.getPinRespaldo().equals(pin);
    }
}
