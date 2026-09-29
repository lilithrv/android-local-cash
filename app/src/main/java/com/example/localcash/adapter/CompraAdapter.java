package com.example.localcash.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.R;
import com.example.localcash.modelo.Compra;
import com.example.localcash.util.Formato;

import java.util.List;

// Adapter RecyclerView compras (historial y agendadas)
public class CompraAdapter extends RecyclerView.Adapter<CompraAdapter.CompraViewHolder> {

    public interface OnCompraEliminarListener {
        void onEliminar(Compra compra);
    }

    // Avisa a la Activity cuando se toca una compra para editarla
    public interface OnCompraClickListener {
        void onClick(Compra compra);
    }

    private final List<Compra> compras;
    private final OnCompraEliminarListener listener;
    private final OnCompraClickListener clickListener;

    public CompraAdapter(List<Compra> compras, OnCompraEliminarListener listener,
                         OnCompraClickListener clickListener) {
        this.compras = compras;
        this.listener = listener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public CompraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_compra, parent, false);
        return new CompraViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull CompraViewHolder holder, int position) {
        Compra compra = compras.get(position);
        holder.tvTitulo.setText(compra.getTitulo());
        holder.tvMonto.setText(String.format(Formato.CL, "$%,.0f", compra.getMonto()));

        // Estado y contacto asociado (si tiene).
        String estado = compra.esAgendada() ? "Agendada" : "Realizada";
        String detalle = estado + "  ·  " + compra.getFecha();
        if (compra.getContactoNombre() != null && !compra.getContactoNombre().isEmpty()) {
            detalle += "  ·  " + compra.getContactoNombre();
        }
        holder.tvDetalle.setText(detalle);

        int color = compra.esAgendada() ? R.color.lc_hint : R.color.lc_accent;
        holder.tvMonto.setTextColor(holder.itemView.getResources().getColor(color, null));

        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEliminar(compra);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(compra);
            }
        });
    }

    @Override
    public int getItemCount() {
        return compras.size();
    }

    static class CompraViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo;
        TextView tvDetalle;
        TextView tvMonto;
        Button btnEliminar;

        public CompraViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDetalle = itemView.findViewById(R.id.tvDetalle);
            tvMonto = itemView.findViewById(R.id.tvMonto);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
