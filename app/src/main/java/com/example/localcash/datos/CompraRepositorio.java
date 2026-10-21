package com.example.localcash.datos;

import com.example.localcash.database.LocalCashDBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.localcash.modelo.Compra;
import com.example.localcash.modelo.CompraProducto;

import java.util.ArrayList;

// Acceso a las tablas de compras
public class CompraRepositorio {

    private final LocalCashDBHelper helper;

    public CompraRepositorio(Context context) {
        this.helper = new LocalCashDBHelper(context.getApplicationContext());
    }

    // Cabecera de la compra
    public long crearCompra(String titulo, double montoEstimado, double montoReal, String fecha,
                            String estado, long contactoId,
                            String lugar, double lat, double lon, boolean tieneGeo) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.COMPRA_TITULO, titulo);
        valores.put(LocalCashDBHelper.COMPRA_MONTO_ESTIMADO, montoEstimado);
        valores.put(LocalCashDBHelper.COMPRA_MONTO_REAL, montoReal);
        valores.put(LocalCashDBHelper.COMPRA_FECHA, fecha);
        valores.put(LocalCashDBHelper.COMPRA_ESTADO, estado);
        ponerContacto(valores, contactoId);
        ponerLugar(valores, lugar, lat, lon, tieneGeo);
        return db.insert(LocalCashDBHelper.T_COMPRA, null, valores);
    }

    public void actualizarCompra(long id, String titulo, double montoEstimado, double montoReal,
                                 String estado, long contactoId,
                                 String lugar, double lat, double lon, boolean tieneGeo) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.COMPRA_TITULO, titulo);
        valores.put(LocalCashDBHelper.COMPRA_MONTO_ESTIMADO, montoEstimado);
        valores.put(LocalCashDBHelper.COMPRA_MONTO_REAL, montoReal);
        valores.put(LocalCashDBHelper.COMPRA_ESTADO, estado);
        ponerContacto(valores, contactoId);
        ponerLugar(valores, lugar, lat, lon, tieneGeo);
        db.update(LocalCashDBHelper.T_COMPRA, valores,
                LocalCashDBHelper.COMPRA_ID + " = ?", new String[]{String.valueOf(id)});
    }

    private void ponerContacto(ContentValues valores, long contactoId) {
        if (contactoId > 0) {
            valores.put(LocalCashDBHelper.COMPRA_CONTACTO_ID, contactoId);
        } else {
            valores.putNull(LocalCashDBHelper.COMPRA_CONTACTO_ID);
        }
    }

    private void ponerLugar(ContentValues valores, String lugar, double lat, double lon, boolean tieneGeo) {
        valores.put(LocalCashDBHelper.COMPRA_LUGAR, lugar != null ? lugar : "");
        valores.put(LocalCashDBHelper.COMPRA_LAT, lat);
        valores.put(LocalCashDBHelper.COMPRA_LON, lon);
        valores.put(LocalCashDBHelper.COMPRA_TIENE_GEO, tieneGeo ? 1 : 0);
    }

    public void eliminar(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String[] arg = {String.valueOf(id)};
        // Primero los productos del carrito, luego la cabecera
        db.delete(LocalCashDBHelper.T_COMPRA_PRODUCTO, LocalCashDBHelper.CPR_COMPRA_ID + " = ?", arg);
        db.delete(LocalCashDBHelper.T_COMPRA, LocalCashDBHelper.COMPRA_ID + " = ?", arg);
    }

    private String sqlSelectCompra() {
        return "SELECT c." + LocalCashDBHelper.COMPRA_ID + ", c." + LocalCashDBHelper.COMPRA_TITULO +
                ", c." + LocalCashDBHelper.COMPRA_MONTO_ESTIMADO + ", c." + LocalCashDBHelper.COMPRA_MONTO_REAL +
                ", c." + LocalCashDBHelper.COMPRA_FECHA + ", c." + LocalCashDBHelper.COMPRA_ESTADO +
                ", c." + LocalCashDBHelper.COMPRA_CONTACTO_ID + ", ct." + LocalCashDBHelper.CONTACTO_NOMBRE +
                ", c." + LocalCashDBHelper.COMPRA_LUGAR + ", c." + LocalCashDBHelper.COMPRA_LAT +
                ", c." + LocalCashDBHelper.COMPRA_LON + ", c." + LocalCashDBHelper.COMPRA_TIENE_GEO +
                " FROM " + LocalCashDBHelper.T_COMPRA + " c" +
                " LEFT JOIN " + LocalCashDBHelper.T_CONTACTO + " ct" +
                " ON c." + LocalCashDBHelper.COMPRA_CONTACTO_ID + " = ct." + LocalCashDBHelper.CONTACTO_ID;
    }

    private Compra leerCompra(Cursor c) {
        long contactoId = c.isNull(6) ? 0 : c.getLong(6);
        String contactoNombre = c.isNull(7) ? "" : c.getString(7);
        String lugar = c.isNull(8) ? "" : c.getString(8);
        return new Compra(
                c.getLong(0), c.getString(1), c.getDouble(2), c.getDouble(3), c.getString(4),
                c.getString(5), contactoId, contactoNombre,
                lugar, c.getDouble(9), c.getDouble(10), c.getInt(11) == 1);
    }

    public ArrayList<Compra> listar() {
        ArrayList<Compra> lista = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery(sqlSelectCompra() +
                " ORDER BY c." + LocalCashDBHelper.COMPRA_ID + " DESC", null);
        while (c.moveToNext()) {
            lista.add(leerCompra(c));
        }
        c.close();
        return lista;
    }

    // Compras con ubicación fijada
    public ArrayList<Compra> listarConGeo() {
        ArrayList<Compra> lista = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery(sqlSelectCompra() +
                " WHERE c." + LocalCashDBHelper.COMPRA_TIENE_GEO + " = 1" +
                " ORDER BY c." + LocalCashDBHelper.COMPRA_ID + " DESC", null);
        while (c.moveToNext()) {
            lista.add(leerCompra(c));
        }
        c.close();
        return lista;
    }

    public Compra obtenerCompra(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery(sqlSelectCompra() +
                " WHERE c." + LocalCashDBHelper.COMPRA_ID + " = ?", new String[]{String.valueOf(id)});
        Compra compra = null;
        if (c.moveToFirst()) {
            compra = leerCompra(c);
        }
        c.close();
        return compra;
    }

    // Productos del carrito

    public long agregarProducto(long compraId, String nombre, double precioEstimado, double precioReal) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.CPR_COMPRA_ID, compraId);
        valores.put(LocalCashDBHelper.CPR_NOMBRE, nombre);
        valores.put(LocalCashDBHelper.CPR_PRECIO_ESTIMADO, precioEstimado);
        valores.put(LocalCashDBHelper.CPR_PRECIO_REAL, precioReal);
        return db.insert(LocalCashDBHelper.T_COMPRA_PRODUCTO, null, valores);
    }

    public ArrayList<CompraProducto> listarProductos(long compraId) {
        ArrayList<CompraProducto> lista = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(LocalCashDBHelper.T_COMPRA_PRODUCTO, null,
                LocalCashDBHelper.CPR_COMPRA_ID + " = ?", new String[]{String.valueOf(compraId)},
                null, null, LocalCashDBHelper.CPR_ID + " ASC");
        while (c.moveToNext()) {
            lista.add(new CompraProducto(
                    c.getLong(c.getColumnIndexOrThrow(LocalCashDBHelper.CPR_ID)),
                    c.getLong(c.getColumnIndexOrThrow(LocalCashDBHelper.CPR_COMPRA_ID)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.CPR_NOMBRE)),
                    c.getDouble(c.getColumnIndexOrThrow(LocalCashDBHelper.CPR_PRECIO_ESTIMADO)),
                    c.getDouble(c.getColumnIndexOrThrow(LocalCashDBHelper.CPR_PRECIO_REAL))));
        }
        c.close();
        return lista;
    }

    // Borra todos los productos de una compra (para regrabar el carrito al guardar)
    public void eliminarProductosDe(long compraId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(LocalCashDBHelper.T_COMPRA_PRODUCTO,
                LocalCashDBHelper.CPR_COMPRA_ID + " = ?", new String[]{String.valueOf(compraId)});
    }
}
