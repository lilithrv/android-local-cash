# LocalCash: Aplicación Móvil en Android

LocalCash es una aplicación móvil nativa Android (Java) que funciona 100% offline. Combina el
control de cuentas personales, el registro de movimientos geolocalizados, la planificación de compras
y un módulo para dividir gastos y deudas entre contactos. El acceso está protegido con autenticación
biométrica (huella) y un PIN de respaldo. Todos los datos se guardan localmente en SQLite.


---

## 1. Funcionalidades por módulo

**Módulo 1 - Acceso y contactos**
- Primer uso: creación de un perfil local (nombre + PIN de respaldo).
- Acceso con BiometricPrompt (huella); si no hay huella registrada o el usuario lo prefiere, valida el PIN.
- Agenda de contactos local (añadir, listar, borrar).

**Módulo 2 - Cuentas y movimientos**
- Cuentas financieras (Efectivo / Ahorro / Tarjeta / Otro) con saldo calculado (saldo inicial + ingresos − egresos).
- Movimientos de ingreso/egreso por cuenta, con geolocalización opcional.
- Mapa (osmdroid) con un marcador por cada movimiento geolocalizado.

**Módulo 3 - Compras e historial**
- Registro de compras realizadas o agendadas (futuras), con un carrito de productos.
- Monto estimado (suma de productos) y, al marcarla realizada, monto real editable.
- Lugar opcional elegido en el mapa (tocar, GPS o búsqueda por nombre) y enlace opcional a un contacto.
- Listas con paginación reutilizable.

**Módulo 4 - Gastos compartidos y deudas**
- Creación de una compra grupal agregando productos/servicios.
- Selección de participantes de la agenda.
- Reparto equitativo o personalizado del total, con saldos pendientes y marca de pagado.

---

## 2. Requisitos técnicos

- Android Studio.
- JDK 11.
- SDK Android: `minSdk 26` (Android 8.0 **Oreo**), `compileSdk` / `targetSdk 37`.
- Lenguaje: Java. 

---

## 3. Arquitectura y estructura del proyecto

Cada pantalla es una *Activity*; los datos se separan en modelos, repositorios (acceso a
SQLite) y adapters (RecyclerView), con utilidades reutilizables.

```
localcash/
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/com/example/localcash/
    │   ├── BaseActivity.java            # Base con toolbar y flecha "volver" reutilizable
    │   ├── MainActivity.java            # Acceso: biometría + PIN de respaldo
    │   ├── SetupActivity.java           # Creación del perfil local
    │   ├── HomeActivity.java            # Panel: saldo total y accesos a módulos
    │   ├── ContactosActivity.java       # Agenda local
    │   ├── CuentasActivity.java         # Cuentas con saldo
    │   ├── MovimientosActivity.java     # Ingresos/egresos + geolocalización
    │   ├── MapaActivity.java            # Mapa osmdroid con marcadores
    │   ├── ComprasActivity.java         # Historial y compras agendadas
    │   ├── CompraDetalleActivity.java   # Añadir/edición de compra: carrito + lugar + monto real
    │   ├── SelectorUbicacionActivity.java # Elegir lugar en el mapa (tocar / GPS / buscar)
    │   ├── GastoCompartidoActivity.java # Crear gasto grupal (productos + participantes)
    │   ├── DeudasActivity.java          # Reparto y saldos por participante
    │   ├── modelo/   # Perfil, Contacto, Cuenta, Movimiento, Compra, CompraProducto,
    │   │             # GastoCompartido, GcProducto, GcParticipante
    │   ├── datos/    # LocalCashDBHelper + repositorio (acceso a DB)
    │   ├── adapter/  # Contacto, Cuenta, Movimiento, Compra, CompraProducto, Producto, Gasto
    │   └── util/     # Validaciones, BiometriaHelper, UbicacionHelper, Formato, Paginador
    └── res/
        ├── layout/   # activity_*.xml, item_*.xml
        └── values/   # strings.xml, arrays.xml, colors.xml, themes.xml
```

**Separación de responsabilidades**
- **UI:** `res/layout/` define las vistas; las *Activities* las controlan.
- **Modelo:** clases en `modelo/`.
- **Datos:** `datos/` centraliza el acceso a SQLite mediante `LocalCashDBHelper` y repositorios de acceso a la base de datos.
- **Reutilización:** `BaseActivity` (toolbar + "volver"), `Validaciones` (formularios),
  `BiometriaHelper` (BiometricPrompt), `UbicacionHelper` (ubicación), `Formato` (locale es-CL
  para los montos), `Paginador` (paginación genérica de listas), y un *Adapter* por lista.

---

## 4. Base de datos local (SQLite)

Archivo `localcash.db`, definido en `datos/LocalCashDBHelper.java`. Claves foráneas activadas.

| Tabla | Campos | Relación |
|-------|--------|----------|
| `perfil` | id, nombre, pin_respaldo, biometria_activa | 1 fila (dueño de la app) |
| `contacto` | id, nombre, telefono, email | agenda local |
| `cuenta` | id, nombre, tipo, saldo_inicial | — |
| `movimiento` | id, **cuenta_id**, tipo, monto, descripcion, fecha, lat, lon, tiene_geo | `cuenta 1—N movimiento` |
| `compra` | id, titulo, monto_estimado, monto_real, fecha, estado, **contacto_id**, lugar_nombre, lat, lon, tiene_geo | `contacto 1—N compra` |
| `compra_producto` | id, **compra_id**, nombre, precio_estimado, precio_real | `compra 1—N producto` |
| `gasto_compartido` | id, titulo, monto_total, fecha | — |
| `gc_producto` | id, **gasto_id**, nombre, precio | `gasto 1—N producto` |
| `gc_participante` | id, **gasto_id**, **contacto_id**, monto_asignado, pagado | `gasto N—M contacto` |

**Cálculos clave**
- **Saldo de una cuenta** = `saldo_inicial` +  ingresos −  egresos (consulta en `CuentaRepositorio`).
- **Total de una compra** =  `precio_estimado` de sus productos; al marcarla realizada se usa el
  **monto real** ( `precio_real`, editable).
- **Deuda de cada participante** = `monto_total / nº participantes` (equitativo), o valor personalizado
  guardado en `monto_asignado`.

---

## 5. Instalación y ejecución

1. Abrir la carpeta `localcash/` en **Android Studio**.
2. Esperar la **sincronización de Gradle** (descarga de dependencias, incluida osmdroid).
3. Ejecutar con **Run ▶** en un emulador o dispositivo con **Android 8.0 (API 26) o superior**.

---

## 6. Flujo de navegación

```
[MainActivity] ──sin perfil──▶ [SetupActivity] ──▶ (vuelve) ──▶ biometría/PIN ──▶ [HomeActivity]
      └── con perfil ──▶ biometría/PIN ──▶ [HomeActivity]

[HomeActivity] ──▶ Cuentas · Movimientos · Mapa · Compras · Gastos compartidos · Contactos
[MovimientosActivity] ──▶ [MapaActivity]
[ComprasActivity] ──▶ [CompraDetalleActivity] ──▶ [SelectorUbicacionActivity]
[GastoCompartidoActivity] ──▶ [DeudasActivity]
```

---
