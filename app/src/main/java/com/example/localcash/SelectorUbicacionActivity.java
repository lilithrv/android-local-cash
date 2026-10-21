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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Locale;

// Permite fijar el lugar de una compra de tres formas: tocando el mapa, con el GPS,
// o buscando por nombre (geocodificación best-effort). Devuelve lat/lon/nombre
public class SelectorUbicacionActivity extends BaseActivity implements OnMapReadyCallback {

    public static final String EXTRA_NOMBRE = "NOMBRE";
    public static final String EXTRA_LAT = "LAT";
    public static final String EXTRA_LON = "LON";
    public static final String EXTRA_TIENE_GEO = "TIENE_GEO";

    // Centro por defecto (Santiago de Chile) cuando no llega un lugar previo
    private static final LatLng CENTRO_DEFECTO = new LatLng(-33.4489, -70.6693);

    GoogleMap map;
    EditText etNombre;
    Button btnBuscar, btnUsarUbicacion, btnGuardar;

    // Único marcador del lugar elegido (null si aún no hay coordenadas)
    Marker marcador;

    // true cuando el permiso se pidió al abrir (solo centrar la vista, sin poner marcador)
    // false cuando viene del botón "Usar mi ubicación" (sí coloca marcador)
    private boolean centrarAlConceder = false;

    private String nombreAuto = "";

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

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_selector_ubicacion);
        configurarBarra(getString(R.string.asu_titulo));

        etNombre = findViewById(R.id.etNombre);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnUsarUbicacion = findViewById(R.id.btnUsarUbicacion);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Lugar previo recibido (al reabrir para ajustar)
        String nombrePrevio = getIntent().getStringExtra(EXTRA_NOMBRE);
        if (nombrePrevio != null) {
            etNombre.setText(nombrePrevio);
        }

        btnBuscar.setOnClickListener(v -> buscarPorNombre());
        btnUsarUbicacion.setOnClickListener(v -> pedirUbicacion());
        btnGuardar.setOnClickListener(v -> guardarLugar());

        // El mapa se entrega de forma asíncrona: la configuración inicial va en onMapReady.
        SupportMapFragment fragmentMapa =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (fragmentMapa != null) {
            fragmentMapa.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);

        // Toque sobre el mapa: fija (o mueve) el marcador
        map.setOnMapClickListener(this::ponerMarcador);

        boolean tieneGeo = getIntent().getBooleanExtra(EXTRA_TIENE_GEO, false);
        if (tieneGeo) {
            // Ya había un lugar elegido: reabrir sobre ese punto
            LatLng previo = new LatLng(
                    getIntent().getDoubleExtra(EXTRA_LAT, 0),
                    getIntent().getDoubleExtra(EXTRA_LON, 0));
            ponerMarcador(previo);
        } else {
            // Lugar nuevo: centrar en la ubicación actual del usuario si está disponible
            // si no hay permiso/ubicación, usar Santiago como respaldo
            centrarEnUbicacionInicial();
        }
    }

    // Coloca (o mueve) el marcador y centra el mapa en ese punto
    private void ponerMarcador(LatLng punto) {
        if (map == null) {
            return;
        }
        if (marcador == null) {
            marcador = map.addMarker(new MarkerOptions().position(punto));
        } else {
            marcador.setPosition(punto);
        }
        map.animateCamera(CameraUpdateFactory.newLatLng(punto));
        // Resolver el nombre del lugar a partir de las coordenadas (en segundo plano).
        resolverNombre(punto);
    }

    // Geocodificación inversa: convierte coordenadas en un nombre de lugar y lo escribe en
    // el campo de nombre. Solo rellena si está vacío o si tenía un nombre puesto automáticamente

    private void resolverNombre(LatLng punto) {
        new Thread(() -> {
            String etiqueta = null;
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> resultados = geocoder.getFromLocation(
                        punto.latitude, punto.longitude, 1);
                if (resultados != null && !resultados.isEmpty()) {
                    etiqueta = describir(resultados.get(0));
                }
            } catch (Exception e) {
                etiqueta = null;
            }
            final String nombre = etiqueta;
            if (nombre == null || nombre.isEmpty()) {
                return;
            }
            runOnUiThread(() -> {
                String actual = etNombre.getText().toString().trim();
                if (actual.isEmpty() || actual.equals(nombreAuto)) {
                    etNombre.setText(nombre);
                    nombreAuto = nombre;
                }
            });
        }).start();
    }

    // Construye un nombre legible y corto a partir de una dirección
    private String describir(Address a) {
        String principal = a.getFeatureName();
        String via = a.getThoroughfare();
        String ciudad = (a.getLocality() != null) ? a.getLocality() : a.getSubAdminArea();
        if (principal == null || principal.matches("\\d+")) {
            principal = (via != null) ? via : principal;
        }
        if (principal == null) {
            return a.getAddressLine(0);
        }
        if (ciudad != null && !ciudad.equalsIgnoreCase(principal)) {
            return principal + ", " + ciudad;
        }
        return principal;
    }

    // Al abrir para un lugar nuevo: intenta partir en la ubicación del usuario. Si aún no hay
    // permiso, lo pide de entrada; mientras tanto centra en Santiago como respaldo
    private void centrarEnUbicacionInicial() {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(CENTRO_DEFECTO, 15f));
        if (UbicacionHelper.tienePermiso(this)) {
            centrarEnUbicacionActual();
        } else {
            centrarAlConceder = true;
            permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // Centra el mapa en la última ubicación conocida (sin colocar marcador).
    private void centrarEnUbicacionActual() {
        if (map == null) {
            return;
        }
        Location ubicacion = UbicacionHelper.ultimaUbicacion(this);
        if (ubicacion != null) {
            map.animateCamera(CameraUpdateFactory.newLatLng(
                    new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude())));
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
            ponerMarcador(new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude()));
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
            LatLng encontrado = null;
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> resultados = geocoder.getFromLocationName(texto, 1);
                if (resultados != null && !resultados.isEmpty()) {
                    Address a = resultados.get(0);
                    encontrado = new LatLng(a.getLatitude(), a.getLongitude());
                }
            } catch (Exception e) {
                // Sin red o sin servicio de geocodificación,  nombre queda como etiqueta manual
                encontrado = null;
            }
            final LatLng resultado = encontrado;
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
            datos.putExtra(EXTRA_LAT, marcador.getPosition().latitude);
            datos.putExtra(EXTRA_LON, marcador.getPosition().longitude);
        }
        setResult(RESULT_OK, datos);
        finish();
    }
}
