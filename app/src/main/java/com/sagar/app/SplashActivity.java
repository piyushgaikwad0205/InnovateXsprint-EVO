package com.sagar.app;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_splash);

        // Apply animations to all elements
        android.widget.ImageView logo = findViewById(R.id.a_minimalis);
        android.widget.ImageView topLeaf = findViewById(R.id.ivSplashTopLeaf);
        android.widget.ImageView bottomLeaves = findViewById(R.id.ivSplashBottomLeaves);
        android.widget.ImageView tallLeaf = findViewById(R.id.ivSplashTallLeaf);

        if (logo != null) {
            android.view.animation.Animation zoomIn = android.view.animation.AnimationUtils.loadAnimation(this,
                    R.anim.zoom_in_fade);
            logo.startAnimation(zoomIn);
        }

        if (topLeaf != null) {
            android.view.animation.Animation slideTopRight = android.view.animation.AnimationUtils.loadAnimation(this,
                    R.anim.leaf_top_right_entrance);
            topLeaf.startAnimation(slideTopRight);
        }

        android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this,
                R.anim.leaf_bottom_entrance);
        if (bottomLeaves != null) {
            bottomLeaves.startAnimation(slideUp);
        }
        if (tallLeaf != null) {
            tallLeaf.startAnimation(slideUp);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() != null) {
                // User is already signed in, go to MainActivity
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                // No user signed in, go to LoginActivity
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DELAY_MS);
    }

    @Override
    public void onBackPressed() {
        // Disable back on splash screen
    }
}
