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

import com.example.localcash.datos.CuentaRepositorio;
import com.example.localcash.datos.PerfilRepositorio;
import com.example.localcash.modelo.Perfil;
import com.example.localcash.util.Formato;

// Panel principal: saldo total y accesos a cada módulo
public class HomeActivity extends AppCompatActivity {

    TextView tvSaludo, tvSaldoTotal;
    Button btnCuentas, btnMovimientos, btnMapa, btnCompras, btnGastosCompartidos, btnContactos;
    Button btnReiniciarPin;

    CuentaRepositorio cuentaRepositorio;
    PerfilRepositorio perfilRepositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvSaludo = findViewById(R.id.tvSaludo);
        tvSaldoTotal = findViewById(R.id.tvSaldoTotal);
        btnCuentas = findViewById(R.id.btnCuentas);
        btnMovimientos = findViewById(R.id.btnMovimientos);
        btnMapa = findViewById(R.id.btnMapa);
        btnCompras = findViewById(R.id.btnCompras);
        btnGastosCompartidos = findViewById(R.id.btnGastosCompartidos);
        btnContactos = findViewById(R.id.btnContactos);
        btnReiniciarPin = findViewById(R.id.btnReiniciarPin);

        cuentaRepositorio = new CuentaRepositorio(this);
        perfilRepositorio = new PerfilRepositorio(this);

        btnCuentas.setOnClickListener(v -> abrir(CuentasActivity.class));
        btnMovimientos.setOnClickListener(v -> abrir(MovimientosActivity.class));
        btnMapa.setOnClickListener(v -> abrir(MapaActivity.class));
        btnCompras.setOnClickListener(v -> abrir(ComprasActivity.class));
        btnGastosCompartidos.setOnClickListener(v -> abrir(GastoCompartidoActivity.class));
        btnContactos.setOnClickListener(v -> abrir(ContactosActivity.class));
        btnReiniciarPin.setOnClickListener(v -> dialogReiniciarPin());
    }

    // Pide un PIN nuevo y lo guarda hasheado. Solo cambia el PIN; el resto de los datos queda igual.
    private void dialogReiniciarPin() {
        EditText etPin = new EditText(this);
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        etPin.setHint(R.string.ah_reiniciarpin_hint);

        new AlertDialog.Builder(this)
                .setTitle(R.string.ah_reiniciarpin_titulo)
                .setView(etPin)
                .setPositiveButton(R.string.ah_reiniciarpin_ok, (dialog, which) -> {
                    String nuevoPin = etPin.getText().toString().trim();
                    if (nuevoPin.length() < 4 || nuevoPin.length() > 6) {
                        Toast.makeText(this, R.string.ah_reiniciarpin_error, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    perfilRepositorio.reiniciarPin(nuevoPin);
                    Toast.makeText(this, R.string.ah_reiniciarpin_hecho, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.al_pin_cancelar, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Perfil perfil = perfilRepositorio.obtenerPerfil();
        String nombre = (perfil != null) ? perfil.getNombre() : "";
        tvSaludo.setText(getString(R.string.ah_saludo, nombre));

        double total = cuentaRepositorio.saldoTotal();
        tvSaldoTotal.setText(String.format(Formato.CL, getString(R.string.ah_saldo_total), total));
    }

    private void abrir(Class<?> destino) {
        startActivity(new Intent(this, destino));
    }
}
