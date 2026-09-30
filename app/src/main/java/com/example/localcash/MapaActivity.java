package com.example.localcash;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.localcash.datos.CompraRepositorio;
import com.example.localcash.datos.MovimientoRepositorio;
import com.example.localcash.modelo.Compra;
import com.example.localcash.modelo.Movimiento;
import com.example.localcash.util.Formato;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;

// Mapa (osmdroid) con un marcador por cada movimiento geolocalizado.
public class
MapaActivity extends BaseActivity {

    // Centro por defecto (Santiago de Chile) cuando aún no hay movimientos con ubicación
    private static final GeoPoint CENTRO_DEFECTO = new GeoPoint(-33.4489, -70.6693);

    MapView map;
    TextView tvVacio;

    MovimientoRepositorio movimientoRepositorio;
    CompraRepositorio compraRepositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // osmdroid: cargar configuración (lee las SharedPreferences).
        Configuration.getInstance().setUserAgentValue("Mapa / (lelsiev@gmail.com)");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mapa);
        configurarBarra(getString(R.string.amap_titulo));

        map = findViewById(R.id.map);
        tvVacio = findViewById(R.id.tvVacio);

        Configuration.getInstance().setUserAgentValue("LocalCash/1.0 (lesliev@gmail.com)");

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(13.0);

        movimientoRepositorio = new MovimientoRepositorio(this);
        compraRepositorio = new CompraRepositorio(this);
        pintarMarcadores();
    }

    private void pintarMarcadores() {
        ArrayList<Movimiento> movConGeo = movimientoRepositorio.listarConGeo();
        ArrayList<Compra> comprasConGeo = compraRepositorio.listarConGeo();
        tvVacio.setVisibility(movConGeo.isEmpty() && comprasConGeo.isEmpty() ? View.VISIBLE : View.GONE);

        GeoPoint primero = null;

        // Marcadores de movimientos (ingresos/egresos)
        for (Movimiento mov : movConGeo) {
            GeoPoint punto = new GeoPoint(mov.getLat(), mov.getLon());
            if (primero == null) {
                primero = punto;
            }
            Marker marcador = new Marker(map);
            marcador.setPosition(punto);
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            String signo = mov.esIngreso() ? "+" : "-";
            marcador.setTitle(mov.getDescripcion());
            marcador.setSnippet(String.format(Formato.CL, "%s$%,.0f  ·  %s",
                    signo, mov.getMonto(), mov.getFecha()));
            map.getOverlays().add(marcador);
        }

        // Marcadores de compras con ubicación (realizadas y agendadas)
        for (Compra compra : comprasConGeo) {
            GeoPoint punto = new GeoPoint(compra.getLat(), compra.getLon());
            if (primero == null) {
                primero = punto;
            }
            Marker marcador = new Marker(map);
            marcador.setPosition(punto);
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marcador.setTitle("🛒 " + compra.getTitulo());
            String estado = compra.esAgendada() ? "Agendada" : "Realizada";
            marcador.setSnippet(String.format(Formato.CL, "%s  ·  $%,.0f  ·  %s",
                    estado, compra.getMontoVigente(), compra.getFecha()));
            map.getOverlays().add(marcador);
        }

        map.getController().setCenter(primero != null ? primero : CENTRO_DEFECTO);
    }

    // El ciclo de vida del MapView de osmdroid debe reflejar el de la Activity
    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}
