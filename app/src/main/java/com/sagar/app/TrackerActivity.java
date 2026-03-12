package com.sagar.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrackerActivity extends AppCompatActivity {

    // ── Stats views ──
    private TextView tvXP, tvRank, tvActivities;
    private TextView tvTasksRatio, tvCompletionPct, tvDonePct, tvPendingPct;
    private ProgressBar progressTaskCircle, progressDone, progressPending, progressActivity;
    private TextView tvCurrentStreak, tvBestStreak, tvStreakMonth;
    private TextView tvActivitySub, tvActivityPct;
    private LinearLayout weekDotRow;
    private TextView tvStreakHeaderCount;

    // ── Nav views ──
    private LinearLayout navHome, navPlanner, navTracker, navMusic, navAbout;
    private ImageView ivHome, ivPlanner, ivTracker, ivMusic, ivAbout;
    private TextView tvHomeLabel, tvPlannerLabel, tvTrackerLabel, tvMusicLabel, tvAboutLabel;

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        firebaseHelper = new FirebaseHelper();
        bindViews();
        setupBottomNavigation();
        loadStreakData();
        loadTaskStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStreakData();
        loadTaskStats();
    }

    // ─────────────────────────────────────────────────────────────────

    private void bindViews() {
        tvXP            = findViewById(R.id.tvXP);
        tvRank          = findViewById(R.id.tvRank);
        tvActivities    = findViewById(R.id.tvActivities);
        tvTasksRatio    = findViewById(R.id.tvTasksRatio);
        tvCompletionPct = findViewById(R.id.tvCompletionPct);
        tvDonePct       = findViewById(R.id.tvDonePct);
        tvPendingPct    = findViewById(R.id.tvPendingPct);

        progressTaskCircle = findViewById(R.id.progressTaskCircle);
        progressDone       = findViewById(R.id.progressDone);
        progressPending    = findViewById(R.id.progressPending);
        progressActivity   = findViewById(R.id.progressActivity);

        tvCurrentStreak    = findViewById(R.id.tvCurrentStreak);
        tvBestStreak       = findViewById(R.id.tvBestStreak);
        tvStreakMonth      = findViewById(R.id.tvStreakMonth);
        tvActivitySub      = findViewById(R.id.tvActivitySub);
        tvActivityPct      = findViewById(R.id.tvActivityPct);
        weekDotRow         = findViewById(R.id.weekDotRow);
        tvStreakHeaderCount = findViewById(R.id.tvStreakHeaderCount);

        navHome    = findViewById(R.id.navHome);
        navPlanner = findViewById(R.id.navPlanner);
        navTracker = findViewById(R.id.navTracker);
        navMusic   = findViewById(R.id.navMusic);
        navAbout   = findViewById(R.id.navAbout);

        ivHome    = findViewById(R.id.ivHome);
        ivPlanner = findViewById(R.id.ivPlanner);
        ivTracker = findViewById(R.id.ivTracker);
        ivMusic   = findViewById(R.id.ivMusic);
        ivAbout   = findViewById(R.id.ivAbout);

        tvHomeLabel    = findViewById(R.id.tvHomeLabel);
        tvPlannerLabel = findViewById(R.id.tvPlannerLabel);
        tvTrackerLabel = findViewById(R.id.tvTrackerLabel);
        tvMusicLabel   = findViewById(R.id.tvMusicLabel);
        tvAboutLabel   = findViewById(R.id.tvAboutLabel);

        // Set current month label
        if (tvStreakMonth != null) {
            String month = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
            tvStreakMonth.setText(month);
        }
    }

    // ─────────────────────────────────────────────────────────────────

    /** Load streak from SharedPrefs and populate the UI */
    private void loadStreakData() {
        SharedPreferences prefs = getSharedPreferences("StreakPrefs", MODE_PRIVATE);
        int currentStreak = prefs.getInt("currentStreak", 0);
        int bestStreak    = prefs.getInt("bestStreak", 0);

        // Keep best streak up-to-date
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
            prefs.edit().putInt("bestStreak", bestStreak).apply();
        }

        if (tvCurrentStreak    != null) tvCurrentStreak.setText(String.valueOf(currentStreak));
        if (tvBestStreak       != null) tvBestStreak.setText(String.valueOf(bestStreak));
        if (tvStreakHeaderCount != null) tvStreakHeaderCount.setText(String.valueOf(currentStreak));

        buildWeekDots(currentStreak);
    }

    /**
     * Draws 7 week-dot indicators.
     * The last `currentStreak` days (up to 7) are filled orange; the rest grey.
     */
    private void buildWeekDots(int currentStreak) {
        if (weekDotRow == null) return;
        weekDotRow.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int dotSize   = (int) (8 * density);
        int margin    = (int) (6 * density);

        for (int i = 0; i < 7; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dotSize);
            params.weight      = 1;
            params.leftMargin  = margin;
            params.rightMargin = margin;
            dot.setLayoutParams(params);

            boolean active = (i >= (7 - Math.min(currentStreak, 7)));
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(active ? Color.parseColor("#f97316") : Color.parseColor("#e2e8f0"));
            dot.setBackground(gd);

            weekDotRow.addView(dot);
        }
    }

    // ─────────────────────────────────────────────────────────────────

    /** Load task stats from Firestore and compute XP / Rank / bars */
    private void loadTaskStats() {
        com.google.firebase.firestore.Query query = firebaseHelper.getTasksQuery();
        if (query == null) return;

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int total = 0, completed = 0, pending = 0;
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                TaskModel task = doc.toObject(TaskModel.class);
                total++;
                if (task.isCompleted()) completed++;
                else pending++;
            }

            int streak     = getSharedPreferences("StreakPrefs", MODE_PRIVATE).getInt("currentStreak", 0);
            int xp         = completed * 10 + streak * 5;
            int rate       = total > 0 ? (int) ((completed * 100.0) / total) : 0;
            int pendingRate = total > 0 ? (int) ((pending * 100.0) / total) : 0;

            // Stats bar
            if (tvXP != null)       tvXP.setText(String.valueOf(xp));
            if (tvActivities != null) tvActivities.setText(String.valueOf(total));
            if (tvRank != null) {
                String rank = rate >= 90 ? "S" : rate >= 70 ? "A" : rate >= 50 ? "B" : rate >= 30 ? "C" : "D";
                tvRank.setText(rank);
            }

            // Task summary
            final int fc = completed, ft = total;
            if (tvTasksRatio    != null) tvTasksRatio.setText(fc + "/" + ft);
            if (tvCompletionPct != null) tvCompletionPct.setText(rate + "%");
            if (tvDonePct       != null) tvDonePct.setText(rate + "%");
            if (tvPendingPct    != null) tvPendingPct.setText(pendingRate + "%");

            if (progressTaskCircle != null) progressTaskCircle.setProgress(rate);
            if (progressDone       != null) progressDone.setProgress(rate);
            if (progressPending    != null) progressPending.setProgress(pendingRate);

            // Activity card
            if (progressActivity != null) progressActivity.setProgress(rate);
            if (tvActivitySub    != null) tvActivitySub.setText(fc + " tasks completed");
            if (tvActivityPct    != null) tvActivityPct.setText(rate + "% overall progress");
        });
    }

    // ─────────────────────────────────────────────────────────────────

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v ->
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(TrackerActivity.this, DashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }, 120));

        navPlanner.setOnClickListener(v ->
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(TrackerActivity.this, AddPlannerActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }, 120));

        // Already on Tracker — no-op
        navTracker.setOnClickListener(v -> { });

        navMusic.setOnClickListener(v ->
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(TrackerActivity.this, ChallengesActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }, 120));

        navAbout.setOnClickListener(v ->
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(TrackerActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }, 120));
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
}
