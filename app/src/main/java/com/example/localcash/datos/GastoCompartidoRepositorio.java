package com.example.localcash.datos;

import com.example.localcash.database.LocalCashDBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.localcash.modelo.GastoCompartido;
import com.example.localcash.modelo.GcParticipante;
import com.example.localcash.modelo.GcProducto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

// Acceso a las tablas de gastos compartidos: cabecera, productos y participantes (deudas). Módulo 3.
public class GastoCompartidoRepositorio {

    private final LocalCashDBHelper helper;

    public GastoCompartidoRepositorio(Context context) {
        this.helper = new LocalCashDBHelper(context.getApplicationContext());
    }

    // ---- Cabecera del gasto ----

    public long crearGasto(String titulo, double montoTotal, String fecha, long cuentaId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.GASTO_TITULO, titulo);
        valores.put(LocalCashDBHelper.GASTO_MONTO_TOTAL, montoTotal);
        valores.put(LocalCashDBHelper.GASTO_FECHA, fecha);
        if (cuentaId > 0) {
            valores.put(LocalCashDBHelper.GASTO_CUENTA_ID, cuentaId);
        } else {
            valores.putNull(LocalCashDBHelper.GASTO_CUENTA_ID);
        }
        long gastoId = db.insert(LocalCashDBHelper.T_GASTO, null, valores);
        // El dueño paga el total por adelantado: se registra un egreso en la cuenta elegida.
        if (cuentaId > 0 && montoTotal > 0) {
            ContentValues mov = new ContentValues();
            mov.put(LocalCashDBHelper.MOV_CUENTA_ID, cuentaId);
            mov.put(LocalCashDBHelper.MOV_TIPO, "egreso");
            mov.put(LocalCashDBHelper.MOV_MONTO, montoTotal);
            mov.put(LocalCashDBHelper.MOV_DESCRIPCION, "Gasto compartido: " + titulo);
            mov.put(LocalCashDBHelper.MOV_FECHA, hoy());
            long movId = db.insert(LocalCashDBHelper.T_MOVIMIENTO, null, mov);
            ContentValues enlace = new ContentValues();
            enlace.put(LocalCashDBHelper.GASTO_MOVIMIENTO_ID, movId);
            db.update(LocalCashDBHelper.T_GASTO, enlace,
                    LocalCashDBHelper.GASTO_ID + " = ?", new String[]{String.valueOf(gastoId)});
        }
        return gastoId;
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
        // El egreso del total debe seguir al nuevo total
        long movId = movimientoGasto(db, gastoId);
        if (movId > 0) {
            ContentValues mov = new ContentValues();
            mov.put(LocalCashDBHelper.MOV_MONTO, montoTotal);
            db.update(LocalCashDBHelper.T_MOVIMIENTO, mov,
                    LocalCashDBHelper.MOV_ID + " = ?", new String[]{String.valueOf(movId)});
        }
    }

    public void eliminarGasto(long gastoId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String[] arg = {String.valueOf(gastoId)};
        // Borra el egreso del total y los ingresos de cada reembolso para no descuadrar el saldo
        long movGasto = movimientoGasto(db, gastoId);
        if (movGasto > 0) {
            db.delete(LocalCashDBHelper.T_MOVIMIENTO,
                    LocalCashDBHelper.MOV_ID + " = ?", new String[]{String.valueOf(movGasto)});
        }
        for (long movPart : movimientosParticipantes(db, gastoId)) {
            db.delete(LocalCashDBHelper.T_MOVIMIENTO,
                    LocalCashDBHelper.MOV_ID + " = ?", new String[]{String.valueOf(movPart)});
        }
        db.delete(LocalCashDBHelper.T_GC_PARTICIPANTE, LocalCashDBHelper.GCPA_GASTO_ID + " = ?", arg);
        db.delete(LocalCashDBHelper.T_GC_PRODUCTO, LocalCashDBHelper.GCP_GASTO_ID + " = ?", arg);
        db.delete(LocalCashDBHelper.T_GASTO, LocalCashDBHelper.GASTO_ID + " = ?", arg);
    }

    // Id del egreso enlazado al gasto (0 si no tiene)
    private long movimientoGasto(SQLiteDatabase db, long gastoId) {
        Cursor c = db.query(LocalCashDBHelper.T_GASTO,
                new String[]{LocalCashDBHelper.GASTO_MOVIMIENTO_ID},
                LocalCashDBHelper.GASTO_ID + " = ?", new String[]{String.valueOf(gastoId)},
                null, null, null);
        long movId = 0;
        if (c.moveToFirst() && !c.isNull(0)) {
            movId = c.getLong(0);
        }
        c.close();
        return movId;
    }

    // Ids de los ingresos de reembolso de los participantes del gasto
    private ArrayList<Long> movimientosParticipantes(SQLiteDatabase db, long gastoId) {
        ArrayList<Long> ids = new ArrayList<>();
        Cursor c = db.query(LocalCashDBHelper.T_GC_PARTICIPANTE,
                new String[]{LocalCashDBHelper.GCPA_MOVIMIENTO_ID},
                LocalCashDBHelper.GCPA_GASTO_ID + " = ?", new String[]{String.valueOf(gastoId)},
                null, null, null);
        while (c.moveToNext()) {
            if (!c.isNull(0)) {
                ids.add(c.getLong(0));
            }
        }
        c.close();
        return ids;
    }

    private String hoy() {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
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
        // Si ya había registrado un reembolso (ingreso), se elimina junto con el participante
        long movId = movimientoParticipante(db, participanteId);
        if (movId > 0) {
            db.delete(LocalCashDBHelper.T_MOVIMIENTO,
                    LocalCashDBHelper.MOV_ID + " = ?", new String[]{String.valueOf(movId)});
        }
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
        sincronizarIngreso(db, id, montoAsignado, pagado);
    }

    private void sincronizarIngreso(SQLiteDatabase db, long participanteId,
                                    double monto, boolean pagado) {
        // Datos del participante y de su gasto (cuenta de donde salió el total y textos para la descripción)
        String sql = "SELECT g." + LocalCashDBHelper.GASTO_CUENTA_ID +
                ", p." + LocalCashDBHelper.GCPA_MOVIMIENTO_ID +
                ", ct." + LocalCashDBHelper.CONTACTO_NOMBRE +
                ", g." + LocalCashDBHelper.GASTO_TITULO +
                " FROM " + LocalCashDBHelper.T_GC_PARTICIPANTE + " p" +
                " JOIN " + LocalCashDBHelper.T_GASTO + " g ON p." + LocalCashDBHelper.GCPA_GASTO_ID +
                " = g." + LocalCashDBHelper.GASTO_ID +
                " JOIN " + LocalCashDBHelper.T_CONTACTO + " ct ON p." + LocalCashDBHelper.GCPA_CONTACTO_ID +
                " = ct." + LocalCashDBHelper.CONTACTO_ID +
                " WHERE p." + LocalCashDBHelper.GCPA_ID + " = ?";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(participanteId)});
        if (!c.moveToFirst()) {
            c.close();
            return;
        }
        long cuentaId = c.isNull(0) ? 0 : c.getLong(0);
        long movId = c.isNull(1) ? 0 : c.getLong(1);
        String nombre = c.getString(2);
        String titulo = c.getString(3);
        c.close();

        boolean corresponde = pagado && cuentaId > 0 && monto > 0;
        if (corresponde) {
            ContentValues mov = new ContentValues();
            mov.put(LocalCashDBHelper.MOV_CUENTA_ID, cuentaId);
            mov.put(LocalCashDBHelper.MOV_TIPO, "ingreso");
            mov.put(LocalCashDBHelper.MOV_MONTO, monto);
            mov.put(LocalCashDBHelper.MOV_DESCRIPCION, "Reembolso de " + nombre + ": " + titulo);
            if (movId > 0) {
                db.update(LocalCashDBHelper.T_MOVIMIENTO, mov,
                        LocalCashDBHelper.MOV_ID + " = ?", new String[]{String.valueOf(movId)});
            } else {
                mov.put(LocalCashDBHelper.MOV_FECHA, hoy());
                long nuevoMov = db.insert(LocalCashDBHelper.T_MOVIMIENTO, null, mov);
                guardarMovimientoParticipante(db, participanteId, nuevoMov);
            }
        } else if (movId > 0) {
            db.delete(LocalCashDBHelper.T_MOVIMIENTO,
                    LocalCashDBHelper.MOV_ID + " = ?", new String[]{String.valueOf(movId)});
            guardarMovimientoParticipante(db, participanteId, 0);
        }
    }

    private long movimientoParticipante(SQLiteDatabase db, long participanteId) {
        Cursor c = db.query(LocalCashDBHelper.T_GC_PARTICIPANTE,
                new String[]{LocalCashDBHelper.GCPA_MOVIMIENTO_ID},
                LocalCashDBHelper.GCPA_ID + " = ?", new String[]{String.valueOf(participanteId)},
                null, null, null);
        long movId = 0;
        if (c.moveToFirst() && !c.isNull(0)) {
            movId = c.getLong(0);
        }
        c.close();
        return movId;
    }

    private void guardarMovimientoParticipante(SQLiteDatabase db, long participanteId, long movId) {
        ContentValues valores = new ContentValues();
        if (movId > 0) {
            valores.put(LocalCashDBHelper.GCPA_MOVIMIENTO_ID, movId);
        } else {
            valores.putNull(LocalCashDBHelper.GCPA_MOVIMIENTO_ID);
        }
        db.update(LocalCashDBHelper.T_GC_PARTICIPANTE, valores,
                LocalCashDBHelper.GCPA_ID + " = ?", new String[]{String.valueOf(participanteId)});
    }
}
