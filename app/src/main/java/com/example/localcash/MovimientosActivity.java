package com.example.localcash;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.adapter.MovimientoAdapter;
import com.example.localcash.datos.CuentaRepositorio;
import com.example.localcash.datos.MovimientoRepositorio;
import com.example.localcash.modelo.Cuenta;
import com.example.localcash.modelo.Movimiento;
import com.example.localcash.util.Paginador;
import com.example.localcash.util.UbicacionHelper;
import com.example.localcash.util.Validaciones;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

// Registro de ingresos/egresos de una cuenta, con geolocalización opcional
public class MovimientosActivity extends BaseActivity {

    Spinner spCuenta;
    RadioGroup rgTipo;
    EditText etMonto, etDescripcion;
    Switch swGeo;
    Button btnAgregar, btnVerMapa, btnCancelar;
    RecyclerView rvMovimientos;

    // Estado de edición: id del movimiento y su geo original para conservarla
    long editandoId = 0;
    double editandoLat = 0, editandoLon = 0;
    boolean editandoTieneGeo = false;

    CuentaRepositorio cuentaRepositorio;
    MovimientoRepositorio movimientoRepositorio;

    ArrayList<Cuenta> cuentas = new ArrayList<>();
    ArrayList<Movimiento> movimientos = new ArrayList<>();       // página visible
    ArrayList<Movimiento> todosMovimientos = new ArrayList<>();  // lista completa de la cuenta
    MovimientoAdapter adapter;
    final Paginador<Movimiento> paginador = new Paginador<>();

    LinearLayout barraPaginacion;
    Button btnPagAnterior, btnPagSiguiente;
    TextView tvPagina;

    // Ubicación
    LocationManager locationManager;
    Location ultimaFija;

    // Guarda cada nueva lectura de ubicación
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            ultimaFija = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    // Lanzador para pedir el permiso de ubicación en tiempo de ejecución
    private final ActivityResultLauncher<String> permisoUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    iniciarActualizacionesUbicacion();
                } else {
                    swGeo.setChecked(false);
                    Toast.makeText(this, R.string.amv_permiso_denegado, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movimientos);
        configurarBarra(getString(R.string.amv_titulo));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spCuenta = findViewById(R.id.spCuenta);
        rgTipo = findViewById(R.id.rgTipo);
        etMonto = findViewById(R.id.etMonto);
        etDescripcion = findViewById(R.id.etDescripcion);
        swGeo = findViewById(R.id.swGeo);
        btnAgregar = findViewById(R.id.btnAgregar);
        btnVerMapa = findViewById(R.id.btnVerMapa);
        btnCancelar = findViewById(R.id.btnCancelar);
        rvMovimientos = findViewById(R.id.rvMovimientos);

        cuentaRepositorio = new CuentaRepositorio(this);
        movimientoRepositorio = new MovimientoRepositorio(this);

        adapter = new MovimientoAdapter(movimientos, this::editarMovimiento, this::eliminarMovimiento);
        rvMovimientos.setLayoutManager(new LinearLayoutManager(this));
        rvMovimientos.setAdapter(adapter);

        barraPaginacion = findViewById(R.id.barraPaginacion);
        btnPagAnterior = findViewById(R.id.btnPagAnterior);
        btnPagSiguiente = findViewById(R.id.btnPagSiguiente);
        tvPagina = findViewById(R.id.tvPagina);
        btnPagAnterior.setOnClickListener(v -> { paginador.anterior(); mostrarPagina(); });
        btnPagSiguiente.setOnClickListener(v -> { paginador.siguiente(); mostrarPagina(); });

        cargarCuentas();

        // Al recargar la lista según la cuenta elegida en el Spinner
        spCuenta.setOnItemSelectedListener(new SimpleItemSelected(this::cargarMovimientos));

        // Al activar la ubicación se solicita el permiso (si falta) y se empieza a escuchar
        // la ubicación para tener un fix listo al guardar; al desactivar se deja de escuchar.
        swGeo.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                if (UbicacionHelper.tienePermiso(this)) {
                    iniciarActualizacionesUbicacion();
                } else {
                    permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            } else {
                detenerActualizacionesUbicacion();
            }
        });

        btnAgregar.setOnClickListener(v -> guardarMovimiento());
        btnCancelar.setOnClickListener(v -> cancelarEdicion());
        btnVerMapa.setOnClickListener(v -> startActivity(new Intent(this, MapaActivity.class)));
    }

    private void cargarCuentas() {
        cuentas.clear();
        cuentas.addAll(cuentaRepositorio.listar());
        ArrayAdapter<Cuenta> cuentaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cuentas);
        cuentaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCuenta.setAdapter(cuentaAdapter);
        cargarMovimientos();
    }

    private void cargarMovimientos() {
        todosMovimientos.clear();
        Cuenta cuenta = cuentaSeleccionada();
        if (cuenta != null) {
            todosMovimientos.addAll(movimientoRepositorio.listarPorCuenta(cuenta.getId()));
        }
        paginador.setDatos(todosMovimientos);
        mostrarPagina();
    }

    // Muestra la página actual en la lista y actualiza la barra de paginación
    private void mostrarPagina() {
        movimientos.clear();
        movimientos.addAll(paginador.paginaActual());
        adapter.notifyDataSetChanged();
        boolean varias = paginador.totalPaginas() > 1;
        barraPaginacion.setVisibility(varias ? View.VISIBLE : View.GONE);
        tvPagina.setText(getString(R.string.paginador_pagina,
                paginador.paginaHumana(), paginador.totalPaginas()));
        btnPagAnterior.setEnabled(paginador.hayAnterior());
        btnPagSiguiente.setEnabled(paginador.haySiguiente());
    }

    private void guardarMovimiento() {
        Cuenta cuenta = cuentaSeleccionada();
        if (cuenta == null) {
            Toast.makeText(this, R.string.amv_sin_cuentas, Toast.LENGTH_SHORT).show();
            return;
        }
        double monto = Validaciones.aDouble(etMonto);
        if (monto <= 0) {
            etMonto.setError(getString(R.string.amv_error_monto));
            return;
        }
        String descripcion = etDescripcion.getText().toString().trim();
        String tipo = (rgTipo.getCheckedRadioButtonId() == R.id.rbIngreso)
                ? Movimiento.TIPO_INGRESO : Movimiento.TIPO_EGRESO;

        double lat = 0, lon = 0;
        boolean tieneGeo = false;
        if (swGeo.isChecked()) {
            // Preferir el fix fresco de la escucha activa; si aún no llega, usar la última conocida
            Location ubicacion = (ultimaFija != null) ? ultimaFija : UbicacionHelper.ultimaUbicacion(this);
            if (ubicacion != null) {
                lat = ubicacion.getLatitude();
                lon = ubicacion.getLongitude();
                tieneGeo = true;
            } else if (editandoId != 0 && editandoTieneGeo) {
                // En edición sin fix nuevo: conservar la ubicación original del movimiento
                lat = editandoLat;
                lon = editandoLon;
                tieneGeo = true;
            } else {
                Toast.makeText(this, R.string.amv_sin_ubicacion, Toast.LENGTH_LONG).show();
            }
        }

        if (editandoId == 0) {
            String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            movimientoRepositorio.agregar(cuenta.getId(), tipo, monto, descripcion, fecha, lat, lon, tieneGeo);
        } else {
            movimientoRepositorio.actualizar(editandoId, cuenta.getId(), tipo, monto, descripcion, lat, lon, tieneGeo);
            Toast.makeText(this, R.string.amv_actualizado, Toast.LENGTH_SHORT).show();
        }
        cancelarEdicion();
        cargarMovimientos();
    }

    private void editarMovimiento(Movimiento mov) {
        editandoId = mov.getId();
        editandoLat = mov.getLat();
        editandoLon = mov.getLon();
        editandoTieneGeo = mov.isTieneGeo();
        seleccionarCuenta(mov.getCuentaId());
        rgTipo.check(mov.esIngreso() ? R.id.rbIngreso : R.id.rbEgreso);
        etMonto.setText(String.format(Locale.getDefault(), "%.0f", mov.getMonto()));
        etDescripcion.setText(mov.getDescripcion());
        swGeo.setChecked(mov.isTieneGeo());
        btnAgregar.setText(R.string.amv_btnguardar_cambios);
        btnCancelar.setVisibility(View.VISIBLE);
        etMonto.requestFocus();
    }

    private void eliminarMovimiento(Movimiento mov) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.amv_eliminar_titulo)
                .setMessage(R.string.amv_eliminar_mensaje)
                .setPositiveButton(R.string.amv_eliminar_confirmar, (dialog, which) -> {
                    movimientoRepositorio.eliminar(mov.getId());
                    Toast.makeText(this, R.string.amv_eliminado, Toast.LENGTH_SHORT).show();
                    if (editandoId == mov.getId()) {
                        cancelarEdicion();
                    }
                    cargarMovimientos();
                })
                .setNegativeButton(R.string.amv_eliminar_cancelar, null)
                .show();
    }

    private void seleccionarCuenta(long cuentaId) {
        for (int i = 0; i < cuentas.size(); i++) {
            if (cuentas.get(i).getId() == cuentaId) {
                spCuenta.setSelection(i);
                return;
            }
        }
    }

    private Cuenta cuentaSeleccionada() {
        return (Cuenta) spCuenta.getSelectedItem();
    }

    // Limpiar formulario
    private void cancelarEdicion() {
        editandoId = 0;
        editandoLat = 0;
        editandoLon = 0;
        editandoTieneGeo = false;
        etMonto.setText("");
        etDescripcion.setText("");
        etMonto.setError(null);
        rgTipo.check(R.id.rbIngreso);
        swGeo.setChecked(false);
        btnAgregar.setText(R.string.amv_btnagregar);
        btnCancelar.setVisibility(View.GONE);
        etMonto.requestFocus();
    }

    private void iniciarActualizacionesUbicacion() {
        if (!UbicacionHelper.tienePermiso(this)) {
            return;
        }
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
        if (locationManager == null) {
            return;
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 2000, 5, locationListener);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 2000, 5, locationListener);
            }
            if (ultimaFija == null) {
                ultimaFija = UbicacionHelper.ultimaUbicacion(this);
            }
        } catch (SecurityException e) {
            // Sin permiso efectivo; al guardar se usará el respaldo o se avisará
        }
    }

    // Deja de escuchar la ubicación para no consumir batería fuera de esta pantalla
    private void detenerActualizacionesUbicacion() {
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (swGeo.isChecked() && UbicacionHelper.tienePermiso(this)) {
            iniciarActualizacionesUbicacion();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        detenerActualizacionesUbicacion();
    }

    // Pequeño adaptador de listener para no repetir métodos vacíos del OnItemSelectedListener
    private static class SimpleItemSelected implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable alSeleccionar;

        SimpleItemSelected(Runnable alSeleccionar) {
            this.alSeleccionar = alSeleccionar;
        }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                   int position, long id) {
            alSeleccionar.run();
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }
    }
}
