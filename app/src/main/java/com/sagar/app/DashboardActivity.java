package com.sagar.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private ViewGroup bottomNavMenu;
    private LinearLayout navHome, navPlanner, navMusic, navAbout;
    private ImageView ivHome, ivPlanner, ivMusic, ivAbout;
    private TextView tvHomeLabel, tvPlannerLabel, tvMusicLabel, tvAboutLabel;

    // Dashboard stat views
    private TextView tvTotalTasks, tvCompletedTasks, tvPendingTasks, tvCompletionRate;
    private TextView tvStatsMonth, tvLastUpdated;
    private ProgressBar progressCompletion;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        firebaseHelper = new FirebaseHelper();
        bindNavViews();
        bindStatViews();
        setupBottomNavigation();
        loadDashboardStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void bindStatViews() {
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = findViewById(R.id.tvPendingTasks);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvStatsMonth = findViewById(R.id.tvStatsMonth);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        progressCompletion = findViewById(R.id.progressCompletion);

        // Set current month
        if (tvStatsMonth != null) {
            String month = new SimpleDateFormat("MMMM yyyy 'Stats'", Locale.getDefault()).format(new Date());
            tvStatsMonth.setText(month);
        }
    }

    private void loadDashboardStats() {
        com.google.firebase.firestore.Query query = firebaseHelper.getTasksQuery();
        if (query == null)
            return;

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int total = 0;
            int completed = 0;
            int pending = 0;

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                TaskModel task = doc.toObject(TaskModel.class);
                total++;
                if (task.isCompleted())
                    completed++;
                else
                    pending++;
            }

            int rate = total > 0 ? (int) ((completed * 100.0) / total) : 0;

            if (tvTotalTasks != null)
                tvTotalTasks.setText(String.valueOf(total));
            if (tvCompletedTasks != null)
                tvCompletedTasks.setText(String.valueOf(completed));
            if (tvPendingTasks != null)
                tvPendingTasks.setText(String.valueOf(pending));
            if (tvCompletionRate != null)
                tvCompletionRate.setText(rate + "%");
            if (progressCompletion != null)
                progressCompletion.setProgress(rate);
            if (tvLastUpdated != null) {
                String now = new SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()).format(new Date());
                tvLastUpdated.setText("Last updated: " + now);
            }

        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load stats", Toast.LENGTH_SHORT).show());
    }

    private void bindNavViews() {
        bottomNavMenu = findViewById(R.id.bottomNavMenu);
        navHome = findViewById(R.id.navHome);
        navPlanner = findViewById(R.id.navPlanner);
        navMusic = findViewById(R.id.navMusic);
        navAbout = findViewById(R.id.navAbout);
        ivHome = findViewById(R.id.ivHome);
        ivPlanner = findViewById(R.id.ivPlanner);
        ivMusic = findViewById(R.id.ivMusic);
        ivAbout = findViewById(R.id.ivAbout);
        tvHomeLabel = findViewById(R.id.tvHomeLabel);
        tvPlannerLabel = findViewById(R.id.tvPlannerLabel);
        tvMusicLabel = findViewById(R.id.tvMusicLabel);
        tvAboutLabel = findViewById(R.id.tvAboutLabel);
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            setActiveNavItem(navHome);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });
        navPlanner.setOnClickListener(v -> {
            setActiveNavItem(navPlanner);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(DashboardActivity.this, AddPlannerActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });
        navMusic.setOnClickListener(v -> {
            setActiveNavItem(navMusic);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(DashboardActivity.this, MusicActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });
        navAbout.setOnClickListener(v -> {
            setActiveNavItem(navAbout);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void setActiveNavItem(LinearLayout activeItem) {
        // Simple color change for active state
        resetNavItem(navHome, ivHome, tvHomeLabel);
        resetNavItem(navPlanner, ivPlanner, tvPlannerLabel);
        resetNavItem(navMusic, ivMusic, tvMusicLabel);
        resetNavItem(navAbout, ivAbout, tvAboutLabel);

        // Set active item to black
        if (activeItem == navHome) {
            ivHome.setColorFilter(Color.BLACK);
            tvHomeLabel.setTextColor(Color.BLACK);
        } else if (activeItem == navPlanner) {
            ivPlanner.setColorFilter(Color.BLACK);
            tvPlannerLabel.setTextColor(Color.BLACK);
        } else if (activeItem == navMusic) {
            ivMusic.setColorFilter(Color.BLACK);
            tvMusicLabel.setTextColor(Color.BLACK);
        } else if (activeItem == navAbout) {
            ivAbout.setColorFilter(Color.BLACK);
            tvAboutLabel.setTextColor(Color.BLACK);
        }
    }

    private void resetNavItem(LinearLayout item, ImageView iv, TextView tv) {
        iv.setColorFilter(Color.parseColor("#888888"));
        tv.setTextColor(Color.parseColor("#888888"));
    }
}
