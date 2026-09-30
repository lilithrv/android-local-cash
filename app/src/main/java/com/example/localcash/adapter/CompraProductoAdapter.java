package com.example.localcash.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.R;
import com.example.localcash.modelo.CompraProducto;
import com.example.localcash.util.Formato;

import java.util.List;

// Adapter del carrito de una compra: muestra el precio estimado y, cuando la compra está
// marcada como realizada, permite editar el precio real de cada producto
public class CompraProductoAdapter extends RecyclerView.Adapter<CompraProductoAdapter.ProductoViewHolder> {

    public interface Listener {
        void onQuitar(int posicion);

        void onPrecioRealCambiado();
    }

    private final List<CompraProducto> productos;
    private final Listener listener;
    private boolean modoReal;

    public CompraProductoAdapter(List<CompraProducto> productos, Listener listener) {
        this.productos = productos;
        this.listener = listener;
    }

    // Cambia entre modo estimado (agendada) y modo real (realizada)
    public void setModoReal(boolean modoReal) {
        this.modoReal = modoReal;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_compra_producto, parent, false);
        return new ProductoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        CompraProducto producto = productos.get(position);
        holder.tvNombre.setText(producto.getNombre());
        holder.tvPrecioEstimado.setText(
                String.format(Formato.CL, "Estimado: $%,.0f", producto.getPrecioEstimado()));

        holder.btnQuitar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuitar(holder.getAdapterPosition());
            }
        });

        // Al reciclar la vista, quitar el watcher anterior antes de reescribir el valor
        if (holder.watcher != null) {
            holder.etPrecioReal.removeTextChangedListener(holder.watcher);
        }

        if (modoReal) {
            holder.etPrecioReal.setVisibility(View.VISIBLE);
            // Precarga: el precio real ya guardado, o el estimado como sugerencia inicial
            double valor = producto.getPrecioReal() > 0
                    ? producto.getPrecioReal() : producto.getPrecioEstimado();
            holder.etPrecioReal.setText(String.format(Formato.CL, "%.0f", valor));
            // Asegurar que el modelo tenga ese valor aunque el usuario no lo toque.
            producto.setPrecioReal(valor);

            holder.watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    double nuevo = parsear(s.toString());
                    producto.setPrecioReal(nuevo);
                    if (listener != null) {
                        listener.onPrecioRealCambiado();
                    }
                }
            };
            holder.etPrecioReal.addTextChangedListener(holder.watcher);
        } else {
            holder.etPrecioReal.setVisibility(View.GONE);
            holder.watcher = null;
        }
    }

    private double parsear(String texto) {
        String limpio = texto.trim();
        if (limpio.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return productos.size();
    }

    static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        TextView tvPrecioEstimado;
        EditText etPrecioReal;
        Button btnQuitar;
        TextWatcher watcher;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvPrecioEstimado = itemView.findViewById(R.id.tvPrecioEstimado);
            etPrecioReal = itemView.findViewById(R.id.etPrecioReal);
            btnQuitar = itemView.findViewById(R.id.btnQuitar);
        }
    }
}
