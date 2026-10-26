package com.example.localcash.datos;

import com.example.localcash.database.LocalCashDBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.localcash.modelo.Compra;
import com.example.localcash.modelo.CompraProducto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

// Acceso a las tablas de compras
public class CompraRepositorio {

    private final LocalCashDBHelper helper;

    public CompraRepositorio(Context context) {
        this.helper = new LocalCashDBHelper(context.getApplicationContext());
    }

    // Cabecera de la compra
    public long crearCompra(String titulo, double montoEstimado, double montoReal, String fecha,
                            String estado, long contactoId,
                            String lugar, double lat, double lon, boolean tieneGeo, long cuentaId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.COMPRA_TITULO, titulo);
        valores.put(LocalCashDBHelper.COMPRA_MONTO_ESTIMADO, montoEstimado);
        valores.put(LocalCashDBHelper.COMPRA_MONTO_REAL, montoReal);
        valores.put(LocalCashDBHelper.COMPRA_FECHA, fecha);
        valores.put(LocalCashDBHelper.COMPRA_ESTADO, estado);
        ponerContacto(valores, contactoId);
        ponerLugar(valores, lugar, lat, lon, tieneGeo);
        ponerCuenta(valores, cuentaId);
        long id = db.insert(LocalCashDBHelper.T_COMPRA, null, valores);
        // Si la compra ya nace realizada, genera el egreso en la cuenta elegida
        sincronizarEgreso(db, id, titulo, estado, montoReal, cuentaId, lat, lon, tieneGeo);
        return id;
    }

    public void actualizarCompra(long id, String titulo, double montoEstimado, double montoReal,
                                 String estado, long contactoId,
                                 String lugar, double lat, double lon, boolean tieneGeo, long cuentaId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.COMPRA_TITULO, titulo);
        valores.put(LocalCashDBHelper.COMPRA_MONTO_ESTIMADO, montoEstimado);
        valores.put(LocalCashDBHelper.COMPRA_MONTO_REAL, montoReal);
        valores.put(LocalCashDBHelper.COMPRA_ESTADO, estado);
        ponerContacto(valores, contactoId);
        ponerLugar(valores, lugar, lat, lon, tieneGeo);
        ponerCuenta(valores, cuentaId);
        db.update(LocalCashDBHelper.T_COMPRA, valores,
                LocalCashDBHelper.COMPRA_ID + " = ?", new String[]{String.valueOf(id)});
        // Crea, actualiza o elimina el egreso según el estado y la cuenta vigentes
        sincronizarEgreso(db, id, titulo, estado, montoReal, cuentaId, lat, lon, tieneGeo);
    }

    private void sincronizarEgreso(SQLiteDatabase db, long compraId, String titulo,
                                   String estado, double montoReal, long cuentaId,
                                   double lat, double lon, boolean tieneGeo) {
        long movId = movimientoDe(db, compraId);
        boolean corresponde = Compra.ESTADO_REALIZADA.equals(estado) && cuentaId > 0 && montoReal > 0;

        if (corresponde) {
            ContentValues mov = new ContentValues();
            mov.put(LocalCashDBHelper.MOV_CUENTA_ID, cuentaId);
            mov.put(LocalCashDBHelper.MOV_TIPO, "egreso");
            mov.put(LocalCashDBHelper.MOV_MONTO, montoReal);
            mov.put(LocalCashDBHelper.MOV_DESCRIPCION, "Compra: " + titulo);
            // El egreso hereda la ubicación de la compra (para que figure como geolocalizado y en el mapa)
            mov.put(LocalCashDBHelper.MOV_TIENE_GEO, tieneGeo ? 1 : 0);
            if (tieneGeo) {
                mov.put(LocalCashDBHelper.MOV_LAT, lat);
                mov.put(LocalCashDBHelper.MOV_LON, lon);
            } else {
                mov.putNull(LocalCashDBHelper.MOV_LAT);
                mov.putNull(LocalCashDBHelper.MOV_LON);
            }
            if (movId > 0) {
                db.update(LocalCashDBHelper.T_MOVIMIENTO, mov,
                        LocalCashDBHelper.MOV_ID + " = ?", new String[]{String.valueOf(movId)});
            } else {
                mov.put(LocalCashDBHelper.MOV_FECHA, hoy());
                long nuevoMov = db.insert(LocalCashDBHelper.T_MOVIMIENTO, null, mov);
                guardarMovimiento(db, compraId, nuevoMov);
            }
        } else if (movId > 0) {
            db.delete(LocalCashDBHelper.T_MOVIMIENTO,
                    LocalCashDBHelper.MOV_ID + " = ?", new String[]{String.valueOf(movId)});
            guardarMovimiento(db, compraId, 0);
        }
    }

    // Id del movimiento enlazado a la compra (0 si no tiene)
    private long movimientoDe(SQLiteDatabase db, long compraId) {
        Cursor c = db.query(LocalCashDBHelper.T_COMPRA,
                new String[]{LocalCashDBHelper.COMPRA_MOVIMIENTO_ID},
                LocalCashDBHelper.COMPRA_ID + " = ?", new String[]{String.valueOf(compraId)},
                null, null, null);
        long movId = 0;
        if (c.moveToFirst() && !c.isNull(0)) {
            movId = c.getLong(0);
        }
        c.close();
        return movId;
    }

    private void guardarMovimiento(SQLiteDatabase db, long compraId, long movId) {
        ContentValues valores = new ContentValues();
        if (movId > 0) {
            valores.put(LocalCashDBHelper.COMPRA_MOVIMIENTO_ID, movId);
        } else {
            valores.putNull(LocalCashDBHelper.COMPRA_MOVIMIENTO_ID);
        }
        db.update(LocalCashDBHelper.T_COMPRA, valores,
                LocalCashDBHelper.COMPRA_ID + " = ?", new String[]{String.valueOf(compraId)});
    }

    private String hoy() {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
    }

    private void ponerContacto(ContentValues valores, long contactoId) {
        if (contactoId > 0) {
            valores.put(LocalCashDBHelper.COMPRA_CONTACTO_ID, contactoId);
        } else {
            valores.putNull(LocalCashDBHelper.COMPRA_CONTACTO_ID);
        }
    }

    private void ponerCuenta(ContentValues valores, long cuentaId) {
        if (cuentaId > 0) {
            valores.put(LocalCashDBHelper.COMPRA_CUENTA_ID, cuentaId);
        } else {
            valores.putNull(LocalCashDBHelper.COMPRA_CUENTA_ID);
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
        // Si tenía un egreso asociado, se elimina para que el saldo vuelva a su lugar
        long movId = movimientoDe(db, id);
        if (movId > 0) {
            db.delete(LocalCashDBHelper.T_MOVIMIENTO,
                    LocalCashDBHelper.MOV_ID + " = ?", new String[]{String.valueOf(movId)});
        }
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
                ", c." + LocalCashDBHelper.COMPRA_CUENTA_ID +
                " FROM " + LocalCashDBHelper.T_COMPRA + " c" +
                " LEFT JOIN " + LocalCashDBHelper.T_CONTACTO + " ct" +
                " ON c." + LocalCashDBHelper.COMPRA_CONTACTO_ID + " = ct." + LocalCashDBHelper.CONTACTO_ID;
    }

    private Compra leerCompra(Cursor c) {
        long contactoId = c.isNull(6) ? 0 : c.getLong(6);
        String contactoNombre = c.isNull(7) ? "" : c.getString(7);
        String lugar = c.isNull(8) ? "" : c.getString(8);
        long cuentaId = c.isNull(12) ? 0 : c.getLong(12);
        return new Compra(
                c.getLong(0), c.getString(1), c.getDouble(2), c.getDouble(3), c.getString(4),
                c.getString(5), contactoId, contactoNombre,
                lugar, c.getDouble(9), c.getDouble(10), c.getInt(11) == 1, cuentaId);
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
