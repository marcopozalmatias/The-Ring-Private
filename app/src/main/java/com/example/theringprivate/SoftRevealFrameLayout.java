package com.example.theringprivate;

// Contenedor personalizado que dibuja su contenido recortándolo con un círculo animable.
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Layout usado para crear el efecto visual de apertura/cierre con una máscara circular suave.
public class SoftRevealFrameLayout extends FrameLayout {

    // Coordenada horizontal del centro desde el que nace el efecto.
    private float centerX = 0f;
    // Coordenada vertical del centro desde el que nace el efecto.
    private float centerY = 0f;
    // Radio actual del círculo de revelado; un valor negativo desactiva el recorte.
    private float revealRadius = -1f;
    // Ruta reutilizable para no crear objetos nuevos en cada dibujo.
    private final Path revealPath = new Path();

    // Constructor estándar requerido cuando el layout se crea desde código.
    public SoftRevealFrameLayout(@NonNull Context context) {
        super(context);
    }

    // Constructor usado cuando el layout se infla desde XML con atributos opcionales.
    public SoftRevealFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    // Constructor completo para escenarios más específicos de inflado.
    public SoftRevealFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // Actualiza el centro horizontal del círculo de revelado.
    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    // Actualiza el centro vertical del círculo de revelado.
    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    // Cambia el radio de recorte y fuerza un redibujado inmediato.
    public void setRevealRadius(float revealRadius) {
        this.revealRadius = revealRadius;
        invalidate();
    }


    // Dibuja el contenido aplicando un recorte circular cuando el efecto está activo.
    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (revealRadius >= 0) {
            canvas.save();
            revealPath.reset();
            revealPath.addCircle(centerX, centerY, revealRadius, Path.Direction.CW);
            canvas.clipPath(revealPath);
            super.dispatchDraw(canvas);
            canvas.restore();
        } else {
            super.dispatchDraw(canvas);
        }
    }
}
