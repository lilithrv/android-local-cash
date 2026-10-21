package com.example.localcash.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public final class PinMapa {

    private PinMapa() {
    }

    public static BitmapDescriptor pin(Context ctx, int iconoRes, int colorFondo) {
        float dens = ctx.getResources().getDisplayMetrics().density;
        int ancho = Math.round(46 * dens);
        int alto = Math.round(58 * dens);
        float radio = 20 * dens;
        float borde = 2.5f * dens;
        float cx = ancho / 2f;
        float cy = radio + borde;
        float puntaY = alto - borde;

        Bitmap bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888);
        Canvas lienzo = new Canvas(bmp);

        Paint blanco = new Paint(Paint.ANTI_ALIAS_FLAG);
        blanco.setColor(Color.WHITE);
        Paint color = new Paint(Paint.ANTI_ALIAS_FLAG);
        color.setColor(colorFondo);

        dibujarGota(lienzo, cx, cy, radio, puntaY, blanco);
        dibujarGota(lienzo, cx, cy, radio - borde, puntaY - borde, color);

        Drawable icono = ContextCompat.getDrawable(ctx, iconoRes);
        if (icono != null) {
            icono = icono.mutate();
            DrawableCompat.setTint(icono, Color.WHITE);
            float lado = radio * 1.15f;
            int izq = Math.round(cx - lado / 2f);
            int arr = Math.round(cy - lado / 2f);
            icono.setBounds(izq, arr, Math.round(izq + lado), Math.round(arr + lado));
            icono.draw(lienzo);
        }

        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    private static void dibujarGota(Canvas lienzo, float cx, float cy, float radio,
                                    float puntaY, Paint pintura) {
        float dist = puntaY - cy;
        if (dist > radio) {
            float tangente = (float) Math.sqrt(dist * dist - radio * radio);
            float tx = radio * tangente / dist;
            float ty = cy + radio * radio / dist;
            Path cola = new Path();
            cola.moveTo(cx - tx, ty);
            cola.lineTo(cx, puntaY);
            cola.lineTo(cx + tx, ty);
            cola.close();
            lienzo.drawPath(cola, pintura);
        }
        lienzo.drawCircle(cx, cy, radio, pintura);
    }
}
