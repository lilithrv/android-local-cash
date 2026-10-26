package com.example.localcash;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.adapter.GastoAdapter;
import com.example.localcash.adapter.ProductoAdapter;
import com.example.localcash.datos.ContactoRepositorio;
import com.example.localcash.datos.CuentaRepositorio;
import com.example.localcash.datos.GastoCompartidoRepositorio;
import com.example.localcash.modelo.Contacto;
import com.example.localcash.modelo.Cuenta;
import com.example.localcash.modelo.GastoCompartido;
import com.example.localcash.modelo.GcParticipante;
import com.example.localcash.modelo.GcProducto;
import com.example.localcash.util.Formato;
import com.example.localcash.util.Paginador;
import com.example.localcash.util.Validaciones;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Crea un gasto compartido: agrega productos y selecciona participantes; reparte el total. Módulo 3.
public class GastoCompartidoActivity extends BaseActivity {

    EditText etTitulo, etProductoNombre, etProductoPrecio;
    Button btnAgregarProducto, btnGuardar;
    TextView tvTotal;
    LinearLayout llParticipantes;
    Spinner spCuenta;
    RecyclerView rvProductos, rvGastos;

    GastoCompartidoRepositorio gastoRepositorio;
    ContactoRepositorio contactoRepositorio;
    CuentaRepositorio cuentaRepositorio;

    // Cuentas disponibles para descontar el total del gasto
    ArrayList<Cuenta> cuentas = new ArrayList<>();

    // Productos que se van agregando antes de guardar (en memoria).
    ArrayList<GcProducto> productos = new ArrayList<>();
    ProductoAdapter productoAdapter;

    // Casillas de selección de participantes (una por contacto).
    ArrayList<Contacto> contactos = new ArrayList<>();
    ArrayList<CheckBox> casillas = new ArrayList<>();

    // Gastos ya guardados y su resumen de pago (id -> "2/3 pagaron · pendiente $X").
    ArrayList<GastoCompartido> gastos = new ArrayList<>();       // página visible
    ArrayList<GastoCompartido> todosGastos = new ArrayList<>();  // lista completa
    Map<Long, String> resumenesPago = new HashMap<>();
    GastoAdapter gastoAdapter;
    final Paginador<GastoCompartido> paginador = new Paginador<>();

    LinearLayout barraPaginacion;
    Button btnPagAnterior, btnPagSiguiente;
    TextView tvPagina;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gasto_compartido);
        configurarBarra(getString(R.string.agc_titulo));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etTitulo = findViewById(R.id.etTitulo);
        etProductoNombre = findViewById(R.id.etProductoNombre);
        etProductoPrecio = findViewById(R.id.etProductoPrecio);
        btnAgregarProducto = findViewById(R.id.btnAgregarProducto);
        btnGuardar = findViewById(R.id.btnGuardar);
        tvTotal = findViewById(R.id.tvTotal);
        llParticipantes = findViewById(R.id.llParticipantes);
        spCuenta = findViewById(R.id.spCuenta);
        rvProductos = findViewById(R.id.rvProductos);
        rvGastos = findViewById(R.id.rvGastos);

        gastoRepositorio = new GastoCompartidoRepositorio(this);
        contactoRepositorio = new ContactoRepositorio(this);
        cuentaRepositorio = new CuentaRepositorio(this);

        productoAdapter = new ProductoAdapter(productos, this::quitarProducto);
        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        rvProductos.setAdapter(productoAdapter);

        gastoAdapter = new GastoAdapter(gastos, this::abrirDeudas, resumenesPago);
        rvGastos.setLayoutManager(new LinearLayoutManager(this));
        rvGastos.setAdapter(gastoAdapter);

        barraPaginacion = findViewById(R.id.barraPaginacion);
        btnPagAnterior = findViewById(R.id.btnPagAnterior);
        btnPagSiguiente = findViewById(R.id.btnPagSiguiente);
        tvPagina = findViewById(R.id.tvPagina);
        btnPagAnterior.setOnClickListener(v -> { paginador.anterior(); mostrarPagina(); });
        btnPagSiguiente.setOnClickListener(v -> { paginador.siguiente(); mostrarPagina(); });

        cargarParticipantes();
        cargarCuentas();
        cargarGastos();
        actualizarTotal();

        btnAgregarProducto.setOnClickListener(v -> agregarProducto());
        btnGuardar.setOnClickListener(v -> guardarGasto());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarGastos();
    }

    // Crea una casilla (CheckBox) por cada contacto de la agenda.
    private void cargarParticipantes() {
        llParticipantes.removeAllViews();
        casillas.clear();
        contactos.clear();
        contactos.addAll(contactoRepositorio.listar());

        if (contactos.isEmpty()) {
            TextView aviso = new TextView(this);
            aviso.setText(R.string.agc_sin_contactos);
            aviso.setTextColor(getColor(R.color.lc_hint));
            llParticipantes.addView(aviso);
            return;
        }
        for (Contacto contacto : contactos) {
            CheckBox cb = new CheckBox(this);
            cb.setText(contacto.getNombre());
            cb.setTextColor(getColor(R.color.lc_text));
            llParticipantes.addView(cb);
            casillas.add(cb);
        }
    }

    private void cargarCuentas() {
        cuentas.clear();
        cuentas.addAll(cuentaRepositorio.listar());
        ArrayAdapter<Cuenta> cuentaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cuentas);
        cuentaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCuenta.setAdapter(cuentaAdapter);
    }

    private void agregarProducto() {
        if (Validaciones.esVacio(etProductoNombre)) {
            etProductoNombre.setError(getString(R.string.agc_error_producto));
            return;
        }
        double precio = Validaciones.aDouble(etProductoPrecio);
        if (precio <= 0) {
            etProductoPrecio.setError(getString(R.string.agc_error_precio));
            return;
        }
        String nombre = etProductoNombre.getText().toString().trim();
        productos.add(new GcProducto(0, 0, nombre, precio));
        productoAdapter.notifyDataSetChanged();
        etProductoNombre.setText("");
        etProductoPrecio.setText("");
        etProductoNombre.requestFocus();
        actualizarTotal();
    }

    private void quitarProducto(int posicion) {
        if (posicion >= 0 && posicion < productos.size()) {
            productos.remove(posicion);
            productoAdapter.notifyDataSetChanged();
            actualizarTotal();
        }
    }

    private double totalProductos() {
        double total = 0;
        for (GcProducto p : productos) {
            total += p.getPrecio();
        }
        return total;
    }

    private void actualizarTotal() {
        tvTotal.setText(String.format(Formato.CL, getString(R.string.agc_total), totalProductos()));
    }

    private void guardarGasto() {
        if (Validaciones.esVacio(etTitulo)) {
            etTitulo.setError(getString(R.string.agc_error_titulo));
            return;
        }
        if (productos.isEmpty()) {
            Toast.makeText(this, R.string.agc_falta_producto, Toast.LENGTH_SHORT).show();
            return;
        }

        // Participantes seleccionados.
        ArrayList<Contacto> seleccionados = new ArrayList<>();
        for (int i = 0; i < casillas.size(); i++) {
            if (casillas.get(i).isChecked()) {
                seleccionados.add(contactos.get(i));
            }
        }
        if (seleccionados.isEmpty()) {
            Toast.makeText(this, R.string.agc_falta_participante, Toast.LENGTH_SHORT).show();
            return;
        }

        String titulo = etTitulo.getText().toString().trim();
        double total = totalProductos();
        String fecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        // Cuenta de la que sale el total (el dueño paga por adelantado); 0 si no hay cuentas.
        Cuenta cuenta = (Cuenta) spCuenta.getSelectedItem();
        long cuentaId = (cuenta != null) ? cuenta.getId() : 0;

        // 1) Cabecera del gasto.
        long gastoId = gastoRepositorio.crearGasto(titulo, total, fecha, cuentaId);
        // 2) Productos.
        for (GcProducto p : productos) {
            gastoRepositorio.agregarProducto(gastoId, p.getNombre(), p.getPrecio());
        }
        // 3) Reparto equitativo inicial entre los participantes.
        double cuota = total / seleccionados.size();
        for (Contacto c : seleccionados) {
            gastoRepositorio.agregarParticipante(gastoId, c.getId(), cuota);
        }

        Toast.makeText(this, R.string.agc_guardado, Toast.LENGTH_SHORT).show();
        limpiarFormulario();
        cargarGastos();
        abrirDeudas(gastoRepositorio.obtenerGasto(gastoId));
    }

    private void limpiarFormulario() {
        etTitulo.setText("");
        productos.clear();
        productoAdapter.notifyDataSetChanged();
        for (CheckBox cb : casillas) {
            cb.setChecked(false);
        }
        actualizarTotal();
    }

    private void cargarGastos() {
        todosGastos.clear();
        todosGastos.addAll(gastoRepositorio.listarGastos());
        // Calcular el resumen de pago de cada gasto (cuántos pagaron y cuánto queda pendiente).
        resumenesPago.clear();
        for (GastoCompartido gasto : todosGastos) {
            ArrayList<GcParticipante> participantes = gastoRepositorio.listarParticipantes(gasto.getId());
            int total = participantes.size();
            int pagados = 0;
            double pendiente = 0;
            for (GcParticipante p : participantes) {
                if (p.isPagado()) {
                    pagados++;
                } else {
                    pendiente += p.getMontoAsignado();
                }
            }
            if (total > 0) {
                resumenesPago.put(gasto.getId(),
                        String.format(Formato.CL, getString(R.string.agc_resumen_pago), pagados, total, pendiente));
            }
        }
        paginador.setDatos(todosGastos);
        mostrarPagina();
    }

    // Muestra la página actual de gastos y actualiza la barra de paginación.
    private void mostrarPagina() {
        gastos.clear();
        gastos.addAll(paginador.paginaActual());
        gastoAdapter.notifyDataSetChanged();
        boolean varias = paginador.totalPaginas() > 1;
        barraPaginacion.setVisibility(varias ? View.VISIBLE : View.GONE);
        tvPagina.setText(getString(R.string.paginador_pagina,
                paginador.paginaHumana(), paginador.totalPaginas()));
        btnPagAnterior.setEnabled(paginador.hayAnterior());
        btnPagSiguiente.setEnabled(paginador.haySiguiente());
    }

    private void abrirDeudas(GastoCompartido gasto) {
        if (gasto == null) {
            return;
        }
        Intent intent = new Intent(this, DeudasActivity.class);
        intent.putExtra("GASTO_ID", gasto.getId());
        startActivity(intent);
    }
}
