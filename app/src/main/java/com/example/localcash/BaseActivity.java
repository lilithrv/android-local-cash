package com.example.localcash;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

// flecha "volver" reutilizable en las pantallas secundarias, para no depender solo del botón del teléfono
public abstract class BaseActivity extends AppCompatActivity {

    // Configura toolbar
    protected void configurarBarra(String titulo) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(titulo);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    // flecha cierra la pantalla actual y vuelve a la anterior
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
