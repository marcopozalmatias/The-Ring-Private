package com.example.theringprivate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SoftRevealFrameLayout extends FrameLayout {

    private float centerX = 0f;
    private float centerY = 0f;
    private float revealRadius = -1f;
    private final Path revealPath = new Path();

    public SoftRevealFrameLayout(@NonNull Context context) {
        super(context);
    }

    public SoftRevealFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SoftRevealFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    public void setRevealRadius(float revealRadius) {
        this.revealRadius = revealRadius;
        invalidate();
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getRevealRadius() {
        return revealRadius;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
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
