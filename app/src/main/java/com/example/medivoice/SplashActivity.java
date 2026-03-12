package com.example.medivoice;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LinearLayout logoContainer = findViewById(R.id.logoContainer);

        // Load the animation
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.app_open_anim);
        logoContainer.startAnimation(anim);

        // Wait for 3 seconds, then open MainActivity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close splash activity so user can't go back to it
            }
        }, 3000); // 3000ms = 3 seconds
    }
}