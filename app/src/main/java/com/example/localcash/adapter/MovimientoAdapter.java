package com.example.localcash.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.R;
import com.example.localcash.modelo.Movimiento;
import com.example.localcash.util.Formato;

import java.util.List;

// Adapter del RecyclerView de movimientos. Pinta ingresos en verde y egresos en rojo. Módulo 1.
public class MovimientoAdapter extends RecyclerView.Adapter<MovimientoAdapter.MovimientoViewHolder> {

    // Avisa a la Activity cuando se toca un movimiento para editarlo.
    public interface OnMovimientoClickListener {
        void onClick(Movimiento movimiento);
    }

    // Avisa a la Activity cuando se pide eliminar un movimiento.
    public interface OnMovimientoEliminarListener {
        void onEliminar(Movimiento movimiento);
    }

    private final List<Movimiento> movimientos;
    private final OnMovimientoClickListener clickListener;
    private final OnMovimientoEliminarListener eliminarListener;

    public MovimientoAdapter(List<Movimiento> movimientos, OnMovimientoClickListener clickListener,
                             OnMovimientoEliminarListener eliminarListener) {
        this.movimientos = movimientos;
        this.clickListener = clickListener;
        this.eliminarListener = eliminarListener;
    }

    @NonNull
    @Override
    public MovimientoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movimiento, parent, false);
        return new MovimientoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull MovimientoViewHolder holder, int position) {
        Movimiento mov = movimientos.get(position);
        holder.tvDescripcion.setText(mov.getDescripcion());
        holder.tvFecha.setText(mov.getFecha());

        String signo = mov.esIngreso() ? "+" : "-";
        holder.tvMonto.setText(String.format(Formato.CL, "%s$%,.0f", signo, mov.getMonto()));
        int color = mov.esIngreso() ? R.color.lc_ingreso : R.color.lc_egreso;
        holder.tvMonto.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), color));

        // Marca visual si el movimiento tiene ubicación geográfica.
        holder.tvGeo.setVisibility(mov.isTieneGeo() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(mov);
            }
        });
        holder.btnEliminar.setOnClickListener(v -> {
            if (eliminarListener != null) {
                eliminarListener.onEliminar(mov);
            }
        });
    }

    @Override
    public int getItemCount() {
        return movimientos.size();
    }

    static class MovimientoViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescripcion;
        TextView tvFecha;
        TextView tvMonto;
        TextView tvGeo;
        Button btnEliminar;

        public MovimientoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvMonto = itemView.findViewById(R.id.tvMonto);
            tvGeo = itemView.findViewById(R.id.tvGeo);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
