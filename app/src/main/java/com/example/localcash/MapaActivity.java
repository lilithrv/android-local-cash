package com.example.localcash;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;

import com.example.localcash.datos.CompraRepositorio;
import com.example.localcash.datos.MovimientoRepositorio;
import com.example.localcash.modelo.Compra;
import com.example.localcash.modelo.Movimiento;
import com.example.localcash.util.Formato;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

// Mapa (Google Maps) con un marcador por cada movimiento geolocalizado.
public class MapaActivity extends BaseActivity implements OnMapReadyCallback {

    // Centro por defecto (Santiago de Chile) cuando aún no hay movimientos con ubicación
    private static final LatLng CENTRO_DEFECTO = new LatLng(-33.4489, -70.6693);

    TextView tvVacio;

    MovimientoRepositorio movimientoRepositorio;
    CompraRepositorio compraRepositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mapa);
        configurarBarra(getString(R.string.amap_titulo));

        tvVacio = findViewById(R.id.tvVacio);

        movimientoRepositorio = new MovimientoRepositorio(this);
        compraRepositorio = new CompraRepositorio(this);

        // El mapa se entrega de forma asíncrona: se pintan los marcadores en onMapReady.
        SupportMapFragment fragmentMapa =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (fragmentMapa != null) {
            fragmentMapa.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        pintarMarcadores(map);
    }

    private void pintarMarcadores(GoogleMap map) {
        ArrayList<Movimiento> movConGeo = movimientoRepositorio.listarConGeo();
        ArrayList<Compra> comprasConGeo = compraRepositorio.listarConGeo();
        tvVacio.setVisibility(movConGeo.isEmpty() && comprasConGeo.isEmpty() ? View.VISIBLE : View.GONE);

        LatLng primero = null;

        // Marcadores de movimientos (ingresos/egresos)
        for (Movimiento mov : movConGeo) {
            LatLng punto = new LatLng(mov.getLat(), mov.getLon());
            if (primero == null) {
                primero = punto;
            }
            String signo = mov.esIngreso() ? "+" : "-";
            map.addMarker(new MarkerOptions()
                    .position(punto)
                    .title(mov.getDescripcion())
                    .snippet(String.format(Formato.CL, "%s$%,.0f  ·  %s",
                            signo, mov.getMonto(), mov.getFecha())));
        }

        // Marcadores de compras con ubicación (realizadas y agendadas)
        for (Compra compra : comprasConGeo) {
            LatLng punto = new LatLng(compra.getLat(), compra.getLon());
            if (primero == null) {
                primero = punto;
            }
            String estado = compra.esAgendada() ? "Agendada" : "Realizada";
            map.addMarker(new MarkerOptions()
                    .position(punto)
                    .title("🛒 " + compra.getTitulo())
                    .snippet(String.format(Formato.CL, "%s  ·  $%,.0f  ·  %s",
                            estado, compra.getMontoVigente(), compra.getFecha())));
        }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                primero != null ? primero : CENTRO_DEFECTO, 13f));
    }
}
