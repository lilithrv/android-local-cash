package com.example.localcash;

import android.Manifest;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.localcash.util.UbicacionHelper;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.Locale;

// Permite fijar el lugar de una compra de tres formas: tocando el mapa, con el GPS,
// o buscando por nombre (geocodificación best-effort). Devuelve lat/lon/nombre
public class SelectorUbicacionActivity extends BaseActivity {

    public static final String EXTRA_NOMBRE = "NOMBRE";
    public static final String EXTRA_LAT = "LAT";
    public static final String EXTRA_LON = "LON";
    public static final String EXTRA_TIENE_GEO = "TIENE_GEO";

    // Centro por defecto (Santiago de Chile) cuando no llega un lugar previo
    private static final GeoPoint CENTRO_DEFECTO = new GeoPoint(-33.4489, -70.6693);

    MapView map;
    EditText etNombre;
    Button btnBuscar, btnUsarUbicacion, btnGuardar;

    // Único marcador del lugar elegido (null si aún no hay coordenadas)
    Marker marcador;

    // true cuando el permiso se pidió al abrir (solo centrar la vista, sin poner marcador)
    // false cuando viene del botón "Usar mi ubicación" (sí coloca marcador)
    private boolean centrarAlConceder = false;

    // Lanzador para pedir el permiso de ubicación en tiempo de ejecución
    private final ActivityResultLauncher<String> permisoUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    if (centrarAlConceder) {
                        centrarEnUbicacionActual();
                    } else {
                        usarUbicacionActual();
                    }
                } else if (!centrarAlConceder) {
                    // Solo avisar si el usuario pidió explícitamente su ubicación
                    Toast.makeText(this, R.string.amv_permiso_denegado, Toast.LENGTH_SHORT).show();
                }
                centrarAlConceder = false;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue("Mapa / (lelsiev@gmail.com)");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_selector_ubicacion);
        configurarBarra(getString(R.string.asu_titulo));

        map = findViewById(R.id.map);
        etNombre = findViewById(R.id.etNombre);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnUsarUbicacion = findViewById(R.id.btnUsarUbicacion);
        btnGuardar = findViewById(R.id.btnGuardar);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        // Overlay que recibe los toques sobre el mapa
        MapEventsOverlay eventos = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                ponerMarcador(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });
        map.getOverlays().add(eventos);

        // Lugar previo recibido (al reabrir para ajustar)
        String nombrePrevio = getIntent().getStringExtra(EXTRA_NOMBRE);
        if (nombrePrevio != null) {
            etNombre.setText(nombrePrevio);
        }
        boolean tieneGeo = getIntent().getBooleanExtra(EXTRA_TIENE_GEO, false);
        if (tieneGeo) {
            // Ya había un lugar elegido: reabrir sobre ese punto
            GeoPoint previo = new GeoPoint(
                    getIntent().getDoubleExtra(EXTRA_LAT, 0),
                    getIntent().getDoubleExtra(EXTRA_LON, 0));
            ponerMarcador(previo);
        } else {
            // Lugar nuevo: centrar en la ubicación actual del usuario si está disponible
            // si no hay permiso/ubicación, usar Santiago como respaldo
            centrarEnUbicacionInicial();
        }

        btnBuscar.setOnClickListener(v -> buscarPorNombre());
        btnUsarUbicacion.setOnClickListener(v -> pedirUbicacion());
        btnGuardar.setOnClickListener(v -> guardarLugar());
    }

    // Coloca (o mueve) el marcador y centra el mapa en ese punto
    private void ponerMarcador(GeoPoint punto) {
        if (marcador == null) {
            marcador = new Marker(map);
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(marcador);
        }
        marcador.setPosition(punto);
        map.getController().setCenter(punto);
        map.invalidate();
    }

    // Al abrir para un lugar nuevo: intenta partir en la ubicación del usuario. Si aún no hay
    // permiso, lo pide de entrada; mientras tanto centra en Santiago como respaldo
    private void centrarEnUbicacionInicial() {
        map.getController().setCenter(CENTRO_DEFECTO);
        if (UbicacionHelper.tienePermiso(this)) {
            centrarEnUbicacionActual();
        } else {
            centrarAlConceder = true;
            permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // Centra el mapa en la última ubicación conocida (sin colocar marcador).
    private void centrarEnUbicacionActual() {
        Location ubicacion = UbicacionHelper.ultimaUbicacion(this);
        if (ubicacion != null) {
            map.getController().setCenter(new GeoPoint(ubicacion.getLatitude(), ubicacion.getLongitude()));
        }
    }

    // GPS

    private void pedirUbicacion() {
        if (UbicacionHelper.tienePermiso(this)) {
            usarUbicacionActual();
        } else {
            permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void usarUbicacionActual() {
        Location ubicacion = UbicacionHelper.ultimaUbicacion(this);
        if (ubicacion != null) {
            ponerMarcador(new GeoPoint(ubicacion.getLatitude(), ubicacion.getLongitude()));
        } else {
            Toast.makeText(this, R.string.asu_sin_ubicacion, Toast.LENGTH_LONG).show();
        }
    }

    // Búsqueda por nombre (geocodificación best-effort)

    private void buscarPorNombre() {
        String texto = etNombre.getText().toString().trim();
        if (texto.isEmpty()) {
            return;
        }
        Toast.makeText(this, R.string.asu_buscando, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            GeoPoint encontrado = null;
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> resultados = geocoder.getFromLocationName(texto, 1);
                if (resultados != null && !resultados.isEmpty()) {
                    Address a = resultados.get(0);
                    encontrado = new GeoPoint(a.getLatitude(), a.getLongitude());
                }
            } catch (Exception e) {
                // Sin red o sin servicio de geocodificación,  nombre queda como etiqueta manual
                encontrado = null;
            }
            final GeoPoint resultado = encontrado;
            runOnUiThread(() -> {
                if (resultado != null) {
                    ponerMarcador(resultado);
                } else {
                    Toast.makeText(this, R.string.asu_sin_resultado, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // Guardar y devolver

    private void guardarLugar() {
        String nombre = etNombre.getText().toString().trim();
        boolean tieneGeo = marcador != null;
        if (!tieneGeo && nombre.isEmpty()) {
            Toast.makeText(this, R.string.asu_sin_marcador, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent datos = new Intent();
        datos.putExtra(EXTRA_NOMBRE, nombre);
        datos.putExtra(EXTRA_TIENE_GEO, tieneGeo);
        if (tieneGeo) {
            datos.putExtra(EXTRA_LAT, marcador.getPosition().getLatitude());
            datos.putExtra(EXTRA_LON, marcador.getPosition().getLongitude());
        }
        setResult(RESULT_OK, datos);
        finish();
    }

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
