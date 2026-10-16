package com.example.localcash;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.localcash.datos.PerfilRepositorio;
import com.example.localcash.util.BiometriaHelper;

// Pantalla de acceso: crear perfil o desbloquear con biometría
public class MainActivity extends AppCompatActivity {

    TextView tvEstado;
    Button btnDesbloquear;

    PerfilRepositorio perfilRepositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvEstado = findViewById(R.id.tvEstado);
        btnDesbloquear = findViewById(R.id.btnDesbloquear);

        perfilRepositorio = new PerfilRepositorio(this);

        btnDesbloquear.setOnClickListener(v -> iniciarFlujoAcceso());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si aún no hay perfil, se envía directamente al Setup.
        if (!perfilRepositorio.existePerfil()) {
            tvEstado.setText(R.string.al_sin_perfil);
            startActivity(new Intent(this, SetupActivity.class));
        } else {
            tvEstado.setText(R.string.al_con_perfil);
        }
    }

    // Lanza la biometría; si no hay hardware disponible, usa el PIN de respaldo del perfil
    private void iniciarFlujoAcceso() {
        if (!perfilRepositorio.existePerfil()) {
            startActivity(new Intent(this, SetupActivity.class));
            return;
        }

        if (BiometriaHelper.hayBiometriaDisponible(this)) {
            BiometriaHelper.autenticar(this, new BiometriaHelper.ResultadoBiometria() {
                @Override
                public void onExito() {
                    entrarAHome();
                }

                @Override
                public void onError(String mensaje) {
                    // El usuario tocó "Usar PIN de respaldo" o la huella falló: se pide el PIN
                    pedirPin();
                }
            });
        } else {
            // No hay huella registrada en el dispositivo: se usa el PIN de respaldo del perfil
            Toast.makeText(this, R.string.al_sin_biometria, Toast.LENGTH_LONG).show();
            pedirPin();
        }
    }

    // Diálogo de respaldo: valida el PIN creado en el Setup contra la base de datos
    private void pedirPin() {
        EditText etPin = new EditText(this);
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        etPin.setHint(R.string.al_pin_hint);

        new AlertDialog.Builder(this)
                .setTitle(R.string.al_pin_titulo)
                .setView(etPin)
                .setPositiveButton(R.string.al_pin_ok, (dialog, which) -> {
                    String pin = etPin.getText().toString().trim();
                    if (perfilRepositorio.validarPin(pin)) {
                        entrarAHome();
                    } else {
                        Toast.makeText(this, R.string.al_pin_incorrecto, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.al_pin_cancelar, null)
                .show();
    }

    private void entrarAHome() {
        startActivity(new Intent(MainActivity.this, HomeActivity.class));
    }
}
