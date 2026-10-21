package com.example.localcash.datos;

import com.example.localcash.database.LocalCashDBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.localcash.modelo.Cuenta;

import java.util.ArrayList;

// Acceso a la tabla cuenta y cálculo de saldos a partir de los movimientos
public class CuentaRepositorio {

    private final LocalCashDBHelper helper;

    public CuentaRepositorio(Context context) {
        this.helper = new LocalCashDBHelper(context.getApplicationContext());
    }

    public long agregar(String nombre, String tipo, double saldoInicial) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.CUENTA_NOMBRE, nombre);
        valores.put(LocalCashDBHelper.CUENTA_TIPO, tipo);
        valores.put(LocalCashDBHelper.CUENTA_SALDO_INICIAL, saldoInicial);
        return db.insert(LocalCashDBHelper.T_CUENTA, null, valores);
    }

    public void actualizar(long id, String nombre, String tipo, double saldoInicial) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(LocalCashDBHelper.CUENTA_NOMBRE, nombre);
        valores.put(LocalCashDBHelper.CUENTA_TIPO, tipo);
        valores.put(LocalCashDBHelper.CUENTA_SALDO_INICIAL, saldoInicial);
        db.update(LocalCashDBHelper.T_CUENTA, valores,
                LocalCashDBHelper.CUENTA_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void eliminar(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        // Primero se borran los movimientos de la cuenta para no dejar registros huérfanos.
        db.delete(LocalCashDBHelper.T_MOVIMIENTO,
                LocalCashDBHelper.MOV_CUENTA_ID + " = ?", new String[]{String.valueOf(id)});
        db.delete(LocalCashDBHelper.T_CUENTA,
                LocalCashDBHelper.CUENTA_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // Lista las cuentas con su saldo actual ya calculado
    public ArrayList<Cuenta> listar() {
        ArrayList<Cuenta> lista = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(LocalCashDBHelper.T_CUENTA, null, null, null, null, null,
                LocalCashDBHelper.CUENTA_NOMBRE + " ASC");
        while (c.moveToNext()) {
            Cuenta cuenta = new Cuenta(
                    c.getLong(c.getColumnIndexOrThrow(LocalCashDBHelper.CUENTA_ID)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.CUENTA_NOMBRE)),
                    c.getString(c.getColumnIndexOrThrow(LocalCashDBHelper.CUENTA_TIPO)),
                    c.getDouble(c.getColumnIndexOrThrow(LocalCashDBHelper.CUENTA_SALDO_INICIAL)));
            cuenta.setSaldoActual(calcularSaldo(db, cuenta.getId(), cuenta.getSaldoInicial()));
            lista.add(cuenta);
        }
        c.close();
        return lista;
    }

    // Saldo de una cuenta = saldo_inicial + ingresos - egresos
    private double calcularSaldo(SQLiteDatabase db, long cuentaId, double saldoInicial) {
        double ingresos = sumarPorTipo(db, cuentaId, "ingreso");
        double egresos = sumarPorTipo(db, cuentaId, "egreso");
        return saldoInicial + ingresos - egresos;
    }

    private double sumarPorTipo(SQLiteDatabase db, long cuentaId, String tipo) {
        Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(" + LocalCashDBHelper.MOV_MONTO + "), 0) FROM " +
                        LocalCashDBHelper.T_MOVIMIENTO +
                        " WHERE " + LocalCashDBHelper.MOV_CUENTA_ID + " = ? AND " +
                        LocalCashDBHelper.MOV_TIPO + " = ?",
                new String[]{String.valueOf(cuentaId), tipo});
        double suma = 0;
        if (c.moveToFirst()) {
            suma = c.getDouble(0);
        }
        c.close();
        return suma;
    }

    // Suma de los saldos de todas las cuentas (para el panel principal)
    public double saldoTotal() {
        double total = 0;
        for (Cuenta cuenta : listar()) {
            total += cuenta.getSaldoActual();
        }
        return total;
    }
}
