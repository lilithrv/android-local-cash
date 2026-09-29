package com.example.localcash.datos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.localcash.modelo.Compra;

import java.util.ArrayList;

// Acceso a la tabla compra (historial y compras agendadas, enlazadas a un contacto)
public class CompraRepositorio {

    private final LocalCashDBHelper helper;

    public CompraRepositorio(Context context) {
        this.helper = new LocalCashDBHelper(context.getApplicationContext());
    }

    public long agregar(String titulo, double monto, String fecha, String estado, long contactoId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.COMPRA_TITULO, titulo);
        valores.put(LocalCashDBHelper.COMPRA_MONTO, monto);
        valores.put(LocalCashDBHelper.COMPRA_FECHA, fecha);
        valores.put(LocalCashDBHelper.COMPRA_ESTADO, estado);
        if (contactoId > 0) {
            valores.put(LocalCashDBHelper.COMPRA_CONTACTO_ID, contactoId);
        }
        return db.insert(LocalCashDBHelper.T_COMPRA, null, valores);
    }

    public void actualizar(long id, String titulo, double monto, String estado, long contactoId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.COMPRA_TITULO, titulo);
        valores.put(LocalCashDBHelper.COMPRA_MONTO, monto);
        valores.put(LocalCashDBHelper.COMPRA_ESTADO, estado);
        // Guardar 0 como NULL para reflejar "sin contacto" igual que en agregar()
        if (contactoId > 0) {
            valores.put(LocalCashDBHelper.COMPRA_CONTACTO_ID, contactoId);
        } else {
            valores.putNull(LocalCashDBHelper.COMPRA_CONTACTO_ID);
        }
        db.update(LocalCashDBHelper.T_COMPRA, valores,
                LocalCashDBHelper.COMPRA_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void eliminar(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(LocalCashDBHelper.T_COMPRA,
                LocalCashDBHelper.COMPRA_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // Lisar compras - nombre contacto
    public ArrayList<Compra> listar() {
        ArrayList<Compra> lista = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT c." + LocalCashDBHelper.COMPRA_ID + ", c." + LocalCashDBHelper.COMPRA_TITULO +
                ", c." + LocalCashDBHelper.COMPRA_MONTO + ", c." + LocalCashDBHelper.COMPRA_FECHA +
                ", c." + LocalCashDBHelper.COMPRA_ESTADO + ", c." + LocalCashDBHelper.COMPRA_CONTACTO_ID +
                ", ct." + LocalCashDBHelper.CONTACTO_NOMBRE +
                " FROM " + LocalCashDBHelper.T_COMPRA + " c" +
                " LEFT JOIN " + LocalCashDBHelper.T_CONTACTO + " ct" +
                " ON c." + LocalCashDBHelper.COMPRA_CONTACTO_ID + " = ct." + LocalCashDBHelper.CONTACTO_ID +
                " ORDER BY c." + LocalCashDBHelper.COMPRA_ID + " DESC";
        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            long contactoId = c.isNull(5) ? 0 : c.getLong(5);
            String contactoNombre = c.isNull(6) ? "" : c.getString(6);
            lista.add(new Compra(
                    c.getLong(0), c.getString(1), c.getDouble(2), c.getString(3),
                    c.getString(4), contactoId, contactoNombre));
        }
        c.close();
        return lista;
    }
}
