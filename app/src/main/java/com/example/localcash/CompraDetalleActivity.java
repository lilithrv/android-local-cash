package com.example.localcash;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.adapter.CompraProductoAdapter;
import com.example.localcash.datos.CompraRepositorio;
import com.example.localcash.datos.ContactoRepositorio;
import com.example.localcash.datos.CuentaRepositorio;
import com.example.localcash.modelo.Compra;
import com.example.localcash.modelo.CompraProducto;
import com.example.localcash.modelo.Contacto;
import com.example.localcash.modelo.Cuenta;
import com.example.localcash.util.Formato;
import com.example.localcash.util.Paginador;
import com.example.localcash.util.Validaciones;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CompraDetalleActivity extends BaseActivity
        implements CompraProductoAdapter.Listener {

    EditText etTitulo, etProductoNombre, etProductoPrecio, etTotalReal;
    RadioGroup rgEstado;
    Spinner spContacto, spCuenta;
    Button btnAgregarProducto, btnElegirLugar, btnGuardar;
    TextView tvTotalEstimado, tvLugar;
    LinearLayout bloqueReal;
    RecyclerView rvProductos;

    // Paginación del carrito
    LinearLayout barraPaginacionProd;
    Button btnPagAnteriorProd, btnPagSiguienteProd;
    TextView tvPaginaProd;

    CompraRepositorio compraRepositorio;
    ContactoRepositorio contactoRepositorio;
    CuentaRepositorio cuentaRepositorio;

    // Id de la compra en edición
    long editandoId = 0;

    // Cuentas disponibles para descontar el egreso al realizar la compra
    ArrayList<Cuenta> cuentas = new ArrayList<>();

    // Productos del carrito (en memoria hasta guardar)
    ArrayList<CompraProducto> productos = new ArrayList<>();
    ArrayList<CompraProducto> todosProductos = new ArrayList<>();
    CompraProductoAdapter productoAdapter;
    final Paginador<CompraProducto> paginadorProd = new Paginador<>();

    ArrayList<Contacto> contactos = new ArrayList<>();

    // Estado del lugar elegido
    String lugarNombre = "";
    double lugarLat = 0, lugarLon = 0;
    boolean lugarTieneGeo = false;

    // Lanzador del selector de ubicación en el mapa
    private final ActivityResultLauncher<Intent> selectorUbicacion =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                    Intent datos = resultado.getData();
                    lugarNombre = datos.getStringExtra(SelectorUbicacionActivity.EXTRA_NOMBRE);
                    if (lugarNombre == null) {
                        lugarNombre = "";
                    }
                    lugarLat = datos.getDoubleExtra(SelectorUbicacionActivity.EXTRA_LAT, 0);
                    lugarLon = datos.getDoubleExtra(SelectorUbicacionActivity.EXTRA_LON, 0);
                    lugarTieneGeo = datos.getBooleanExtra(SelectorUbicacionActivity.EXTRA_TIENE_GEO, false);
                    mostrarLugar();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_compra_detalle);

        etTitulo = findViewById(R.id.etTitulo);
        rgEstado = findViewById(R.id.rgEstado);
        etProductoNombre = findViewById(R.id.etProductoNombre);
        etProductoPrecio = findViewById(R.id.etProductoPrecio);
        btnAgregarProducto = findViewById(R.id.btnAgregarProducto);
        rvProductos = findViewById(R.id.rvProductos);
        tvTotalEstimado = findViewById(R.id.tvTotalEstimado);
        bloqueReal = findViewById(R.id.bloqueReal);
        etTotalReal = findViewById(R.id.etTotalReal);
        tvLugar = findViewById(R.id.tvLugar);
        btnElegirLugar = findViewById(R.id.btnElegirLugar);
        spContacto = findViewById(R.id.spContacto);
        spCuenta = findViewById(R.id.spCuenta);
        btnGuardar = findViewById(R.id.btnGuardar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        compraRepositorio = new CompraRepositorio(this);
        contactoRepositorio = new ContactoRepositorio(this);
        cuentaRepositorio = new CuentaRepositorio(this);

        productoAdapter = new CompraProductoAdapter(productos, this);
        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        rvProductos.setAdapter(productoAdapter);

        barraPaginacionProd = findViewById(R.id.barraPaginacionProd);
        btnPagAnteriorProd = findViewById(R.id.btnPagAnteriorProd);
        btnPagSiguienteProd = findViewById(R.id.btnPagSiguienteProd);
        tvPaginaProd = findViewById(R.id.tvPaginaProd);
        btnPagAnteriorProd.setOnClickListener(v -> { paginadorProd.anterior(); mostrarPaginaProductos(); });
        btnPagSiguienteProd.setOnClickListener(v -> { paginadorProd.siguiente(); mostrarPaginaProductos(); });

        cargarContactos();
        cargarCuentas();

        btnAgregarProducto.setOnClickListener(v -> agregarProducto());
        btnElegirLugar.setOnClickListener(v -> abrirSelectorUbicacion());
        btnGuardar.setOnClickListener(v -> guardar());

        // Al cambiar de estado se muestra/oculta el bloque de monto real
        rgEstado.setOnCheckedChangeListener((group, checkedId) -> aplicarModoReal());

        editandoId = getIntent().getLongExtra(ComprasActivity.EXTRA_COMPRA_ID, 0);
        if (editandoId > 0) {
            configurarBarra(getString(R.string.acp_detalle_editar));
            cargarCompra();
        } else {
            configurarBarra(getString(R.string.acp_detalle_nueva));
        }

        mostrarLugar();
        aplicarModoReal();
        actualizarTotalEstimado();
    }

    private void cargarContactos() {
        contactos.clear();
        contactos.add(new Contacto(0, getString(R.string.acp_sin_contacto), "", ""));
        contactos.addAll(contactoRepositorio.listar());
        ArrayAdapter<Contacto> contactoAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, contactos);
        contactoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spContacto.setAdapter(contactoAdapter);
    }

    private void cargarCuentas() {
        cuentas.clear();
        cuentas.addAll(cuentaRepositorio.listar());
        ArrayAdapter<Cuenta> cuentaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cuentas);
        cuentaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCuenta.setAdapter(cuentaAdapter);
    }

    // Cuenta elegida en el Spinner, o null si no hay cuentas
    private Cuenta cuentaSeleccionada() {
        return (Cuenta) spCuenta.getSelectedItem();
    }

    private void seleccionarCuenta(long cuentaId) {
        for (int i = 0; i < cuentas.size(); i++) {
            if (cuentas.get(i).getId() == cuentaId) {
                spCuenta.setSelection(i);
                return;
            }
        }
    }

    // Carga una compra existente y su carrito para editarla
    private void cargarCompra() {
        Compra compra = compraRepositorio.obtenerCompra(editandoId);
        if (compra == null) {
            editandoId = 0;
            return;
        }
        etTitulo.setText(compra.getTitulo());
        rgEstado.check(compra.esAgendada() ? R.id.rbAgendada : R.id.rbRealizada);
        seleccionarContacto(compra.getContactoId());
        seleccionarCuenta(compra.getCuentaId());

        lugarNombre = compra.getLugarNombre() != null ? compra.getLugarNombre() : "";
        lugarLat = compra.getLat();
        lugarLon = compra.getLon();
        lugarTieneGeo = compra.isTieneGeo();

        todosProductos.clear();
        todosProductos.addAll(compraRepositorio.listarProductos(editandoId));
        refrescarProductos();

        if (!compra.esAgendada() && compra.getMontoReal() > 0) {
            etTotalReal.setText(String.format(Formato.CL, "%.0f", compra.getMontoReal()));
        }
    }

    private void seleccionarContacto(long contactoId) {
        for (int i = 0; i < contactos.size(); i++) {
            if (contactos.get(i).getId() == contactoId) {
                spContacto.setSelection(i);
                return;
            }
        }
        spContacto.setSelection(0);
    }

    // Carrito

    private void agregarProducto() {
        if (Validaciones.esVacio(etProductoNombre)) {
            etProductoNombre.setError(getString(R.string.agc_error_producto));
            return;
        }
        double precio = Validaciones.aDouble(etProductoPrecio);
        if (precio <= 0) {
            etProductoPrecio.setError(getString(R.string.acp_error_precio));
            return;
        }
        String nombre = etProductoNombre.getText().toString().trim();
        todosProductos.add(new CompraProducto(0, editandoId, nombre, precio, 0));
        refrescarProductos();
        irAUltimaPaginaProd();  // mostrar el producto recién agregado
        etProductoNombre.setText("");
        etProductoPrecio.setText("");
        etProductoNombre.requestFocus();
        actualizarTotalEstimado();
    }

    @Override
    public void onQuitar(int posicion) {
        if (posicion >= 0 && posicion < productos.size()) {
            todosProductos.remove(productos.get(posicion));
            refrescarProductos();
            actualizarTotalEstimado();
        }
    }

    // Recalcula las páginas del carrito y refresca la vista
    private void refrescarProductos() {
        paginadorProd.setDatos(todosProductos);
        mostrarPaginaProductos();
    }

    private void mostrarPaginaProductos() {
        productos.clear();
        productos.addAll(paginadorProd.paginaActual());
        productoAdapter.notifyDataSetChanged();
        boolean varias = paginadorProd.totalPaginas() > 1;
        barraPaginacionProd.setVisibility(varias ? View.VISIBLE : View.GONE);
        tvPaginaProd.setText(getString(R.string.paginador_pagina,
                paginadorProd.paginaHumana(), paginadorProd.totalPaginas()));
        btnPagAnteriorProd.setEnabled(paginadorProd.hayAnterior());
        btnPagSiguienteProd.setEnabled(paginadorProd.haySiguiente());
    }

    private void irAUltimaPaginaProd() {
        while (paginadorProd.haySiguiente()) {
            paginadorProd.siguiente();
        }
        mostrarPaginaProductos();
    }

    @Override
    public void onPrecioRealCambiado() {
        // El total real se autocompleta con la suma de los precios reales (editable luego)
        etTotalReal.setText(String.format(Formato.CL, "%.0f", sumaReales()));
    }

    private double sumaEstimados() {
        double total = 0;
        for (CompraProducto p : todosProductos) {
            total += p.getPrecioEstimado();
        }
        return total;
    }

    private double sumaReales() {
        double total = 0;
        for (CompraProducto p : todosProductos) {
            total += p.getPrecioReal();
        }
        return total;
    }

    private void actualizarTotalEstimado() {
        tvTotalEstimado.setText(String.format(Formato.CL,
                getString(R.string.acp_total_estimado), sumaEstimados()));
    }

    // Muestra el bloque de monto real solo cuando la compra está marcada como realizada
    private void aplicarModoReal() {
        boolean realizada = rgEstado.getCheckedRadioButtonId() == R.id.rbRealizada;
        bloqueReal.setVisibility(realizada ? View.VISIBLE : View.GONE);
        if (realizada) {
            for (CompraProducto p : todosProductos) {
                if (p.getPrecioReal() <= 0) {
                    p.setPrecioReal(p.getPrecioEstimado());
                }
            }
        }
        productoAdapter.setModoReal(realizada);
        if (realizada && etTotalReal.getText().toString().trim().isEmpty()) {
            etTotalReal.setText(String.format(Formato.CL, "%.0f", sumaReales()));
        }
    }

    // Lugar

    private void abrirSelectorUbicacion() {
        Intent intent = new Intent(this, SelectorUbicacionActivity.class);
        intent.putExtra(SelectorUbicacionActivity.EXTRA_NOMBRE, lugarNombre);
        intent.putExtra(SelectorUbicacionActivity.EXTRA_LAT, lugarLat);
        intent.putExtra(SelectorUbicacionActivity.EXTRA_LON, lugarLon);
        intent.putExtra(SelectorUbicacionActivity.EXTRA_TIENE_GEO, lugarTieneGeo);
        selectorUbicacion.launch(intent);
    }

    private void mostrarLugar() {
        if (lugarNombre != null && !lugarNombre.isEmpty()) {
            tvLugar.setText(lugarNombre);
        } else if (lugarTieneGeo) {
            tvLugar.setText(String.format(Locale.US, "%.5f, %.5f", lugarLat, lugarLon));
        } else {
            tvLugar.setText(R.string.acp_sin_lugar);
        }
    }

    // Guardar

    private void guardar() {
        if (Validaciones.esVacio(etTitulo)) {
            etTitulo.setError(getString(R.string.acp_error_titulo));
            return;
        }
        if (todosProductos.isEmpty()) {
            Toast.makeText(this, R.string.acp_falta_producto, Toast.LENGTH_SHORT).show();
            return;
        }

        String titulo = etTitulo.getText().toString().trim();
        boolean realizada = rgEstado.getCheckedRadioButtonId() == R.id.rbRealizada;
        String estado = realizada ? Compra.ESTADO_REALIZADA : Compra.ESTADO_AGENDADA;
        double montoEstimado = sumaEstimados();
        // Total real: lo escrito por el usuario
        double montoReal = realizada ? Validaciones.aDouble(etTotalReal) : 0;

        Contacto contacto = (Contacto) spContacto.getSelectedItem();
        long contactoId = (contacto != null) ? contacto.getId() : 0;

        Cuenta cuenta = cuentaSeleccionada();
        long cuentaId = (cuenta != null) ? cuenta.getId() : 0;

        long compraId;
        if (editandoId == 0) {
            String fecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            compraId = compraRepositorio.crearCompra(titulo, montoEstimado, montoReal, fecha,
                    estado, contactoId, lugarNombre, lugarLat, lugarLon, lugarTieneGeo, cuentaId);
        } else {
            compraId = editandoId;
            compraRepositorio.actualizarCompra(compraId, titulo, montoEstimado, montoReal,
                    estado, contactoId, lugarNombre, lugarLat, lugarLon, lugarTieneGeo, cuentaId);
            compraRepositorio.eliminarProductosDe(compraId);
        }

        // Regrabar el carrito. El precio real solo tiene sentido si la compra se realizó
        for (CompraProducto p : todosProductos) {
            double real = realizada ? p.getPrecioReal() : 0;
            compraRepositorio.agregarProducto(compraId, p.getNombre(), p.getPrecioEstimado(), real);
        }

        Toast.makeText(this,
                editandoId == 0 ? R.string.acp_guardada : R.string.acp_actualizada,
                Toast.LENGTH_SHORT).show();
        finish();
    }
}
