package com.example.localcash.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.R;
import com.example.localcash.modelo.GastoCompartido;
import com.example.localcash.util.Formato;

import java.util.List;
import java.util.Map;

// Adapter de la lista de gastos compartidos existentes
public class GastoAdapter extends RecyclerView.Adapter<GastoAdapter.GastoViewHolder> {

    public interface OnGastoClickListener {
        void onClick(GastoCompartido gasto);
    }

    private final List<GastoCompartido> gastos;
    private final OnGastoClickListener listener;
    // Resumen de pago por gasto
    private final Map<Long, String> resumenes;

    public GastoAdapter(List<GastoCompartido> gastos, OnGastoClickListener listener,
                        Map<Long, String> resumenes) {
        this.gastos = gastos;
        this.listener = listener;
        this.resumenes = resumenes;
    }

    @NonNull
    @Override
    public GastoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gasto, parent, false);
        return new GastoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull GastoViewHolder holder, int position) {
        GastoCompartido gasto = gastos.get(position);
        holder.tvTitulo.setText(gasto.getTitulo());
        String resumen = (resumenes != null) ? resumenes.get(gasto.getId()) : null;
        String detalle = (resumen != null && !resumen.isEmpty())
                ? gasto.getFecha() + "  ·  " + resumen
                : gasto.getFecha();
        holder.tvDetalle.setText(detalle);
        holder.tvTotal.setText(String.format(Formato.CL, "$%,.0f", gasto.getMontoTotal()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(gasto);
            }
        });
    }

    @Override
    public int getItemCount() {
        return gastos.size();
    }

    static class GastoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo;
        TextView tvDetalle;
        TextView tvTotal;

        public GastoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDetalle = itemView.findViewById(R.id.tvDetalle);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }
    }
}
