package com.example.thinkfirst;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Redirect to SplashActivity — MainActivity is not used in ThinkFirst
        startActivity(new Intent(this, SplashActivity.class));
        finish();
    }
}