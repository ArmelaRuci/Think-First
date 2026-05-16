package com.example.thinkfirst;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {

    // ── Data model ────────────────────────────────────────────────────────────
    public static class BarEntry {
        public final String label;
        public final float  value;
        public final float  maxValue;
        public final int    color;
        public final String valueLabel;

        public BarEntry(String label, float value, float maxValue,
                        int color, String valueLabel) {
            this.label      = label;
            this.value      = value;
            this.maxValue   = maxValue;
            this.color      = color;
            this.valueLabel = valueLabel;
        }
    }

    private final List<BarEntry> entries = new ArrayList<>();
    private float animProgress = 1f;

    private Paint labelPaint;
    private Paint barBgPaint;
    private Paint barFillPaint;
    private Paint valuePaint;
    private float density;

    // ── Constructors ──────────────────────────────────────────────────────────
    public BarChartView(Context context) {
        super(context); init(context);
    }
    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs); init(context);
    }
    public BarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(ContextCompat.getColor(context, R.color.colorTextSecondary));
        labelPaint.setTextSize(12 * density);
        labelPaint.setTextAlign(Paint.Align.RIGHT);

        barBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barBgPaint.setColor(ContextCompat.getColor(context, R.color.colorBorder));
        barBgPaint.setStyle(Paint.Style.FILL);

        barFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barFillPaint.setStyle(Paint.Style.FILL);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(ContextCompat.getColor(context, R.color.colorTextSecondary));
        valuePaint.setTextSize(11 * density);
        valuePaint.setTextAlign(Paint.Align.LEFT);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setData(List<BarEntry> data) {
        entries.clear();
        entries.addAll(data);
        animProgress = 0f;
        requestLayout(); // re-measure height for new data size

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(900);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            animProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (entries.isEmpty()) return;

        float w             = getWidth();
        float labelAreaW    = 58 * density;
        float valueAreaW    = 46 * density;
        float barAreaW      = w - labelAreaW - valueAreaW - 16 * density;
        float barAreaStart  = labelAreaW + 8 * density;
        float barCorner     = 4 * density;

        int   n             = entries.size();
        float topPad        = 10 * density;
        float barH          = 32 * density;
        float spacing       = (getHeight() - topPad - barH * n) / (n + 1f);

        for (int i = 0; i < n; i++) {
            BarEntry entry = entries.get(i);
            float   top    = topPad + spacing * (i + 1) + barH * i;
            float   mid    = top + barH / 2f;

            // Label — right-aligned in label area
            float labelY = mid - (labelPaint.descent() + labelPaint.ascent()) / 2f;
            canvas.drawText(entry.label, labelAreaW, labelY, labelPaint);

            // Bar background track
            RectF bgRect = new RectF(
                    barAreaStart, top + barH * 0.15f,
                    barAreaStart + barAreaW, top + barH * 0.85f
            );
            canvas.drawRoundRect(bgRect, barCorner, barCorner, barBgPaint);

            // Animated fill
            float ratio = (entry.maxValue > 0f)
                    ? (entry.value / entry.maxValue) * animProgress
                    : 0f;

            if (ratio > 0.01f) {
                barFillPaint.setColor(entry.color);
                float fillW  = barAreaW * Math.min(ratio, 1f);
                RectF fillRect = new RectF(
                        barAreaStart, top + barH * 0.15f,
                        barAreaStart + fillW, top + barH * 0.85f
                );
                canvas.drawRoundRect(fillRect, barCorner, barCorner, barFillPaint);
            }

            // Value label — right of bar track
            float valueX = barAreaStart + barAreaW + 8 * density;
            float valueY = mid - (valuePaint.descent() + valuePaint.ascent()) / 2f;
            canvas.drawText(
                    entry.value > 0f ? entry.valueLabel : "—",
                    valueX, valueY, valuePaint
            );
        }
    }

    // ── Measure ───────────────────────────────────────────────────────────────

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int n          = entries.isEmpty() ? 5 : entries.size();
        int desiredH   = (int)((n * 44 + 40) * density);
        int width      = MeasureSpec.getSize(widthMeasureSpec);
        int hMode      = MeasureSpec.getMode(heightMeasureSpec);
        int hSize      = MeasureSpec.getSize(heightMeasureSpec);
        int height;

        if (hMode == MeasureSpec.EXACTLY) {
            height = hSize;
        } else if (hMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredH, hSize);
        } else {
            height = desiredH;
        }
        setMeasuredDimension(width, height);
    }
}