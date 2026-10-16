package com.example.localcash;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.localcash.datos.PerfilRepositorio;
import com.example.localcash.util.Validaciones;

// Primer uso: crea el perfil local (nombre + PIN de respaldo)
public class SetupActivity extends AppCompatActivity {

    EditText etNombre, etPin;
    Button btnCrearPerfil;

    PerfilRepositorio perfilRepositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etNombre = findViewById(R.id.etNombre);
        etPin = findViewById(R.id.etPin);
        btnCrearPerfil = findViewById(R.id.btnCrearPerfil);

        perfilRepositorio = new PerfilRepositorio(this);

        btnCrearPerfil.setOnClickListener(v -> {
            if (Validaciones.esVacio(etNombre)) {
                etNombre.setError(getString(R.string.as_error_nombre));
                return;
            }
            String pin = etPin.getText().toString().trim();
            if (pin.length() < 4 || pin.length() > 6) {
                etPin.setError(getString(R.string.as_error_pin));
                return;
            }

            String nombre = etNombre.getText().toString().trim();
            perfilRepositorio.crearPerfil(nombre, pin, true);
            Toast.makeText(this, R.string.as_perfil_creado, Toast.LENGTH_SHORT).show();
            finish(); // Vuelve a MainActivity
        });
    }
}
