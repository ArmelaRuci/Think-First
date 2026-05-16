package com.example.thinkfirst;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView tvAppName = findViewById(R.id.tv_app_name);
        TextView tvTagline = findViewById(R.id.tv_tagline);

        // App name: scale from 0.7 + fade in
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvAppName, "scaleX", 0.7f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvAppName, "scaleY", 0.7f, 1f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(tvAppName, "alpha",  0f,   1f);
        scaleX.setDuration(700);
        scaleY.setDuration(700);
        fadeIn.setDuration(700);

        AnimatorSet appNameAnim = new AnimatorSet();
        appNameAnim.playTogether(scaleX, scaleY, fadeIn);
        appNameAnim.start();

        // Tagline: fade in with delay
        tvTagline.setAlpha(0f);
        tvTagline.animate()
                .alpha(1f)
                .setStartDelay(500)
                .setDuration(600)
                .start();

        // Navigate to HomeActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION_MS);
    }
}