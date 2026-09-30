package com.example.localcash;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.localcash.datos.ContactoRepositorio;
import com.example.localcash.datos.GastoCompartidoRepositorio;
import com.example.localcash.datos.PerfilRepositorio;
import com.example.localcash.modelo.Contacto;
import com.example.localcash.modelo.GastoCompartido;
import com.example.localcash.modelo.GcParticipante;
import com.example.localcash.modelo.GcProducto;
import com.example.localcash.modelo.Perfil;
import com.example.localcash.util.Formato;
import com.example.localcash.util.Validaciones;

import java.util.ArrayList;
import java.util.Locale;

// Detalle de un gasto compartido: reparte el total entre los participantes y marca lo pagado
public class DeudasActivity extends BaseActivity {

    TextView tvTitulo, tvTotal, tvResumen, tvDueno;
    LinearLayout llDeudas, llProductos;
    EditText etProductoNombre, etProductoPrecio;
    Button btnEquitativo, btnGuardar, btnEliminar, btnAgregarProducto, btnAgregarParticipante;

    GastoCompartidoRepositorio gastoRepositorio;
    ContactoRepositorio contactoRepositorio;
    PerfilRepositorio perfilRepositorio;
    String nombreDueno;

    long gastoId;
    GastoCompartido gasto;

    ArrayList<GcParticipante> participantes = new ArrayList<>();
    // Filas creadas dinámicamente (una por participante) para leer/escribir sus montos
    ArrayList<View> filas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_deudas);
        configurarBarra(getString(R.string.ade_titulo));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvTitulo = findViewById(R.id.tvTitulo);
        tvTotal = findViewById(R.id.tvTotal);
        tvResumen = findViewById(R.id.tvResumen);
        tvDueno = findViewById(R.id.tvDueno);
        llDeudas = findViewById(R.id.llDeudas);
        llProductos = findViewById(R.id.llProductos);
        etProductoNombre = findViewById(R.id.etProductoNombre);
        etProductoPrecio = findViewById(R.id.etProductoPrecio);
        btnEquitativo = findViewById(R.id.btnEquitativo);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnEliminar = findViewById(R.id.btnEliminar);
        btnAgregarProducto = findViewById(R.id.btnAgregarProducto);
        btnAgregarParticipante = findViewById(R.id.btnAgregarParticipante);

        gastoRepositorio = new GastoCompartidoRepositorio(this);
        contactoRepositorio = new ContactoRepositorio(this);
        perfilRepositorio = new PerfilRepositorio(this);
        Perfil perfil = perfilRepositorio.obtenerPerfil();
        nombreDueno = (perfil != null && perfil.getNombre() != null) ? perfil.getNombre() : "Yo";

        gastoId = getIntent().getLongExtra("GASTO_ID", 0);
        gasto = gastoRepositorio.obtenerGasto(gastoId);

        if (gasto == null) {
            Toast.makeText(this, R.string.ade_no_encontrado, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitulo.setText(gasto.getTitulo());
        tvTotal.setText(String.format(Formato.CL, getString(R.string.ade_total), gasto.getMontoTotal()));

        cargarProductos();
        cargarParticipantes();

        btnEquitativo.setOnClickListener(v -> repartoEquitativo());
        btnGuardar.setOnClickListener(v -> guardar());
        btnEliminar.setOnClickListener(v -> confirmarEliminar());
        btnAgregarProducto.setOnClickListener(v -> agregarProducto());
        btnAgregarParticipante.setOnClickListener(v -> dialogAgregarParticipante());
    }

    // Muestra el detalle de productos con un botón para quitar cada uno
    private void cargarProductos() {
        llProductos.removeAllViews();
        ArrayList<GcProducto> productos = gastoRepositorio.listarProductos(gastoId);
        for (GcProducto p : productos) {
            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tv.setText(String.format(Formato.CL, "• %s — $%,.0f", p.getNombre(), p.getPrecio()));
            tv.setTextColor(getColor(R.color.lc_text));
            tv.setPadding(0, 8, 0, 8);
            fila.addView(tv);

            Button btnQuitar = new Button(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnQuitar.setText(R.string.ade_quitar);
            btnQuitar.setMinWidth(0);
            btnQuitar.setMinHeight(0);
            btnQuitar.setPadding(24, 0, 24, 0);
            btnQuitar.setOnClickListener(v -> {
                gastoRepositorio.eliminarProducto(p.getId());
                recalcularTotal();
                cargarProductos();
            });
            fila.addView(btnQuitar);

            llProductos.addView(fila);
        }
    }

    // Agrega un producto al gasto y actualiza el total
    private void agregarProducto() {
        if (Validaciones.esVacio(etProductoNombre)) {
            etProductoNombre.setError(getString(R.string.agc_error_producto));
            return;
        }
        double precio = Validaciones.aDouble(etProductoPrecio);
        if (precio <= 0) {
            etProductoPrecio.setError(getString(R.string.agc_error_precio));
            return;
        }
        gastoRepositorio.agregarProducto(gastoId, etProductoNombre.getText().toString().trim(), precio);
        etProductoNombre.setText("");
        etProductoPrecio.setText("");
        recalcularTotal();
        cargarProductos();
        Toast.makeText(this, R.string.ade_producto_agregado, Toast.LENGTH_SHORT).show();
    }

    // Recalcula el monto total del gasto como la suma de sus productos y lo persiste
    private void recalcularTotal() {
        double total = 0;
        for (GcProducto p : gastoRepositorio.listarProductos(gastoId)) {
            total += p.getPrecio();
        }
        gastoRepositorio.actualizarMontoTotal(gastoId, total);
        gasto = gastoRepositorio.obtenerGasto(gastoId);
        tvTotal.setText(String.format(Formato.CL, getString(R.string.ade_total), gasto.getMontoTotal()));
        actualizarResumen();
    }

    // Diálogo para agregar como participante un contacto de la agenda que aún no esté en el gasto
    private void dialogAgregarParticipante() {
        ArrayList<Long> yaEstan = new ArrayList<>();
        for (GcParticipante p : participantes) {
            yaEstan.add(p.getContactoId());
        }
        ArrayList<Contacto> disponibles = new ArrayList<>();
        for (Contacto c : contactoRepositorio.listar()) {
            if (!yaEstan.contains(c.getId())) {
                disponibles.add(c);
            }
        }
        if (disponibles.isEmpty()) {
            Toast.makeText(this, R.string.ade_sin_contactos_disponibles, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] nombres = new String[disponibles.size()];
        for (int i = 0; i < disponibles.size(); i++) {
            nombres[i] = disponibles.get(i).getNombre();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.ade_agregar_participante_titulo)
                .setItems(nombres, (dialog, which) -> {
                    gastoRepositorio.agregarParticipante(gastoId, disponibles.get(which).getId(), 0);
                    Toast.makeText(this, R.string.ade_participante_agregado, Toast.LENGTH_SHORT).show();
                    cargarParticipantes();
                })
                .setNegativeButton(R.string.ade_eliminar_cancelar, null)
                .show();
    }

    // Pide confirmación y elimina el gasto completo
    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ade_eliminar_titulo)
                .setMessage(R.string.ade_eliminar_mensaje)
                .setPositiveButton(R.string.ade_eliminar_confirmar, (dialog, which) -> {
                    gastoRepositorio.eliminarGasto(gastoId);
                    Toast.makeText(this, R.string.ade_eliminado, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.ade_eliminar_cancelar, null)
                .show();
    }

    // Crea una fila por participante con su monto editable y la casilla de pagado
    private void cargarParticipantes() {
        llDeudas.removeAllViews();
        filas.clear();
        participantes = gastoRepositorio.listarParticipantes(gastoId);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (GcParticipante p : participantes) {
            View fila = inflater.inflate(R.layout.item_deuda, llDeudas, false);
            ((TextView) fila.findViewById(R.id.tvNombre)).setText(p.getContactoNombre());
            EditText etMonto = fila.findViewById(R.id.etMonto);
            etMonto.setText(String.format(Locale.getDefault(), "%.0f", p.getMontoAsignado()));
            // Al cambiar el monto de un participante se recalcula la parte del dueño y el resumen
            etMonto.addTextChangedListener(new SimpleTextWatcher(this::actualizarResumen));
            ((CheckBox) fila.findViewById(R.id.cbPagado)).setChecked(p.isPagado());
            fila.findViewById(R.id.btnQuitar).setOnClickListener(v -> {
                gastoRepositorio.eliminarParticipante(p.getId());
                cargarParticipantes();
            });
            llDeudas.addView(fila);
            filas.add(fila);
        }
        actualizarResumen();
    }

    // Reparte el total en partes iguales entre los participantes MÁS el dueño (n+1)
    private void repartoEquitativo() {
        if (!participantes.isEmpty()) {
            double cuota = gasto.getMontoTotal() / (participantes.size() + 1);
            for (View fila : filas) {
                EditText etMonto = fila.findViewById(R.id.etMonto);
                etMonto.setText(String.format(Locale.getDefault(), "%.0f", cuota));
            }
        }
        actualizarResumen();
    }

    // Persiste el monto y el estado de pago de cada participante
    private void guardar() {
        for (int i = 0; i < participantes.size(); i++) {
            View fila = filas.get(i);
            EditText etMonto = fila.findViewById(R.id.etMonto);
            CheckBox cbPagado = fila.findViewById(R.id.cbPagado);
            double monto = Validaciones.aDouble(etMonto);
            boolean pagado = cbPagado.isChecked();
            gastoRepositorio.actualizarParticipante(participantes.get(i).getId(), monto, pagado);
        }
        Toast.makeText(this, R.string.ade_guardado, Toast.LENGTH_SHORT).show();
        cargarParticipantes();
    }

    // Muestra la suma asignada a los participantes, la parte del dueño y si cuadra con el total
    private void actualizarResumen() {
        double asignadoParticipantes = 0;
        for (View fila : filas) {
            EditText etMonto = fila.findViewById(R.id.etMonto);
            asignadoParticipantes += Validaciones.aDouble(etMonto);
        }
        double total = gasto.getMontoTotal();
        // El dueño cubre lo que falta para llegar al total (nunca negativo)
        double parteDueno = Math.max(0, total - asignadoParticipantes);
        tvDueno.setText(String.format(Formato.CL, getString(R.string.ade_dueno), nombreDueno, parteDueno));

        double asignadoConDueno = asignadoParticipantes + parteDueno;
        String estado = (asignadoParticipantes <= total + 0.5)
                ? getString(R.string.ade_cuadra)
                : getString(R.string.ade_no_cuadra);
        tvResumen.setText(String.format(Formato.CL, getString(R.string.ade_resumen), asignadoConDueno, total, estado));
    }

    // TextWatcher mínimo que solo reacciona al cambio de texto
    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable alCambiar;

        SimpleTextWatcher(Runnable alCambiar) {
            this.alCambiar = alCambiar;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            alCambiar.run();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
