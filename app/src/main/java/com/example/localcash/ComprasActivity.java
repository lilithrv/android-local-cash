package com.example.localcash;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.adapter.CompraAdapter;
import com.example.localcash.datos.CompraRepositorio;
import com.example.localcash.datos.ContactoRepositorio;
import com.example.localcash.modelo.Compra;
import com.example.localcash.modelo.Contacto;
import com.example.localcash.util.Paginador;
import com.example.localcash.util.Validaciones;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

// Historial de compras y agendamiento de compras futuras, enlazables a un contacto
public class ComprasActivity extends BaseActivity {

    EditText etTitulo, etMonto;
    RadioGroup rgEstado;
    Spinner spContacto;
    Button btnAgregar, btnCancelar;
    RecyclerView rvCompras;

    // Id de la compra en edición
    long editandoId = 0;

    CompraRepositorio compraRepositorio;
    ContactoRepositorio contactoRepositorio;

    ArrayList<Compra> compras = new ArrayList<>();      // página visible
    ArrayList<Compra> todosCompras = new ArrayList<>(); // lista completa
    ArrayList<Contacto> contactos = new ArrayList<>();
    CompraAdapter adapter;
    final Paginador<Compra> paginador = new Paginador<>();

    LinearLayout barraPaginacion;
    Button btnPagAnterior, btnPagSiguiente;
    TextView tvPagina;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_compras);
        configurarBarra(getString(R.string.acp_titulo));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etTitulo = findViewById(R.id.etTitulo);
        etMonto = findViewById(R.id.etMonto);
        rgEstado = findViewById(R.id.rgEstado);
        spContacto = findViewById(R.id.spContacto);
        btnAgregar = findViewById(R.id.btnAgregar);
        btnCancelar = findViewById(R.id.btnCancelar);
        rvCompras = findViewById(R.id.rvCompras);

        compraRepositorio = new CompraRepositorio(this);
        contactoRepositorio = new ContactoRepositorio(this);

        adapter = new CompraAdapter(compras, this::eliminarCompra, this::editarCompra);
        rvCompras.setLayoutManager(new LinearLayoutManager(this));
        rvCompras.setAdapter(adapter);

        barraPaginacion = findViewById(R.id.barraPaginacion);
        btnPagAnterior = findViewById(R.id.btnPagAnterior);
        btnPagSiguiente = findViewById(R.id.btnPagSiguiente);
        tvPagina = findViewById(R.id.tvPagina);
        btnPagAnterior.setOnClickListener(v -> { paginador.anterior(); mostrarPagina(); });
        btnPagSiguiente.setOnClickListener(v -> { paginador.siguiente(); mostrarPagina(); });

        cargarContactos();
        cargarCompras();

        btnAgregar.setOnClickListener(v -> guardarCompra());
        btnCancelar.setOnClickListener(v -> cancelarEdicion());
    }

    private void cargarContactos() {
        contactos.clear();
        // Opción inicial para no enlazar contacto (id 0).
        contactos.add(new Contacto(0, getString(R.string.acp_sin_contacto), "", ""));
        contactos.addAll(contactoRepositorio.listar());
        ArrayAdapter<Contacto> contactoAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, contactos);
        contactoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spContacto.setAdapter(contactoAdapter);
    }

    private void cargarCompras() {
        todosCompras.clear();
        todosCompras.addAll(compraRepositorio.listar());
        paginador.setDatos(todosCompras);
        mostrarPagina();
    }

    // Muestra la página actual en la lista y actualiza la barra de paginación
    private void mostrarPagina() {
        compras.clear();
        compras.addAll(paginador.paginaActual());
        adapter.notifyDataSetChanged();
        boolean varias = paginador.totalPaginas() > 1;
        barraPaginacion.setVisibility(varias ? View.VISIBLE : View.GONE);
        tvPagina.setText(getString(R.string.paginador_pagina,
                paginador.paginaHumana(), paginador.totalPaginas()));
        btnPagAnterior.setEnabled(paginador.hayAnterior());
        btnPagSiguiente.setEnabled(paginador.haySiguiente());
    }

    private void guardarCompra() {
        if (Validaciones.esVacio(etTitulo)) {
            etTitulo.setError(getString(R.string.acp_error_titulo));
            return;
        }
        String titulo = etTitulo.getText().toString().trim();
        double monto = Validaciones.aDouble(etMonto);
        String estado = (rgEstado.getCheckedRadioButtonId() == R.id.rbAgendada)
                ? Compra.ESTADO_AGENDADA : Compra.ESTADO_REALIZADA;

        Contacto contacto = (Contacto) spContacto.getSelectedItem();
        long contactoId = (contacto != null) ? contacto.getId() : 0;

        if (editandoId == 0) {
            String fecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            compraRepositorio.agregar(titulo, monto, fecha, estado, contactoId);
        } else {
            compraRepositorio.actualizar(editandoId, titulo, monto, estado, contactoId);
            Toast.makeText(this, R.string.acp_actualizada, Toast.LENGTH_SHORT).show();
        }
        cancelarEdicion();
        cargarCompras();
    }

    private void editarCompra(Compra compra) {
        editandoId = compra.getId();
        etTitulo.setText(compra.getTitulo());
        etMonto.setText(String.format(Locale.getDefault(), "%.0f", compra.getMonto()));
        rgEstado.check(compra.esAgendada() ? R.id.rbAgendada : R.id.rbRealizada);
        seleccionarContacto(compra.getContactoId());
        btnAgregar.setText(R.string.acp_btnguardar_cambios);
        btnCancelar.setVisibility(View.VISIBLE);
        etTitulo.requestFocus();
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

    private void eliminarCompra(Compra compra) {
        compraRepositorio.eliminar(compra.getId());
        Toast.makeText(this, R.string.acp_eliminada, Toast.LENGTH_SHORT).show();
        cargarCompras();
    }

    // Limpiar formulario
    private void cancelarEdicion() {
        editandoId = 0;
        etTitulo.setText("");
        etMonto.setText("");
        etTitulo.setError(null);
        rgEstado.check(R.id.rbRealizada);
        spContacto.setSelection(0);
        btnAgregar.setText(R.string.acp_btnagregar);
        btnCancelar.setVisibility(View.GONE);
        etTitulo.requestFocus();
    }
}
