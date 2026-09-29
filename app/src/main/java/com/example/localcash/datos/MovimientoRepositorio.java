package com.example.localcash.datos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.localcash.modelo.Movimiento;

import java.util.ArrayList;

// Acceso a la tabla movimiento
public class MovimientoRepositorio {

    private final LocalCashDBHelper helper;

    public MovimientoRepositorio(Context context) {
        this.helper = new LocalCashDBHelper(context.getApplicationContext());
    }

    public long agregar(long cuentaId, String tipo, double monto, String descripcion,
                        String fecha, double lat, double lon, boolean tieneGeo) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.MOV_CUENTA_ID, cuentaId);
        valores.put(LocalCashDBHelper.MOV_TIPO, tipo);
        valores.put(LocalCashDBHelper.MOV_MONTO, monto);
        valores.put(LocalCashDBHelper.MOV_DESCRIPCION, descripcion);
        valores.put(LocalCashDBHelper.MOV_FECHA, fecha);
        valores.put(LocalCashDBHelper.MOV_TIENE_GEO, tieneGeo ? 1 : 0);
        if (tieneGeo) {
            valores.put(LocalCashDBHelper.MOV_LAT, lat);
            valores.put(LocalCashDBHelper.MOV_LON, lon);
        }
        return db.insert(LocalCashDBHelper.T_MOVIMIENTO, null, valores);
    }

    // Actualiza un movimiento existente
    public void actualizar(long id, long cuentaId, String tipo, double monto, String descripcion,
                           double lat, double lon, boolean tieneGeo) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.MOV_CUENTA_ID, cuentaId);
        valores.put(LocalCashDBHelper.MOV_TIPO, tipo);
        valores.put(LocalCashDBHelper.MOV_MONTO, monto);
        valores.put(LocalCashDBHelper.MOV_DESCRIPCION, descripcion);
        valores.put(LocalCashDBHelper.MOV_TIENE_GEO, tieneGeo ? 1 : 0);
        if (tieneGeo) {
            valores.put(LocalCashDBHelper.MOV_LAT, lat);
            valores.put(LocalCashDBHelper.MOV_LON, lon);
        } else {
            valores.putNull(LocalCashDBHelper.MOV_LAT);
            valores.putNull(LocalCashDBHelper.MOV_LON);
        }
        db.update(LocalCashDBHelper.T_MOVIMIENTO, valores,
                LocalCashDBHelper.MOV_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void eliminar(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(LocalCashDBHelper.T_MOVIMIENTO,
                LocalCashDBHelper.MOV_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // Movimientos de una cuenta, del más reciente al más antiguo
    public ArrayList<Movimiento> listarPorCuenta(long cuentaId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(LocalCashDBHelper.T_MOVIMIENTO, null,
                LocalCashDBHelper.MOV_CUENTA_ID + " = ?",
                new String[]{String.valueOf(cuentaId)},
                null, null, LocalCashDBHelper.MOV_ID + " DESC");
        return leerLista(c);
    }

    // Movimientos con geolocalización (para pintar marcadores en el mapa)
    public ArrayList<Movimiento> listarConGeo() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(LocalCashDBHelper.T_MOVIMIENTO, null,
                LocalCashDBHelper.MOV_TIENE_GEO + " = 1",
                null, null, null, LocalCashDBHelper.MOV_ID + " DESC");
        return leerLista(c);
    }

    private ArrayList<Movimiento> leerLista(Cursor c) {
        ArrayList<Movimiento> lista = new ArrayList<>();
        while (c.moveToNext()) {
            lista.add(new Movimiento(
                    c.getLong(c.getColumnIndexOrThrow(LocalCashDBHelper.MOV_ID)),
                    c.getLong(c.getColumnIndexOrThrow(LocalCashDBHelper.MOV_CUENTA_ID)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.MOV_TIPO)),
                    c.getDouble(c.getColumnIndexOrThrow(LocalCashDBHelper.MOV_MONTO)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.MOV_DESCRIPCION)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.MOV_FECHA)),
                    c.getDouble(c.getColumnIndexOrThrow(LocalCashDBHelper.MOV_LAT)),
                    c.getDouble(c.getColumnIndexOrThrow(LocalCashDBHelper.MOV_LON)),
                    c.getInt(c.getColumnIndexOrThrow(LocalCashDBHelper.MOV_TIENE_GEO)) == 1));
        }
        c.close();
        return lista;
    }
}
