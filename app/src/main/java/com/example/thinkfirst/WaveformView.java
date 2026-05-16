package com.example.thinkfirst;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.core.content.ContextCompat;

public class WaveformView extends View {

    private static final int   BAR_COUNT  = 5;
    private static final int   DURATION   = 600; // ms per bar cycle

    private final Paint   barPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] barHeights = new float[BAR_COUNT];
    private final ValueAnimator[] animators = new ValueAnimator[BAR_COUNT];
    private boolean running = false;

    public WaveformView(Context context) {
        super(context); init(context);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs); init(context);
    }

    private void init(Context context) {
        barPaint.setColor(ContextCompat.getColor(context, R.color.colorAccent));
        barPaint.setStyle(Paint.Style.FILL);

        // Each bar gets a different phase offset so they look organic
        int[] delays = {0, 120, 240, 120, 60};

        for (int i = 0; i < BAR_COUNT; i++) {
            final int idx = i;
            ValueAnimator anim = ValueAnimator.ofFloat(0.15f, 1.0f, 0.15f);
            anim.setDuration(DURATION);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setRepeatMode(ValueAnimator.RESTART);
            anim.setInterpolator(new LinearInterpolator());
            anim.setStartDelay(delays[i]);
            anim.addUpdateListener(animation -> {
                barHeights[idx] = (float) animation.getAnimatedValue();
                invalidate();
            });
            animators[i] = anim;
        }
    }

    public void startWave() {
        if (running) return;
        running = true;
        for (ValueAnimator a : animators) a.start();
    }

    public void stopWave() {
        running = false;
        for (ValueAnimator a : animators) {
            a.cancel();
        }
        for (int i = 0; i < BAR_COUNT; i++) barHeights[i] = 0f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!running) return;

        float w       = getWidth();
        float h       = getHeight();
        float barW    = w / (BAR_COUNT * 2f);          // bar width
        float gap     = barW;                            // gap between bars
        float totalW  = BAR_COUNT * barW + (BAR_COUNT - 1) * gap;
        float startX  = (w - totalW) / 2f;
        float cornerR = barW / 2f;

        for (int i = 0; i < BAR_COUNT; i++) {
            float barH   = Math.max(barW, barHeights[i] * h);
            float left   = startX + i * (barW + gap);
            float right  = left + barW;
            float top    = (h - barH) / 2f;
            float bottom = top + barH;

            canvas.drawRoundRect(left, top, right, bottom,
                    cornerR, cornerR, barPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopWave();
    }
}