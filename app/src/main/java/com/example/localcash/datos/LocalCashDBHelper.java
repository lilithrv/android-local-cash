package com.example.localcash.datos;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

// Helper central de la base de datos local SQLite (offline)

public class LocalCashDBHelper extends SQLiteOpenHelper {

    public static final String NOMBRE_BD = "localcash.db";
    public static final int VERSION_BD = 2;

    //  Tabla perfil (una sola fila con el dueño de la app)
    public static final String T_PERFIL = "perfil";
    public static final String PERFIL_ID = "id";
    public static final String PERFIL_NOMBRE = "nombre";
    public static final String PERFIL_PIN = "pin_respaldo";
    public static final String PERFIL_BIOMETRIA = "biometria_activa";

    //  Tabla contacto (agenda local)
    //  Tabla contacto (agenda local)
    public static final String T_CONTACTO = "contacto";
    public static final String CONTACTO_ID = "id";
    public static final String CONTACTO_NOMBRE = "nombre";
    public static final String CONTACTO_TELEFONO = "telefono";
    public static final String CONTACTO_EMAIL = "email";

    // Tabla cuenta
    public static final String T_CUENTA = "cuenta";
    public static final String CUENTA_ID = "id";
    public static final String CUENTA_NOMBRE = "nombre";
    public static final String CUENTA_TIPO = "tipo";
    public static final String CUENTA_SALDO_INICIAL = "saldo_inicial";

    // Tabla movimiento
    public static final String T_MOVIMIENTO = "movimiento";
    public static final String MOV_ID = "id";
    public static final String MOV_CUENTA_ID = "cuenta_id";
    public static final String MOV_TIPO = "tipo";
    public static final String MOV_MONTO = "monto";
    public static final String MOV_DESCRIPCION = "descripcion";
    public static final String MOV_FECHA = "fecha";
    public static final String MOV_LAT = "lat";
    public static final String MOV_LON = "lon";
    public static final String MOV_TIENE_GEO = "tiene_geo";

    // Tabla compra
    public static final String T_COMPRA = "compra";
    public static final String COMPRA_ID = "id";
    public static final String COMPRA_TITULO = "titulo";
    public static final String COMPRA_MONTO_ESTIMADO = "monto_estimado";
    public static final String COMPRA_MONTO_REAL = "monto_real";
    public static final String COMPRA_FECHA = "fecha";
    public static final String COMPRA_ESTADO = "estado";
    public static final String COMPRA_CONTACTO_ID = "contacto_id";
    public static final String COMPRA_LUGAR = "lugar_nombre";
    public static final String COMPRA_LAT = "lat";
    public static final String COMPRA_LON = "lon";
    public static final String COMPRA_TIENE_GEO = "tiene_geo";

    // Tabla compra_producto
    public static final String T_COMPRA_PRODUCTO = "compra_producto";
    public static final String CPR_ID = "id";
    public static final String CPR_COMPRA_ID = "compra_id";
    public static final String CPR_NOMBRE = "nombre";
    public static final String CPR_PRECIO_ESTIMADO = "precio_estimado";
    public static final String CPR_PRECIO_REAL = "precio_real";

    // Tabla gasto_compartido
    public static final String T_GASTO = "gasto_compartido";
    public static final String GASTO_ID = "id";
    public static final String GASTO_TITULO = "titulo";
    public static final String GASTO_MONTO_TOTAL = "monto_total";
    public static final String GASTO_FECHA = "fecha";

    // Tabla gc_producto
    public static final String T_GC_PRODUCTO = "gc_producto";
    public static final String GCP_ID = "id";
    public static final String GCP_GASTO_ID = "gasto_id";
    public static final String GCP_NOMBRE = "nombre";
    public static final String GCP_PRECIO = "precio";

    // Tabla gc_participante
    public static final String T_GC_PARTICIPANTE = "gc_participante";
    public static final String GCPA_ID = "id";
    public static final String GCPA_GASTO_ID = "gasto_id";
    public static final String GCPA_CONTACTO_ID = "contacto_id";
    public static final String GCPA_MONTO_ASIGNADO = "monto_asignado";
    public static final String GCPA_PAGADO = "pagado";

    public LocalCashDBHelper(Context context) {
        super(context, NOMBRE_BD, null, VERSION_BD);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_PERFIL + " (" +
                PERFIL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                PERFIL_NOMBRE + " TEXT NOT NULL, " +
                PERFIL_PIN + " TEXT, " +
                PERFIL_BIOMETRIA + " INTEGER NOT NULL DEFAULT 1)");

        db.execSQL("CREATE TABLE " + T_CONTACTO + " (" +
                CONTACTO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CONTACTO_NOMBRE + " TEXT NOT NULL, " +
                CONTACTO_TELEFONO + " TEXT, " +
                CONTACTO_EMAIL + " TEXT)");

        db.execSQL("CREATE TABLE " + T_CUENTA + " (" +
                CUENTA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CUENTA_NOMBRE + " TEXT NOT NULL, " +
                CUENTA_TIPO + " TEXT NOT NULL, " +
                CUENTA_SALDO_INICIAL + " REAL NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE " + T_MOVIMIENTO + " (" +
                MOV_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MOV_CUENTA_ID + " INTEGER NOT NULL, " +
                MOV_TIPO + " TEXT NOT NULL, " +
                MOV_MONTO + " REAL NOT NULL, " +
                MOV_DESCRIPCION + " TEXT, " +
                MOV_FECHA + " TEXT, " +
                MOV_LAT + " REAL, " +
                MOV_LON + " REAL, " +
                MOV_TIENE_GEO + " INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(" + MOV_CUENTA_ID + ") REFERENCES " + T_CUENTA + "(" + CUENTA_ID + "))");

        db.execSQL("CREATE TABLE " + T_COMPRA + " (" +
                COMPRA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COMPRA_TITULO + " TEXT NOT NULL, " +
                COMPRA_MONTO_ESTIMADO + " REAL NOT NULL DEFAULT 0, " +
                COMPRA_MONTO_REAL + " REAL NOT NULL DEFAULT 0, " +
                COMPRA_FECHA + " TEXT, " +
                COMPRA_ESTADO + " TEXT NOT NULL, " +
                COMPRA_CONTACTO_ID + " INTEGER, " +
                COMPRA_LUGAR + " TEXT, " +
                COMPRA_LAT + " REAL, " +
                COMPRA_LON + " REAL, " +
                COMPRA_TIENE_GEO + " INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(" + COMPRA_CONTACTO_ID + ") REFERENCES " + T_CONTACTO + "(" + CONTACTO_ID + "))");

        db.execSQL("CREATE TABLE " + T_COMPRA_PRODUCTO + " (" +
                CPR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CPR_COMPRA_ID + " INTEGER NOT NULL, " +
                CPR_NOMBRE + " TEXT NOT NULL, " +
                CPR_PRECIO_ESTIMADO + " REAL NOT NULL DEFAULT 0, " +
                CPR_PRECIO_REAL + " REAL NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(" + CPR_COMPRA_ID + ") REFERENCES " + T_COMPRA + "(" + COMPRA_ID + "))");

        db.execSQL("CREATE TABLE " + T_GASTO + " (" +
                GASTO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                GASTO_TITULO + " TEXT NOT NULL, " +
                GASTO_MONTO_TOTAL + " REAL NOT NULL DEFAULT 0, " +
                GASTO_FECHA + " TEXT)");

        db.execSQL("CREATE TABLE " + T_GC_PRODUCTO + " (" +
                GCP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                GCP_GASTO_ID + " INTEGER NOT NULL, " +
                GCP_NOMBRE + " TEXT NOT NULL, " +
                GCP_PRECIO + " REAL NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(" + GCP_GASTO_ID + ") REFERENCES " + T_GASTO + "(" + GASTO_ID + "))");

        db.execSQL("CREATE TABLE " + T_GC_PARTICIPANTE + " (" +
                GCPA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                GCPA_GASTO_ID + " INTEGER NOT NULL, " +
                GCPA_CONTACTO_ID + " INTEGER NOT NULL, " +
                GCPA_MONTO_ASIGNADO + " REAL NOT NULL DEFAULT 0, " +
                GCPA_PAGADO + " INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(" + GCPA_GASTO_ID + ") REFERENCES " + T_GASTO + "(" + GASTO_ID + "), " +
                "FOREIGN KEY(" + GCPA_CONTACTO_ID + ") REFERENCES " + T_CONTACTO + "(" + CONTACTO_ID + "))");
    }

    @Override
    public void onConfigure(@NonNull SQLiteDatabase db) {
        super.onConfigure(db);
        // Activar claves foráneas (SQLite las trae desactivadas por defecto)
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_GC_PARTICIPANTE);
        db.execSQL("DROP TABLE IF EXISTS " + T_GC_PRODUCTO);
        db.execSQL("DROP TABLE IF EXISTS " + T_GASTO);
        db.execSQL("DROP TABLE IF EXISTS " + T_COMPRA_PRODUCTO);
        db.execSQL("DROP TABLE IF EXISTS " + T_COMPRA);
        db.execSQL("DROP TABLE IF EXISTS " + T_MOVIMIENTO);
        db.execSQL("DROP TABLE IF EXISTS " + T_CUENTA);
        db.execSQL("DROP TABLE IF EXISTS " + T_CONTACTO);
        db.execSQL("DROP TABLE IF EXISTS " + T_PERFIL);
        onCreate(db);
    }
}
