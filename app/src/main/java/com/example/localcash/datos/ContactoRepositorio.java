package com.example.localcash.datos;

import com.example.localcash.database.LocalCashDBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.localcash.modelo.Contacto;

import java.util.ArrayList;

// Acceso a la tabla contacto
public class ContactoRepositorio {

    private final LocalCashDBHelper helper;

    public ContactoRepositorio(Context context) {
        this.helper = new LocalCashDBHelper(context.getApplicationContext());
    }

    public long agregar(String nombre, String telefono, String email) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.CONTACTO_NOMBRE, nombre);
        valores.put(LocalCashDBHelper.CONTACTO_TELEFONO, telefono);
        valores.put(LocalCashDBHelper.CONTACTO_EMAIL, email);
        return db.insert(LocalCashDBHelper.T_CONTACTO, null, valores);
    }

    public void actualizar(long id, String nombre, String telefono, String email) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.CONTACTO_NOMBRE, nombre);
        valores.put(LocalCashDBHelper.CONTACTO_TELEFONO, telefono);
        valores.put(LocalCashDBHelper.CONTACTO_EMAIL, email);
        db.update(LocalCashDBHelper.T_CONTACTO, valores,
                LocalCashDBHelper.CONTACTO_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void eliminar(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(LocalCashDBHelper.T_CONTACTO,
                LocalCashDBHelper.CONTACTO_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public ArrayList<Contacto> listar() {
        ArrayList<Contacto> lista = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(LocalCashDBHelper.T_CONTACTO, null, null, null, null, null,
                LocalCashDBHelper.CONTACTO_NOMBRE + " ASC");
        while (c.moveToNext()) {
            lista.add(new Contacto(
                    c.getLong(c.getColumnIndexOrThrow(LocalCashDBHelper.CONTACTO_ID)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.CONTACTO_NOMBRE)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.CONTACTO_TELEFONO)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.CONTACTO_EMAIL))));
        }
        c.close();
        return lista;
    }
}
