package com.example.localcash.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.R;
import com.example.localcash.modelo.GcProducto;
import com.example.localcash.util.Formato;

import java.util.List;

// Adapter de la lista de productos de un gasto compartido
public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder> {

    public interface OnProductoQuitarListener {
        void onQuitar(int posicion);
    }

    private final List<GcProducto> productos;
    private final OnProductoQuitarListener listener;

    public ProductoAdapter(List<GcProducto> productos, OnProductoQuitarListener listener) {
        this.productos = productos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_producto, parent, false);
        return new ProductoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        GcProducto producto = productos.get(position);
        holder.tvNombre.setText(producto.getNombre());
        holder.tvPrecio.setText(String.format(Formato.CL, "$%,.0f", producto.getPrecio()));
        holder.btnQuitar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuitar(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return productos.size();
    }

    static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        TextView tvPrecio;
        Button btnQuitar;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            btnQuitar = itemView.findViewById(R.id.btnQuitar);
        }
    }
}
