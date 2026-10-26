# LocalCash: Aplicación Móvil en Android

LocalCash es una aplicación móvil nativa Android (Java) que funciona 100% offline. Combina el
control de cuentas personales, el registro de movimientos geolocalizados, la planificación de compras
y un módulo para dividir gastos y deudas entre contactos. El acceso está protegido con autenticación
biométrica (huella) y un PIN de respaldo. Todos los datos se guardan localmente en SQLite.


---

## 1. Funcionalidades por módulo

**Módulo 1 - Acceso y contactos**
- Primer uso: creación de un perfil local (nombre + PIN de respaldo). El PIN no se guarda en texto plano, sino como hash PBKDF2 con sal (`SeguridadPin`).
- Acceso con BiometricPrompt (huella); si no hay huella registrada o el usuario lo prefiere, valida el PIN.
- Agenda de contactos local (añadir, listar, borrar).

**Módulo 2 - Cuentas y movimientos**
- Cuentas financieras (Efectivo / Ahorro / Tarjeta / Otro) con saldo calculado (saldo inicial + ingresos − egresos).
- Movimientos de ingreso/egreso por cuenta, con geolocalización opcional.
- Mapa (Google Maps) con un marcador por cada movimiento geolocalizado.

**Módulo 3 - Compras e historial**
- Registro de compras realizadas o agendadas (futuras), con un carrito de productos.
- Monto estimado (suma de productos) y, al marcarla realizada, monto real editable.
- Lugar opcional elegido en el mapa (tocar, GPS o búsqueda por nombre) y enlace opcional a un contacto.
- Al marcarla realizada puede pagarse desde una cuenta: genera automáticamente un egreso por el monto real, que hereda la ubicación de la compra (figura en el mapa) y descuenta el saldo. Si la compra vuelve a pendiente o se borra, el egreso se elimina.
- Listas con paginación reutilizable.

**Módulo 4 - Gastos compartidos y deudas**
- Creación de una compra grupal agregando productos/servicios.
- Selección de participantes de la agenda.
- Reparto equitativo o personalizado del total, con saldos pendientes y marca de pagado.
- Puede pagarse desde una cuenta: el dueño registra un egreso por el total adelantado y, cuando cada participante marca su parte como pagada (reembolso), se registra un ingreso en esa cuenta.

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
    │   ├── MapaActivity.java            # Mapa Google Maps con marcadores
    │   ├── ComprasActivity.java         # Historial y compras agendadas
    │   ├── CompraDetalleActivity.java   # Añadir/edición de compra: carrito + lugar + monto real
    │   ├── SelectorUbicacionActivity.java # Elegir lugar en el mapa (tocar / GPS / buscar)
    │   ├── GastoCompartidoActivity.java # Crear gasto grupal (productos + participantes)
    │   ├── DeudasActivity.java          # Reparto y saldos por participante
    │   ├── modelo/   # Perfil, Contacto, Cuenta, Movimiento, Compra, CompraProducto,
    │   │             # GastoCompartido, GcProducto, GcParticipante
    │   ├── database/ # LocalCashDBHelper (creación/actualización del esquema SQLite)
    │   ├── datos/    # Repositorios de acceso a la DB (Perfil, Contacto, Cuenta,
    │   │             # Movimiento, Compra, GastoCompartido)
    │   ├── adapter/  # Contacto, Cuenta, Movimiento, Compra, CompraProducto, Producto, Gasto
    │   └── util/     # Validaciones, BiometriaHelper, SeguridadPin, UbicacionHelper,
    │                 # Formato, Paginador, PinMapa
    └── res/
        ├── layout/   # activity_*.xml, item_*.xml
        └── values/   # strings.xml, arrays.xml, colors.xml, themes.xml
        └── drawable/ # iconografía (los pines del mapa se dibujan por código en PinMapa)
```

**Separación de responsabilidades**
- **UI:** `res/layout/` define las vistas; las *Activities* las controlan.
- **Modelo:** clases en `modelo/`.
- **Datos:** `database/LocalCashDBHelper` crea y mantiene el esquema SQLite; los repositorios de
  `datos/` centralizan el acceso a la base de datos.
- **Reutilización:** `BaseActivity` (toolbar + "volver"), `Validaciones` (formularios),
  `BiometriaHelper` (BiometricPrompt), `SeguridadPin` (hash PBKDF2 del PIN de respaldo),
  `UbicacionHelper` (ubicación), `Formato` (locale es-CL para los montos), `Paginador`
  (paginación genérica de listas), `PinMapa` (íconos de los marcadores del mapa) y un *Adapter* por lista.

---

## 4. Base de datos local (SQLite)

Archivo `localcash.db`, definido en `database/LocalCashDBHelper.java`. Claves foráneas activadas.

| Tabla | Campos | Relación |
|-------|--------|----------|
| `perfil` | id, nombre, pin_respaldo *(hash PBKDF2 con sal)*, biometria_activa | 1 fila (dueño de la app) |
| `contacto` | id, nombre, telefono, email | agenda local |
| `cuenta` | id, nombre, tipo, saldo_inicial | — |
| `movimiento` | id, **cuenta_id**, tipo, monto, descripcion, fecha, lat, lon, tiene_geo | `cuenta 1—N movimiento` |
| `compra` | id, titulo, monto_estimado, monto_real, fecha, estado, **contacto_id**, lugar_nombre, lat, lon, tiene_geo, **cuenta_id**, **movimiento_id** | `contacto 1—N compra`; opcionalmente paga desde `cuenta` y genera un `movimiento` |
| `compra_producto` | id, **compra_id**, nombre, precio_estimado, precio_real | `compra 1—N producto` |
| `gasto_compartido` | id, titulo, monto_total, fecha, **cuenta_id**, **movimiento_id** | opcionalmente paga desde `cuenta` y genera un `movimiento` |
| `gc_producto` | id, **gasto_id**, nombre, precio | `gasto 1—N producto` |
| `gc_participante` | id, **gasto_id**, **contacto_id**, monto_asignado, pagado, **movimiento_id** | `gasto N—M contacto` |

**Cálculos clave**
- **Saldo de una cuenta** = `saldo_inicial` +  ingresos −  egresos (consulta en `CuentaRepositorio`).
  Los egresos/ingresos incluyen los movimientos que generan automáticamente las compras pagadas y los
  gastos compartidos enlazados a esa cuenta.
- **Total de una compra** =  `precio_estimado` de sus productos; al marcarla realizada se usa el
  **monto real** ( `precio_real`, editable), que es también el importe del egreso si se pagó desde una cuenta.
- **Deuda de cada participante** = `monto_total / nº participantes` (equitativo), o valor personalizado
  guardado en `monto_asignado`.

---

## 5. Instalación y ejecución

1. Abrir la carpeta `localcash/` en **Android Studio**.
2. Agregar la **API key de Google Maps** en `localcash/local.properties`:
   `MAPS_API_KEY=TU_CLAVE` (obtenida en Google Cloud Console con "Maps SDK for Android" habilitado).
3. Esperar la **sincronización de Gradle** (descarga de dependencias, incluida play-services-maps).
4. Ejecutar con **Run ▶** en un emulador o dispositivo con **Android 8.0 (API 26) o superior**.

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
