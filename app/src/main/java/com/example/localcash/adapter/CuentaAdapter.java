package com.example.localcash.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.R;
import com.example.localcash.modelo.Cuenta;
import com.example.localcash.util.Formato;

import java.util.List;

// Adapter RecyclerView de cuentas, muestra el saldo actual calculado
public class CuentaAdapter extends RecyclerView.Adapter<CuentaAdapter.CuentaViewHolder> {

    public interface OnCuentaEliminarListener {
        void onEliminar(Cuenta cuenta);
    }

    // Avisa a la Activity cuando se toca una cuenta para editarla
    public interface OnCuentaClickListener {
        void onClick(Cuenta cuenta);
    }

    private final List<Cuenta> cuentas;
    private final OnCuentaEliminarListener listener;
    private final OnCuentaClickListener clickListener;

    public CuentaAdapter(List<Cuenta> cuentas, OnCuentaEliminarListener listener,
                         OnCuentaClickListener clickListener) {
        this.cuentas = cuentas;
        this.listener = listener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public CuentaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cuenta, parent, false);
        return new CuentaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull CuentaViewHolder holder, int position) {
        Cuenta cuenta = cuentas.get(position);
        holder.tvNombre.setText(cuenta.getNombre());
        holder.tvTipo.setText(cuenta.getTipo());
        holder.tvSaldo.setText(String.format(Formato.CL, "$%,.0f", cuenta.getSaldoActual()));
        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEliminar(cuenta);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(cuenta);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cuentas.size();
    }

    static class CuentaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        TextView tvTipo;
        TextView tvSaldo;
        Button btnEliminar;

        public CuentaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvTipo = itemView.findViewById(R.id.tvTipo);
            tvSaldo = itemView.findViewById(R.id.tvSaldo);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
