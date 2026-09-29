package com.example.localcash.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localcash.R;
import com.example.localcash.modelo.Contacto;

import java.util.List;

// Adapter RecyclerView contactos
public class ContactoAdapter extends RecyclerView.Adapter<ContactoAdapter.ContactoViewHolder> {

    // Avisa a la Activity cuando se pide eliminar un contacto
    public interface OnContactoEliminarListener {
        void onEliminar(Contacto contacto);
    }

    // Avisa a la Activity cuando se toca un contacto para editarlo
    public interface OnContactoClickListener {
        void onClick(Contacto contacto);
    }

    private final List<Contacto> contactos;
    private final OnContactoEliminarListener listener;
    private final OnContactoClickListener clickListener;

    public ContactoAdapter(List<Contacto> contactos, OnContactoEliminarListener listener,
                           OnContactoClickListener clickListener) {
        this.contactos = contactos;
        this.listener = listener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ContactoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contacto, parent, false);
        return new ContactoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactoViewHolder holder, int position) {
        Contacto contacto = contactos.get(position);
        holder.tvNombre.setText(contacto.getNombre());
        holder.tvDetalle.setText(contacto.getTelefono() + "  ·  " + contacto.getEmail());
        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEliminar(contacto);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(contacto);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactos.size();
    }

    static class ContactoViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        TextView tvDetalle;
        Button btnEliminar;

        public ContactoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvDetalle = itemView.findViewById(R.id.tvDetalle);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
