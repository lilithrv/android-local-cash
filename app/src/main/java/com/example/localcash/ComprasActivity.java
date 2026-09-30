package com.example.localcash;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.adapter.CompraAdapter;
import com.example.localcash.datos.CompraRepositorio;
import com.example.localcash.modelo.Compra;
import com.example.localcash.util.Paginador;

import java.util.ArrayList;

// Lista de compras (historial y agendadas)
public class ComprasActivity extends BaseActivity {

    public static final String EXTRA_COMPRA_ID = "COMPRA_ID";

    Button btnNueva;
    RecyclerView rvCompras;

    CompraRepositorio compraRepositorio;

    ArrayList<Compra> compras = new ArrayList<>();      // página visible
    ArrayList<Compra> todosCompras = new ArrayList<>(); // lista completa
    CompraAdapter adapter;
    final Paginador<Compra> paginador = new Paginador<>();

    android.widget.LinearLayout barraPaginacion;
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

        btnNueva = findViewById(R.id.btnNueva);
        rvCompras = findViewById(R.id.rvCompras);

        compraRepositorio = new CompraRepositorio(this);

        adapter = new CompraAdapter(compras, this::eliminarCompra, this::editarCompra);
        rvCompras.setLayoutManager(new LinearLayoutManager(this));
        rvCompras.setAdapter(adapter);

        barraPaginacion = findViewById(R.id.barraPaginacion);
        btnPagAnterior = findViewById(R.id.btnPagAnterior);
        btnPagSiguiente = findViewById(R.id.btnPagSiguiente);
        tvPagina = findViewById(R.id.tvPagina);
        btnPagAnterior.setOnClickListener(v -> { paginador.anterior(); mostrarPagina(); });
        btnPagSiguiente.setOnClickListener(v -> { paginador.siguiente(); mostrarPagina(); });

        btnNueva.setOnClickListener(v -> abrirDetalle(0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar al volver del detalle (nueva compra o edición)
        cargarCompras();
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

    private void abrirDetalle(long compraId) {
        Intent intent = new Intent(this, CompraDetalleActivity.class);
        if (compraId > 0) {
            intent.putExtra(EXTRA_COMPRA_ID, compraId);
        }
        startActivity(intent);
    }

    private void editarCompra(Compra compra) {
        abrirDetalle(compra.getId());
    }

    private void eliminarCompra(Compra compra) {
        compraRepositorio.eliminar(compra.getId());
        Toast.makeText(this, R.string.acp_eliminada, Toast.LENGTH_SHORT).show();
        cargarCompras();
    }
}
