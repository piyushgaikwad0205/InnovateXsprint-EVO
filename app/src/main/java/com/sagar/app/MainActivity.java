package com.sagar.app;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.view.ViewGroup;
import android.view.View;
import android.graphics.Color;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private TextView tvDays, tvHours, tvMinutes, tvSeconds;
    private TextView btnStartFocus, btnSetFocus, btnStopFocus;
    private ImageView btnPlayMusic;
    private FloatingActionButton fabCreateTask;
    private Handler timerHandler = new Handler(Looper.getMainLooper()); // Initialized here
    private Runnable timerRunnable;
    private long remainingSeconds = 0;
    private boolean isRunning = false;
    private boolean isCountdown = false;

    // Stats View
    private TextView tvTotalTasks, tvCompletedTasks, tvPendingTasks, tvCompletionRate;
    private TextView tvStatsMonth, tvLastUpdated;
    private ProgressBar progressCompletion;
    private FirebaseHelper firebaseHelper; // Added FirebaseHelper

    private ViewGroup bottomNavMenu;
    private LinearLayout navHome, navPlanner, navMusic, navAbout;
    private ImageView ivHome, ivPlanner, ivMusic, ivAbout;
    private TextView tvHomeLabel, tvPlannerLabel, tvMusicLabel, tvAboutLabel;
    private TextView tvAiSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseHelper = new FirebaseHelper(); // Added initialization

        // Initialize Firebase Auth
        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        } else {
            // Check if profile is complete
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists() || !documentSnapshot.contains("profileComplete")
                                || !documentSnapshot.getBoolean("profileComplete")) {
                            android.content.Intent intent = new android.content.Intent(this,
                                    EditProfileActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
        }

        // Clip scroll content to the grey container's rounded corners
        android.widget.LinearLayout greyContainer = findViewById(R.id.greyContentContainer);
        if (greyContainer != null) {
            greyContainer.setClipToOutline(true);
        }

        // Bind timer views
        tvDays = findViewById(R.id.tvDays);
        tvHours = findViewById(R.id.tvHours);
        tvMinutes = findViewById(R.id.tvMinutes);
        tvSeconds = findViewById(R.id.tvSeconds);
        btnStartFocus = findViewById(R.id.btnStartFocus);
        btnSetFocus = findViewById(R.id.btnSetFocus);
        btnStopFocus = findViewById(R.id.btnStopFocus);
        btnPlayMusic = findViewById(R.id.btnPlayMusic);
        fabCreateTask = findViewById(R.id.fabCreateTask);

        // 1. Initialize Navigation
        bottomNavMenu = findViewById(R.id.bottomNavMenu);
        navHome = findViewById(R.id.navHome);
        navPlanner = findViewById(R.id.navPlanner);
        navMusic = findViewById(R.id.navMusic);
        navAbout = findViewById(R.id.navAbout);
        navAbout = findViewById(R.id.navAbout);

        ivHome = findViewById(R.id.ivHome);
        ivPlanner = findViewById(R.id.ivPlanner);
        ivMusic = findViewById(R.id.ivMusic);
        ivAbout = findViewById(R.id.ivAbout);

        tvHomeLabel = findViewById(R.id.tvHomeLabel);
        tvPlannerLabel = findViewById(R.id.tvPlannerLabel);
        tvMusicLabel = findViewById(R.id.tvMusicLabel);
        tvAboutLabel = findViewById(R.id.tvAboutLabel);

        setupBottomNavigation();

        // Set up music button
        if (btnPlayMusic != null) {
            btnPlayMusic.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, MusicActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // Set up floating create button
        if (fabCreateTask != null) {
            fabCreateTask.setOnClickListener(v -> showTaskCreationSheet());
        }

        // Set up refresh button for AI Summary
        ImageView btnRefreshAiSummary = findViewById(R.id.btnRefreshAiSummary);
        TextView tvRefreshLabel = findViewById(R.id.tvRefreshLabel);

        View.OnClickListener refreshClickListener = v -> {
            // Show loading state
            tvAiSummary.setText("Refreshing...");
            // Regenerate motivation with new random message
            setupAiSummary();
        };

        if (btnRefreshAiSummary != null) {
            btnRefreshAiSummary.setOnClickListener(refreshClickListener);
        }
        if (tvRefreshLabel != null) {
            tvRefreshLabel.setOnClickListener(refreshClickListener);
        }

        // Set up refresh button for Motivation blog
        ImageView btnRefreshMotivation = findViewById(R.id.btnRefreshMotivation);
        if (btnRefreshMotivation != null) {
            btnRefreshMotivation.setOnClickListener(v -> {
                // You can add logic here to refresh the motivation blog content
                android.widget.Toast.makeText(this, "Refreshing motivation...", android.widget.Toast.LENGTH_SHORT)
                        .show();
            });
        }

        // Set up streak click handler
        LinearLayout navStreak = findViewById(R.id.navStreak);
        if (navStreak != null) {
            navStreak.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Current streak: " +
                        getSharedPreferences("StreakPrefs", MODE_PRIVATE).getInt("currentStreak", 0) + " days! 🔥",
                        android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        // Set AI Summary text with bold keywords
        setupAiSummary();

        // Set up streak display and tracking
        updateStreakDisplay();
        updateStreak(); // Track daily activity

        // Set up timer handler
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    if (isCountdown) {
                        if (remainingSeconds > 0) {
                            remainingSeconds--;
                            updateTimerDisplay(remainingSeconds);
                            timerHandler.postDelayed(this, 1000);
                        } else {
                            isRunning = false;
                            btnStartFocus.setText("Start");
                            android.widget.Toast.makeText(MainActivity.this, "Focus Time Completed! 🎉",
                                    android.widget.Toast.LENGTH_LONG).show();
                            try {
                                stopLockTask();
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        // Count up mode
                        remainingSeconds++;
                        updateTimerDisplay(remainingSeconds);
                        timerHandler.postDelayed(this, 1000);
                    }
                }
            }
        };

        if (btnSetFocus != null) {
            btnSetFocus.setOnClickListener(v -> {
                if (isRunning) {
                    android.widget.Toast.makeText(this, "Pause timer to set time", android.widget.Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                android.widget.EditText input = new android.widget.EditText(this);
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                input.setHint("Minutes");
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Set Focus Time (Minutes)")
                        .setView(input)
                        .setPositiveButton("Set", (dialog, which) -> {
                            String val = input.getText().toString();
                            if (!val.isEmpty()) {
                                long mins = Long.parseLong(val);
                                remainingSeconds = mins * 60;
                                isCountdown = remainingSeconds > 0;
                                updateTimerDisplay(remainingSeconds);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (btnStartFocus != null) {
            btnStartFocus.setOnClickListener(v -> {
                if (!isRunning) {
                    isRunning = true;
                    btnStartFocus.setText("Pause");
                    timerHandler.postDelayed(timerRunnable, 1000);
                    // Lock app to prevent leaving during focus
                    try {
                        startLockTask();
                    } catch (Exception ignored) {
                    }
                } else {
                    isRunning = false;
                    btnStartFocus.setText("Resume");
                    timerHandler.removeCallbacks(timerRunnable);
                }
            });
        }

        if (btnStopFocus != null) {
            btnStopFocus.setOnClickListener(v -> {
                isRunning = false;
                remainingSeconds = 0;
                isCountdown = false;
                updateTimerDisplay(remainingSeconds);
                if (btnStartFocus != null)
                    btnStartFocus.setText("Start");
                timerHandler.removeCallbacks(timerRunnable);
                try {
                    stopLockTask();
                } catch (Exception ignored) {
                }
            });
        }

        // Load user name
        loadUserData();

        // New: Load Stats
        initializeStatsViews();
        loadDashboardStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void initializeStatsViews() {
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = findViewById(R.id.tvPendingTasks);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvStatsMonth = findViewById(R.id.tvStatsMonth);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        progressCompletion = findViewById(R.id.progressCompletion);

        // Set current month
        if (tvStatsMonth != null) {
            String month = new java.text.SimpleDateFormat("MMMM yyyy 'Stats'", java.util.Locale.getDefault())
                    .format(new java.util.Date());
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

            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
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
                tvCompletionRate.setText("Overall Progress: " + rate + "%");
            if (progressCompletion != null)
                progressCompletion.setProgress(rate);
            if (tvLastUpdated != null) {
                String now = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(new java.util.Date());
                tvLastUpdated.setText("Updated: " + now);
            }

        });
    }

    private void loadUserData() {
        // Set time-based greeting
        android.widget.TextView tvGreeting = findViewById(R.id.tvGreeting);
        if (tvGreeting != null) {
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            String greeting;
            if (hour >= 5 && hour < 12) {
                greeting = "Good Morning,";
            } else if (hour >= 12 && hour < 17) {
                greeting = "Good Afternoon,";
            } else if (hour >= 17 && hour < 21) {
                greeting = "Good Evening,";
            } else {
                greeting = "Good Night,";
            }
            tvGreeting.setText(greeting);
        }

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user != null) {
            String fullName = user.getDisplayName();
            TextView tvName = findViewById(R.id.tvGreetingName);
            if (tvName != null) {
                if (fullName != null && !fullName.isEmpty()) {
                    String firstName = fullName.split(" ")[0];
                    tvName.setText(firstName + "!!");
                } else {
                    // Fallback to Firestore if display name is not set
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String name = documentSnapshot.getString("name");
                                    if (name != null && !name.isEmpty()) {
                                        String firstName = name.split(" ")[0];
                                        tvName.setText(firstName + "!!");
                                    }
                                }
                            });
                }
            }
        }
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> setActiveNavItem(navHome));
        navPlanner.setOnClickListener(v -> {
            setActiveNavItem(navPlanner);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, AddPlannerActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });
        navMusic.setOnClickListener(v -> {
            setActiveNavItem(navMusic);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, MusicActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });
        navAbout.setOnClickListener(v -> {
            setActiveNavItem(navAbout);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });
    }

    private void setActiveNavItem(LinearLayout activeItem) {
        // Simple color change for active state
        resetNavItem(navHome, ivHome, tvHomeLabel, R.drawable.ic_home);
        resetNavItem(navPlanner, ivPlanner, tvPlannerLabel, R.drawable.ic_location);
        resetNavItem(navMusic, ivMusic, tvMusicLabel, R.drawable.ic_music);
        resetNavItem(navAbout, ivAbout, tvAboutLabel, R.drawable.ic_profile);

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

    private void resetNavItem(android.widget.LinearLayout item, android.widget.ImageView iv,
            android.widget.TextView tv, int drawableRes) {
        iv.setColorFilter(Color.parseColor("#888888"));
        tv.setTextColor(Color.parseColor("#888888"));
    }

    /**
     * Updates the four timer TextViews from total elapsed seconds.
     */
    private void updateTimerDisplay(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        tvDays.setText(String.valueOf(days));
        tvHours.setText(String.format("%02d", hours));
        tvMinutes.setText(String.format("%02d", minutes));
        tvSeconds.setText(String.format("%02d", seconds));
    }

    /**
     * Fetches pending tasks from Firestore and builds a structured AI summary.
     */
    private void setupAiSummary() {
        tvAiSummary = findViewById(R.id.tvAiSummary);
        if (tvAiSummary == null)
            return;

        // Show loading text first
        tvAiSummary.setText("Loading your tasks...");

        FirebaseHelper helper = new FirebaseHelper();
        com.google.firebase.firestore.Query query = helper.getTasksQuery();
        if (query == null) {
            tvAiSummary.setText("Login to see your task summary.");
            return;
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            java.util.List<String> pendingTasks = new java.util.ArrayList<>();
            int totalCompleted = 0;

            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                TaskModel task = doc.toObject(TaskModel.class);
                if (!task.isCompleted()) {
                    pendingTasks.add(task.getTitle());
                } else {
                    totalCompleted++;
                }
            }

            StringBuilder sb = new StringBuilder();

            if (pendingTasks.isEmpty()) {
                sb.append("Remaining Tasks:\n");
                sb.append("All tasks completed!\n\n");
                sb.append("Motivation:\n");
                sb.append(generateMotivation(true));
            } else {
                sb.append("Remaining Tasks:\n");
                for (String taskName : pendingTasks) {
                    sb.append("- ").append(taskName).append("\n");
                }
                sb.append("\nMotivation:\n");
                sb.append(generateMotivation(false));
            }

            tvAiSummary.setText(sb.toString());

        }).addOnFailureListener(e -> tvAiSummary.setText("Unable to load tasks. Please check your connection."));
    }

    /**
     * Generates a unique motivational message based on task completion status.
     */
    private String generateMotivation(boolean allCompleted) {
        java.util.Random random = new java.util.Random();

        if (allCompleted) {
            String[] messages = {
                    "Excellent work! You've cleared all your tasks. Take a moment to celebrate your achievements.",
                    "Outstanding! All tasks are complete. Your dedication is paying off.",
                    "Well done! You've accomplished everything. Ready to take on more challenges?",
                    "Perfect! All tasks completed. Your productivity is inspiring.",
                    "Fantastic! You've conquered all your tasks. Keep this momentum going!"
            };
            return messages[random.nextInt(messages.length)];
        } else {
            String[] messages = {
                    "Consistent effort leads to meaningful progress. Complete the next task and keep moving forward.",
                    "You are making steady progress toward your goals. Stay focused on the next task and maintain your momentum.",
                    "Every task you complete brings you closer to success. Tackle the next one with confidence.",
                    "Focus on one task at a time. Small steps lead to big achievements.",
                    "Your determination is building positive habits. Continue with the next task.",
                    "Progress happens when you commit to taking action. You've got this!",
                    "Stay present with your current task. Completion is within reach.",
                    "Each completed task strengthens your discipline. Keep pushing forward.",
                    "Your efforts today create the foundation for tomorrow's success.",
                    "Clarity and focus will help you conquer these remaining tasks efficiently."
            };
            return messages[random.nextInt(messages.length)];
        }
    }

    /**
     * Helper: applies bold StyleSpan to a specific substring within an SSB.
     */
    private void boldSubstring(SpannableStringBuilder ssb, String fullText, String target) {
        int start = fullText.indexOf(target);
        if (start >= 0) {
            ssb.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    start,
                    start + target.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Show task creation bottom sheet dialog
     */
    private void showTaskCreationSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_task_creation_sheet, null);
        dialog.setContentView(view);

        android.widget.EditText etTaskName = view.findViewById(R.id.etTaskName);
        View btnCreate = view.findViewById(R.id.btnCreate);

        // Create button click
        btnCreate.setOnClickListener(v -> {
            String taskName = etTaskName.getText().toString().trim();

            if (taskName.isEmpty()) {
                etTaskName.setError("Task name is required");
                return;
            }

            // Save task to Firestore (description is empty in this new UI)
            saveTaskToFirestore(taskName, "");

            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Save task to Firestore
     */
    private void saveTaskToFirestore(String taskName, String description) {
        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            android.widget.Toast.makeText(this, "Please login to create tasks", android.widget.Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        java.util.Map<String, Object> task = new java.util.HashMap<>();
        task.put("title", taskName);
        task.put("description", description != null ? description : "");
        task.put("completed", false);
        task.put("timestamp", System.currentTimeMillis());
        task.put("type", "TASK");

        // Use the same path as FirebaseHelper: users/{userId}/tasks/
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("tasks")
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    android.widget.Toast
                            .makeText(this, "Task created successfully! ✨", android.widget.Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(this, "Failed to create task: " + e.getMessage(),
                            android.widget.Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Update streak display in bottom navigation
     */
    private void updateStreakDisplay() {
        TextView tvStreakCount = findViewById(R.id.tvStreakCount);
        if (tvStreakCount == null)
            return;

        // Get current streak from SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("StreakPrefs", MODE_PRIVATE);
        int currentStreak = prefs.getInt("currentStreak", 0);

        tvStreakCount.setText(String.valueOf(currentStreak));
    }

    /**
     * Update user's streak based on last activity
     */
    private void updateStreak() {
        android.content.SharedPreferences prefs = getSharedPreferences("StreakPrefs", MODE_PRIVATE);
        long lastActiveTime = prefs.getLong("lastActiveTime", 0);
        int currentStreak = prefs.getInt("currentStreak", 0);

        long currentTime = System.currentTimeMillis();
        long oneDayInMillis = 24 * 60 * 60 * 1000;

        // If this is the first time or more than a day has passed
        if (lastActiveTime == 0 || (currentTime - lastActiveTime) > oneDayInMillis) {
            // Check if it's a new day (reset at midnight)
            java.util.Calendar lastCalendar = java.util.Calendar.getInstance();
            lastCalendar.setTimeInMillis(lastActiveTime);
            java.util.Calendar currentCalendar = java.util.Calendar.getInstance();
            currentCalendar.setTimeInMillis(currentTime);

            // If it's a different day
            if (lastCalendar.get(java.util.Calendar.DAY_OF_YEAR) != currentCalendar
                    .get(java.util.Calendar.DAY_OF_YEAR)) {
                // If less than 2 days since last activity, increment streak
                if (lastActiveTime > 0 && (currentTime - lastActiveTime) < (oneDayInMillis * 2)) {
                    currentStreak++;
                } else if (lastActiveTime > 0) {
                    // Streak broken, reset to 1
                    currentStreak = 1;
                } else {
                    // First time
                    currentStreak = 1;
                }

                // Save updated streak
                prefs.edit()
                        .putInt("currentStreak", currentStreak)
                        .putLong("lastActiveTime", currentTime)
                        .apply();

                updateStreakDisplay();
            }
        }

        // Update last active time
        prefs.edit().putLong("lastActiveTime", currentTime).apply();
    }

    /**
     * Increment streak (call this when user completes a task or focus session)
     */
    private void incrementStreak() {
        android.content.SharedPreferences prefs = getSharedPreferences("StreakPrefs", MODE_PRIVATE);
        int currentStreak = prefs.getInt("currentStreak", 0);
        currentStreak++;
        prefs.edit().putInt("currentStreak", currentStreak).apply();
        updateStreakDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}
