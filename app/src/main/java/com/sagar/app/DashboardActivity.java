package com.sagar.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private ViewGroup bottomNavMenu;
    private LinearLayout navHome, navPlanner, navMusic, navAbout, navTracker;
    private ImageView ivHome, ivPlanner, ivMusic, ivAbout;
    private TextView tvHomeLabel, tvPlannerLabel, tvMusicLabel, tvAboutLabel;

    // Dashboard stat views (new Figma design)
    private TextView tvTotalTasks, tvCompletedTasks, tvCompletionRate;
    private TextView tvStatsMonth, tvLastUpdated;
    private ProgressBar progressCompletion;
    private TextView tvSubCompleted, tvSubPending;
    private TextView tvStudyTrend, tvFocusTrend;
    private TextView tvAiSummaryText;
    private TextView tvGreeting;
    private TextView tvHighTasks, tvMedLowTasks;
    private ProgressBar progressSubjectRing;

    // Focus Time timer views
    private TextView tvFocusDays, tvFocusHours, tvFocusMins, tvFocusSecs;
    private boolean timerRunning = false;
    private long focusElapsedSeconds = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        firebaseHelper = new FirebaseHelper();
        bindNavViews();
        bindStatViews();
        setGreeting();
        setupBottomNavigation();
        setupFocusTimer();
        loadDashboardStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void bindStatViews() {
        tvTotalTasks     = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvSubCompleted   = findViewById(R.id.tvSubCompleted);
        tvSubPending     = findViewById(R.id.tvSubPending);
        tvStudyTrend     = findViewById(R.id.tvStudyTrend);
        tvFocusTrend     = findViewById(R.id.tvFocusTrend);
        tvAiSummaryText  = findViewById(R.id.tvAiSummaryText);
        tvFocusDays      = findViewById(R.id.tvFocusDays);
        tvFocusHours     = findViewById(R.id.tvFocusHours);
        tvFocusMins      = findViewById(R.id.tvFocusMins);
        tvFocusSecs      = findViewById(R.id.tvFocusSecs);
        tvGreeting       = findViewById(R.id.tvGreeting);
        tvHighTasks      = findViewById(R.id.tvHighTasks);
        tvMedLowTasks    = findViewById(R.id.tvMedLowTasks);
        progressSubjectRing = findViewById(R.id.progressSubjectRing);
        // Legacy hidden views (still bound to avoid NPEs in Java)
        tvStatsMonth     = findViewById(R.id.tvStatsMonth);
        tvLastUpdated    = findViewById(R.id.tvLastUpdated);
        progressCompletion = findViewById(R.id.progressCompletion);

        // Load saved focus time
        focusElapsedSeconds = getSharedPreferences("FocusPrefs", MODE_PRIVATE)
                .getLong("elapsedSeconds", 0);
        updateFocusTimerDisplay(focusElapsedSeconds);

        // AI summary from SharedPrefs if available
        String aiText = getSharedPreferences("AiPrefs", MODE_PRIVATE)
                .getString("aiSummary", "");
        if (!aiText.isEmpty() && tvAiSummaryText != null) {
            tvAiSummaryText.setText(aiText);
        }

        // Wire AI refresh button
        TextView tvRefresh = findViewById(R.id.tvRefreshAi);
        if (tvRefresh != null) {
            tvRefresh.setOnClickListener(v -> {
                if (tvAiSummaryText != null) {
                    tvAiSummaryText.setText("Refreshing AI insights...");
                }
                Toast.makeText(this, "AI summary refreshed!", Toast.LENGTH_SHORT).show();
                loadAiSummary();
            });
        }
    }

    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeGreet = hour < 12 ? "Good Morning," : hour < 17 ? "Good Afternoon," : "Good Evening,";
        // Set time-based greeting line (tvGreetingLine1)
        TextView tvLine1 = findViewById(R.id.tvGreetingLine1);
        if (tvLine1 != null) tvLine1.setText(timeGreet);

        // Set name
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                ? user.getDisplayName() + "!!"
                : "there!!";
        if (tvGreeting != null) tvGreeting.setText(name);
    }

    private void loadDashboardStats() {
        com.google.firebase.firestore.Query query = firebaseHelper.getTasksQuery();
        if (query == null) return;

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int total = 0, completed = 0, pending = 0, high = 0, medLow = 0;
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                TaskModel task = doc.toObject(TaskModel.class);
                total++;
                if (task.isCompleted()) completed++;
                else pending++;
                String p = task.getPriority();
                if ("High".equalsIgnoreCase(p)) high++;
                else medLow++;
            }

            int rate = total > 0 ? (int) ((completed * 100.0) / total) : 0;

            if (tvTotalTasks != null)     tvTotalTasks.setText(String.valueOf(total));
            if (tvSubCompleted != null)   tvSubCompleted.setText(String.valueOf(completed));
            if (tvSubPending != null)     tvSubPending.setText(String.valueOf(pending));
            if (tvHighTasks != null)      tvHighTasks.setText(String.valueOf(high));
            if (tvMedLowTasks != null)    tvMedLowTasks.setText(String.valueOf(medLow));
            if (progressSubjectRing != null) progressSubjectRing.setProgress(rate);
            // Study Time = completed tasks (h) + fraction
            if (tvCompletedTasks != null) tvCompletedTasks.setText(String.valueOf(completed));
            if (tvCompletionRate != null) tvCompletionRate.setText(rate + "%");
            if (tvStudyTrend != null)     tvStudyTrend.setText("+" + Math.min(rate, 99) + "% vs last week");
            if (tvFocusTrend != null)     tvFocusTrend.setText("+" + Math.min(rate / 2, 50) + "% improvement");

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load stats", Toast.LENGTH_SHORT).show());
    }

    private void loadAiSummary() {
        // Pull pending tasks to craft a context-aware summary
        com.google.firebase.firestore.Query query = firebaseHelper.getTasksQuery();
        if (query == null) return;
        query.get().addOnSuccessListener(snap -> {
            int pending = 0; String firstPending = "";
            for (QueryDocumentSnapshot doc : snap) {
                TaskModel t = doc.toObject(TaskModel.class);
                if (!t.isCompleted()) {
                    pending++;
                    if (firstPending.isEmpty()) firstPending = t.getTitle();
                }
            }
            String summary = pending == 0
                    ? "Great job! All your tasks are complete. Keep the momentum going with a new goal."
                    : "You have " + pending + " pending task" + (pending > 1 ? "s" : "") + " today."
                    + (firstPending.isEmpty() ? "" : " Start with: \"" + firstPending + "\".");
            if (tvAiSummaryText != null) tvAiSummaryText.setText(summary);
            getSharedPreferences("AiPrefs", MODE_PRIVATE).edit()
                    .putString("aiSummary", summary).apply();
        });
    }

    private void setupFocusTimer() {
        Button btnStart = findViewById(R.id.btnStartFocus);
        if (btnStart == null) return;
        btnStart.setOnClickListener(v -> {
            if (timerRunning) {
                timerRunning = false;
                timerHandler.removeCallbacks(timerRunnable);
                btnStart.setText("Resume Focus Time");
                // Save elapsed time
                getSharedPreferences("FocusPrefs", MODE_PRIVATE).edit()
                        .putLong("elapsedSeconds", focusElapsedSeconds).apply();
            } else {
                timerRunning = true;
                btnStart.setText("Pause Focus Time");
                timerRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!timerRunning) return;
                        focusElapsedSeconds++;
                        updateFocusTimerDisplay(focusElapsedSeconds);
                        timerHandler.postDelayed(this, 1000);
                    }
                };
                timerHandler.post(timerRunnable);
            }
        });
    }

    private void updateFocusTimerDisplay(long totalSeconds) {
        long days    = totalSeconds / 86400;
        long hours   = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs    = totalSeconds % 60;
        if (tvFocusDays  != null) tvFocusDays.setText(String.valueOf(days));
        if (tvFocusHours != null) tvFocusHours.setText(String.format(Locale.getDefault(), "%02d", hours));
        if (tvFocusMins  != null) tvFocusMins.setText(String.format(Locale.getDefault(), "%02d", minutes));
        if (tvFocusSecs  != null) tvFocusSecs.setText(String.format(Locale.getDefault(), "%02d", secs));
    }

    @Override
    protected void onStop() {
        super.onStop();
        timerRunning = false;
        timerHandler.removeCallbacksAndMessages(null);
        getSharedPreferences("FocusPrefs", MODE_PRIVATE).edit()
                .putLong("elapsedSeconds", focusElapsedSeconds).apply();
    }

    private void bindNavViews() {
        bottomNavMenu = findViewById(R.id.bottomNavMenu);
        navHome    = findViewById(R.id.navHome);
        navPlanner = findViewById(R.id.navPlanner);
        navTracker = findViewById(R.id.navTracker);
        navMusic   = findViewById(R.id.navMusic);
        navAbout   = findViewById(R.id.navAbout);
        ivHome    = findViewById(R.id.ivHome);
        ivPlanner = findViewById(R.id.ivPlanner);
        ivMusic   = findViewById(R.id.ivMusic);
        ivAbout   = findViewById(R.id.ivAbout);
        tvHomeLabel    = findViewById(R.id.tvHomeLabel);
        tvPlannerLabel = findViewById(R.id.tvPlannerLabel);
        tvMusicLabel   = findViewById(R.id.tvMusicLabel);
        tvAboutLabel   = findViewById(R.id.tvAboutLabel);

        // Update streak badge in header
        android.widget.TextView tvStreakBadge = findViewById(R.id.tvStreakCount);
        if (tvStreakBadge != null) {
            int streak = getSharedPreferences("StreakPrefs", MODE_PRIVATE).getInt("currentStreak", 0);
            tvStreakBadge.setText(String.valueOf(streak));
        }
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
        if (navTracker != null) {
            navTracker.setOnClickListener(v -> {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    Intent intent = new Intent(DashboardActivity.this, TrackerActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                }, 120);
            });
        }
        navMusic.setOnClickListener(v -> {
            setActiveNavItem(navMusic);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(DashboardActivity.this, ChallengesActivity.class);
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
