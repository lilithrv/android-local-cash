package com.example.localcash;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

import com.example.localcash.adapter.CuentaAdapter;
import com.example.localcash.datos.CuentaRepositorio;
import com.example.localcash.modelo.Cuenta;
import com.example.localcash.util.Paginador;
import com.example.localcash.util.Validaciones;

import java.util.ArrayList;

public class CuentasActivity extends BaseActivity {

    EditText etNombre, etSaldoInicial;
    Spinner spTipo;
    Button btnAgregar, btnCancelar;
    RecyclerView rvCuentas;

    CuentaRepositorio cuentaRepositorio;
    ArrayList<Cuenta> cuentas = new ArrayList<>();      // página visible
    ArrayList<Cuenta> todosCuentas = new ArrayList<>(); // lista completa
    CuentaAdapter adapter;
    final Paginador<Cuenta> paginador = new Paginador<>();

    LinearLayout barraPaginacion;
    Button btnPagAnterior, btnPagSiguiente;
    TextView tvPagina;

    // Id de la cuenta en edición
    long editandoId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cuentas);
        configurarBarra(getString(R.string.ac_titulo));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etNombre = findViewById(R.id.etNombre);
        etSaldoInicial = findViewById(R.id.etSaldoInicial);
        spTipo = findViewById(R.id.spTipo);
        btnAgregar = findViewById(R.id.btnAgregar);
        btnCancelar = findViewById(R.id.btnCancelar);
        rvCuentas = findViewById(R.id.rvCuentas);

        cuentaRepositorio = new CuentaRepositorio(this);

        // Spinner con los tipos de cuenta definidos en arrays.xml
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.ac_tipos_cuenta, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTipo.setAdapter(spinnerAdapter);

        adapter = new CuentaAdapter(cuentas, this::eliminarCuenta, this::editarCuenta);
        rvCuentas.setLayoutManager(new LinearLayoutManager(this));
        rvCuentas.setAdapter(adapter);

        barraPaginacion = findViewById(R.id.barraPaginacion);
        btnPagAnterior = findViewById(R.id.btnPagAnterior);
        btnPagSiguiente = findViewById(R.id.btnPagSiguiente);
        tvPagina = findViewById(R.id.tvPagina);
        btnPagAnterior.setOnClickListener(v -> { paginador.anterior(); mostrarPagina(); });
        btnPagSiguiente.setOnClickListener(v -> { paginador.siguiente(); mostrarPagina(); });

        cargarCuentas();

        btnAgregar.setOnClickListener(v -> guardarCuenta());
        btnCancelar.setOnClickListener(v -> cancelarEdicion());
    }

    private void guardarCuenta() {
        if (Validaciones.esVacio(etNombre)) {
            etNombre.setError(getString(R.string.ac_error_nombre));
            return;
        }
        String nombre = etNombre.getText().toString().trim();
        String tipo = spTipo.getSelectedItem().toString();
        double saldoInicial = Validaciones.aDouble(etSaldoInicial);

        if (editandoId == 0) {
            cuentaRepositorio.agregar(nombre, tipo, saldoInicial);
        } else {
            cuentaRepositorio.actualizar(editandoId, nombre, tipo, saldoInicial);
            Toast.makeText(this, R.string.ac_actualizada, Toast.LENGTH_SHORT).show();
        }
        cancelarEdicion();
        cargarCuentas();
    }

    // Carga una cuenta en el formulario y pasa a modo edición
    private void editarCuenta(Cuenta cuenta) {
        editandoId = cuenta.getId();
        etNombre.setText(cuenta.getNombre());
        etSaldoInicial.setText(String.format(java.util.Locale.getDefault(), "%.0f", cuenta.getSaldoInicial()));
        seleccionarTipo(cuenta.getTipo());
        btnAgregar.setText(R.string.ac_btnguardar_cambios);
        btnCancelar.setVisibility(View.VISIBLE);
        etNombre.requestFocus();
    }

    // Posiciona el Spinner en el tipo guardado
    private void seleccionarTipo(String tipo) {
        for (int i = 0; i < spTipo.getCount(); i++) {
            if (spTipo.getItemAtPosition(i).toString().equals(tipo)) {
                spTipo.setSelection(i);
                return;
            }
        }
    }

    private void cargarCuentas() {
        todosCuentas.clear();
        todosCuentas.addAll(cuentaRepositorio.listar());
        paginador.setDatos(todosCuentas);
        mostrarPagina();
    }

    // Muestra la página actual en la lista y actualiza la barra de paginación
    private void mostrarPagina() {
        cuentas.clear();
        cuentas.addAll(paginador.paginaActual());
        adapter.notifyDataSetChanged();
        boolean varias = paginador.totalPaginas() > 1;
        barraPaginacion.setVisibility(varias ? View.VISIBLE : View.GONE);
        tvPagina.setText(getString(R.string.paginador_pagina,
                paginador.paginaHumana(), paginador.totalPaginas()));
        btnPagAnterior.setEnabled(paginador.hayAnterior());
        btnPagSiguiente.setEnabled(paginador.haySiguiente());
    }

    private void eliminarCuenta(Cuenta cuenta) {
        cuentaRepositorio.eliminar(cuenta.getId());
        Toast.makeText(this, R.string.ac_eliminada, Toast.LENGTH_SHORT).show();
        cargarCuentas();
    }

    // Limpiar formulario
    private void cancelarEdicion() {
        editandoId = 0;
        etNombre.setText("");
        etSaldoInicial.setText("");
        etNombre.setError(null);
        spTipo.setSelection(0);
        btnAgregar.setText(R.string.ac_btnagregar);
        btnCancelar.setVisibility(View.GONE);
        etNombre.requestFocus();
    }
}
