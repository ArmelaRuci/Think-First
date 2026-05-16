package com.example.thinkfirst;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.core.content.ContextCompat;

public class TankGaugeView extends View {

    private Paint borderPaint;
    private Paint fillPaint;
    private Paint bgPaint;
    private Paint textPaint;

    private float animatedLevel = 0.75f;
    private float targetLevel   = 0.75f;

    private float paddingPx;
    private float strokePx;
    private float cornerPx;

    // ── Constructors (all three required for XML inflation) ───────────────────
    public TankGaugeView(Context context) {
        super(context);
        init(context);
    }

    public TankGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TankGaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        paddingPx = 6  * density;
        strokePx  = 2  * density;
        cornerPx  = 14 * density;

        // Tank background
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(ContextCompat.getColor(context, R.color.colorSurface));
        bgPaint.setStyle(Paint.Style.FILL);

        // Border
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(ContextCompat.getColor(context, R.color.colorBorder));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(strokePx);

        // Fill — color set dynamically
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        updateFillColor();

        // Percentage text
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
        textPaint.setTextSize(15 * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Animates the tank fill to a new level.
     * @param newLevel float between 0.0 and 1.0
     */
    public void setLevel(float newLevel) {
        newLevel    = Math.max(0f, Math.min(1f, newLevel));
        targetLevel = newLevel;

        ValueAnimator animator = ValueAnimator.ofFloat(animatedLevel, newLevel);
        animator.setDuration(900);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animatedLevel = (float) animation.getAnimatedValue();
            updateFillColor();
            invalidate();
        });
        animator.start();
    }

    public float getLevel() { return targetLevel; }

    // ── Drawing ───────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        RectF outerRect = new RectF(
                paddingPx, paddingPx,
                w - paddingPx, h - paddingPx
        );

        // 1. Draw background
        canvas.drawRoundRect(outerRect, cornerPx, cornerPx, bgPaint);

        // 2. Draw fill from bottom up
        float innerLeft   = paddingPx + strokePx;
        float innerTop    = paddingPx + strokePx;
        float innerRight  = w - paddingPx - strokePx;
        float innerBottom = h - paddingPx - strokePx;
        float innerHeight = innerBottom - innerTop;

        if (animatedLevel > 0.01f) {
            float fillHeight = innerHeight * animatedLevel;
            float fillTop    = innerBottom - fillHeight;
            float fillCorner = Math.max(0f, cornerPx - strokePx);

            RectF fillRect = new RectF(innerLeft, fillTop, innerRight, innerBottom);
            canvas.drawRoundRect(fillRect, fillCorner, fillCorner, fillPaint);
        }

        // 3. Draw border on top of fill
        canvas.drawRoundRect(outerRect, cornerPx, cornerPx, borderPaint);

        // 4. Draw percentage text centered
        int    pct   = Math.round(animatedLevel * 100);
        String label = pct + "%";
        float  textX = w / 2f;
        float  textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(label, textX, textY, textPaint);
    }

    // ── Color logic ───────────────────────────────────────────────────────────

    private void updateFillColor() {
        int color;
        if (animatedLevel >= 0.8f) {
            color = 0xFF238636;                                     // green
        } else if (animatedLevel >= 0.5f) {
            float t = (animatedLevel - 0.5f) / 0.3f;
            color   = blendColors(0xFFFFC107, 0xFF238636, t);       // amber → green
        } else if (animatedLevel >= 0.25f) {
            float t = (animatedLevel - 0.25f) / 0.25f;
            color   = blendColors(0xFFDA3633, 0xFFFFC107, t);       // red → amber
        } else {
            color = 0xFFDA3633;                                     // red
        }
        fillPaint.setColor(color);
    }

    private int blendColors(int from, int to, float ratio) {
        return (int) new ArgbEvaluator().evaluate(ratio, from, to);
    }
}