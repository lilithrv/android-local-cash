package com.example.localcash.datos;

import com.example.localcash.database.LocalCashDBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.localcash.modelo.GastoCompartido;
import com.example.localcash.modelo.GcParticipante;
import com.example.localcash.modelo.GcProducto;

import java.util.ArrayList;

// Acceso a las tablas de gastos compartidos: cabecera, productos y participantes (deudas). Módulo 3.
public class GastoCompartidoRepositorio {

    private final LocalCashDBHelper helper;

    public GastoCompartidoRepositorio(Context context) {
        this.helper = new LocalCashDBHelper(context.getApplicationContext());
    }

    // ---- Cabecera del gasto ----

    public long crearGasto(String titulo, double montoTotal, String fecha) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.GASTO_TITULO, titulo);
        valores.put(LocalCashDBHelper.GASTO_MONTO_TOTAL, montoTotal);
        valores.put(LocalCashDBHelper.GASTO_FECHA, fecha);
        return db.insert(LocalCashDBHelper.T_GASTO, null, valores);
    }

    public ArrayList<GastoCompartido> listarGastos() {
        ArrayList<GastoCompartido> lista = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(LocalCashDBHelper.T_GASTO, null, null, null, null, null,
                LocalCashDBHelper.GASTO_ID + " DESC");
        while (c.moveToNext()) {
            lista.add(new GastoCompartido(
                    c.getLong(c.getColumnIndexOrThrow(LocalCashDBHelper.GASTO_ID)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.GASTO_TITULO)),
                    c.getDouble(c.getColumnIndexOrThrow(LocalCashDBHelper.GASTO_MONTO_TOTAL)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.GASTO_FECHA))));
        }
        c.close();
        return lista;
    }

    public GastoCompartido obtenerGasto(long gastoId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(LocalCashDBHelper.T_GASTO, null,
                LocalCashDBHelper.GASTO_ID + " = ?", new String[]{String.valueOf(gastoId)},
                null, null, null);
        GastoCompartido gasto = null;
        if (c.moveToFirst()) {
            gasto = new GastoCompartido(
                    c.getLong(c.getColumnIndexOrThrow(LocalCashDBHelper.GASTO_ID)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.GASTO_TITULO)),
                    c.getDouble(c.getColumnIndexOrThrow(LocalCashDBHelper.GASTO_MONTO_TOTAL)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.GASTO_FECHA)));
        }
        c.close();
        return gasto;
    }

    // Actualiza el monto total de la cabecera (tras agregar/quitar productos).
    public void actualizarMontoTotal(long gastoId, double montoTotal) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.GASTO_MONTO_TOTAL, montoTotal);
        db.update(LocalCashDBHelper.T_GASTO, valores,
                LocalCashDBHelper.GASTO_ID + " = ?", new String[]{String.valueOf(gastoId)});
    }

    public void eliminarGasto(long gastoId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String[] arg = {String.valueOf(gastoId)};
        db.delete(LocalCashDBHelper.T_GC_PARTICIPANTE, LocalCashDBHelper.GCPA_GASTO_ID + " = ?", arg);
        db.delete(LocalCashDBHelper.T_GC_PRODUCTO, LocalCashDBHelper.GCP_GASTO_ID + " = ?", arg);
        db.delete(LocalCashDBHelper.T_GASTO, LocalCashDBHelper.GASTO_ID + " = ?", arg);
    }

    // ---- Productos ----

    public long agregarProducto(long gastoId, String nombre, double precio) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.GCP_GASTO_ID, gastoId);
        valores.put(LocalCashDBHelper.GCP_NOMBRE, nombre);
        valores.put(LocalCashDBHelper.GCP_PRECIO, precio);
        return db.insert(LocalCashDBHelper.T_GC_PRODUCTO, null, valores);
    }

    public void eliminarProducto(long productoId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(LocalCashDBHelper.T_GC_PRODUCTO,
                LocalCashDBHelper.GCP_ID + " = ?", new String[]{String.valueOf(productoId)});
    }

    public ArrayList<GcProducto> listarProductos(long gastoId) {
        ArrayList<GcProducto> lista = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(LocalCashDBHelper.T_GC_PRODUCTO, null,
                LocalCashDBHelper.GCP_GASTO_ID + " = ?", new String[]{String.valueOf(gastoId)},
                null, null, LocalCashDBHelper.GCP_ID + " ASC");
        while (c.moveToNext()) {
            lista.add(new GcProducto(
                    c.getLong(c.getColumnIndexOrThrow(LocalCashDBHelper.GCP_ID)),
                    c.getLong(c.getColumnIndexOrThrow(LocalCashDBHelper.GCP_GASTO_ID)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.GCP_NOMBRE)),
                    c.getDouble(c.getColumnIndexOrThrow(LocalCashDBHelper.GCP_PRECIO))));
        }
        c.close();
        return lista;
    }

    // ---- Participantes (deudas) ----

    public long agregarParticipante(long gastoId, long contactoId, double montoAsignado) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.GCPA_GASTO_ID, gastoId);
        valores.put(LocalCashDBHelper.GCPA_CONTACTO_ID, contactoId);
        valores.put(LocalCashDBHelper.GCPA_MONTO_ASIGNADO, montoAsignado);
        valores.put(LocalCashDBHelper.GCPA_PAGADO, 0);
        return db.insert(LocalCashDBHelper.T_GC_PARTICIPANTE, null, valores);
    }

    // Participantes de un gasto con el nombre del contacto (JOIN).
    public ArrayList<GcParticipante> listarParticipantes(long gastoId) {
        ArrayList<GcParticipante> lista = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT p." + LocalCashDBHelper.GCPA_ID + ", p." + LocalCashDBHelper.GCPA_GASTO_ID +
                ", p." + LocalCashDBHelper.GCPA_CONTACTO_ID + ", ct." + LocalCashDBHelper.CONTACTO_NOMBRE +
                ", p." + LocalCashDBHelper.GCPA_MONTO_ASIGNADO + ", p." + LocalCashDBHelper.GCPA_PAGADO +
                " FROM " + LocalCashDBHelper.T_GC_PARTICIPANTE + " p" +
                " JOIN " + LocalCashDBHelper.T_CONTACTO + " ct" +
                " ON p." + LocalCashDBHelper.GCPA_CONTACTO_ID + " = ct." + LocalCashDBHelper.CONTACTO_ID +
                " WHERE p." + LocalCashDBHelper.GCPA_GASTO_ID + " = ?" +
                " ORDER BY ct." + LocalCashDBHelper.CONTACTO_NOMBRE + " ASC";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(gastoId)});
        while (c.moveToNext()) {
            lista.add(new GcParticipante(
                    c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3),
                    c.getDouble(4), c.getInt(5) == 1));
        }
        c.close();
        return lista;
    }

    public void eliminarParticipante(long participanteId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(LocalCashDBHelper.T_GC_PARTICIPANTE,
                LocalCashDBHelper.GCPA_ID + " = ?", new String[]{String.valueOf(participanteId)});
    }

    public void actualizarParticipante(long id, double montoAsignado, boolean pagado) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.GCPA_MONTO_ASIGNADO, montoAsignado);
        valores.put(LocalCashDBHelper.GCPA_PAGADO, pagado ? 1 : 0);
        db.update(LocalCashDBHelper.T_GC_PARTICIPANTE, valores,
                LocalCashDBHelper.GCPA_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
