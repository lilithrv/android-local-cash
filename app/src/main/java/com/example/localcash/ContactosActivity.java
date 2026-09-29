package com.example.localcash;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.adapter.ContactoAdapter;
import com.example.localcash.datos.ContactoRepositorio;
import com.example.localcash.modelo.Contacto;
import com.example.localcash.util.Paginador;
import com.example.localcash.util.Validaciones;

import java.util.ArrayList;

// Agenda local de contactos
public class ContactosActivity extends BaseActivity {

    EditText etNombre, etTelefono, etEmail;
    Button btnAgregar, btnCancelar;
    RecyclerView rvContactos;

    ContactoRepositorio contactoRepositorio;
    ArrayList<Contacto> contactos = new ArrayList<>();   // página visible
    ArrayList<Contacto> todosContactos = new ArrayList<>(); // lista completa
    ContactoAdapter adapter;
    final Paginador<Contacto> paginador = new Paginador<>();

    LinearLayout barraPaginacion;
    Button btnPagAnterior, btnPagSiguiente;
    TextView tvPagina;

    // Id del contacto en edición
    long editandoId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contactos);
        configurarBarra(getString(R.string.acon_titulo));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etNombre = findViewById(R.id.etNombre);
        etTelefono = findViewById(R.id.etTelefono);
        etEmail = findViewById(R.id.etEmail);
        btnAgregar = findViewById(R.id.btnAgregar);
        btnCancelar = findViewById(R.id.btnCancelar);
        rvContactos = findViewById(R.id.rvContactos);

        contactoRepositorio = new ContactoRepositorio(this);

        adapter = new ContactoAdapter(contactos, this::eliminarContacto, this::editarContacto);
        rvContactos.setLayoutManager(new LinearLayoutManager(this));
        rvContactos.setAdapter(adapter);

        barraPaginacion = findViewById(R.id.barraPaginacion);
        btnPagAnterior = findViewById(R.id.btnPagAnterior);
        btnPagSiguiente = findViewById(R.id.btnPagSiguiente);
        tvPagina = findViewById(R.id.tvPagina);
        btnPagAnterior.setOnClickListener(v -> { paginador.anterior(); mostrarPagina(); });
        btnPagSiguiente.setOnClickListener(v -> { paginador.siguiente(); mostrarPagina(); });

        cargarContactos();

        btnAgregar.setOnClickListener(v -> guardarContacto());
        btnCancelar.setOnClickListener(v -> cancelarEdicion());
    }

    private void guardarContacto() {
        if (Validaciones.esVacio(etNombre)) {
            etNombre.setError(getString(R.string.acon_error_nombre));
            return;
        }
        // El email es opcional, pero si se escribe debe ser válido
        if (!Validaciones.esVacio(etEmail) && Validaciones.esEmailInvalido(etEmail)) {
            etEmail.setError(getString(R.string.acon_error_email));
            return;
        }
        String nombre = etNombre.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (editandoId == 0) {
            contactoRepositorio.agregar(nombre, telefono, email);
        } else {
            contactoRepositorio.actualizar(editandoId, nombre, telefono, email);
            Toast.makeText(this, R.string.acon_actualizado, Toast.LENGTH_SHORT).show();
        }
        cancelarEdicion();
        cargarContactos();
    }

    // Cargar contacto en formulario, modo edición
    private void editarContacto(Contacto contacto) {
        editandoId = contacto.getId();
        etNombre.setText(contacto.getNombre());
        etTelefono.setText(contacto.getTelefono());
        etEmail.setText(contacto.getEmail());
        btnAgregar.setText(R.string.acon_btnguardar_cambios);
        btnCancelar.setVisibility(View.VISIBLE);
        etNombre.requestFocus();
    }

    private void cargarContactos() {
        todosContactos.clear();
        todosContactos.addAll(contactoRepositorio.listar());
        paginador.setDatos(todosContactos);
        mostrarPagina();
    }

    // Muestra la página actual en la lista y actualiza la barra de paginación
    private void mostrarPagina() {
        contactos.clear();
        contactos.addAll(paginador.paginaActual());
        adapter.notifyDataSetChanged();
        boolean varias = paginador.totalPaginas() > 1;
        barraPaginacion.setVisibility(varias ? View.VISIBLE : View.GONE);
        tvPagina.setText(getString(R.string.paginador_pagina,
                paginador.paginaHumana(), paginador.totalPaginas()));
        btnPagAnterior.setEnabled(paginador.hayAnterior());
        btnPagSiguiente.setEnabled(paginador.haySiguiente());
    }

    private void eliminarContacto(Contacto contacto) {
        contactoRepositorio.eliminar(contacto.getId());
        Toast.makeText(this, R.string.acon_eliminado, Toast.LENGTH_SHORT).show();
        cargarContactos();
    }

    // Limpiar formulario
    private void cancelarEdicion() {
        editandoId = 0;
        etNombre.setText("");
        etTelefono.setText("");
        etEmail.setText("");
        etNombre.setError(null);
        etEmail.setError(null);
        btnAgregar.setText(R.string.acon_btnagregar);
        btnCancelar.setVisibility(View.GONE);
        etNombre.requestFocus();
    }
}
