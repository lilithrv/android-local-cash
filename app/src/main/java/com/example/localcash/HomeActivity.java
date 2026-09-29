package com.example.localcash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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

        cuentaRepositorio = new CuentaRepositorio(this);
        perfilRepositorio = new PerfilRepositorio(this);

        btnCuentas.setOnClickListener(v -> abrir(CuentasActivity.class));
        btnMovimientos.setOnClickListener(v -> abrir(MovimientosActivity.class));
        btnMapa.setOnClickListener(v -> abrir(MapaActivity.class));
        btnCompras.setOnClickListener(v -> abrir(ComprasActivity.class));
        btnGastosCompartidos.setOnClickListener(v -> abrir(GastoCompartidoActivity.class));
        btnContactos.setOnClickListener(v -> abrir(ContactosActivity.class));
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
