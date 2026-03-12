package com.sagar.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PlannerActivity extends AppCompatActivity {

    private ViewGroup bottomNavMenu;
    private LinearLayout navHome, navPlanner, navMusic, navAbout;
    private ImageView ivHome, ivPlanner, ivMusic, ivAbout;
    private TextView tvHomeLabel, tvPlannerLabel, tvMusicLabel, tvAboutLabel;

    private LinearLayout activeTasksContainer, completedTasksContainer;
    private TextView tvTodayDate, tabDailyTasks, tabTourReminders;
    private String currentFilterType = "TASK"; // "TASK" or "TOUR"
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planner);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        firebaseHelper = new FirebaseHelper();
        initializeViews();
        setupBottomNavigation();
        setupTabSwitching();
        setActiveNavItem(navPlanner);
        setTodayDate();
        loadTasksFromFirebase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tasks whenever screen resumes
        loadTasksFromFirebase();
    }

    private void setTodayDate() {
        if (tvTodayDate != null) {
            String today = new SimpleDateFormat("'Today''s' , dd MMM yyyy", Locale.getDefault()).format(new Date());
            tvTodayDate.setText(today);
        }
    }

    private void loadTasksFromFirebase() {
        if (activeTasksContainer == null || completedTasksContainer == null)
            return;

        activeTasksContainer.removeAllViews();
        completedTasksContainer.removeAllViews();

        com.google.firebase.firestore.Query query = firebaseHelper.getTasksQuery();
        if (query == null) {
            Toast.makeText(this, "Please login to view tasks", Toast.LENGTH_SHORT).show();
            return;
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int activeCount = 0;
            int completedCount = 0;

            activeTasksContainer.removeAllViews();
            completedTasksContainer.removeAllViews();

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                TaskModel task = doc.toObject(TaskModel.class);
                task.setId(doc.getId());

                // Filter by type (Legacy tasks with null type default to TASK)
                String taskType = task.getType();
                if (taskType == null)
                    taskType = "TASK";

                if (!taskType.equals(currentFilterType))
                    continue;

                if (task.isCompleted()) {
                    addCompletedTaskView(task);
                    completedCount++;
                } else {
                    addActiveTaskView(task);
                    activeCount++;
                }
            }

            // Show empty states
            if (activeCount == 0) {
                TextView empty = new TextView(this);
                String msg = currentFilterType.equals("TASK") ? "No tasks for today!" : "No tours planned yet!";
                empty.setText(msg);
                empty.setTextColor(Color.parseColor("#888888"));
                empty.setTextSize(14f);
                empty.setPadding(0, 16, 0, 16);
                activeTasksContainer.addView(empty);
            }
            if (completedCount == 0) {
                TextView empty = new TextView(this);
                empty.setText("Nothing here.");
                empty.setTextColor(Color.parseColor("#AAAAAA"));
                empty.setTextSize(14f);
                empty.setPadding(0, 16, 0, 16);
                completedTasksContainer.addView(empty);
            }

        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load tasks", Toast.LENGTH_SHORT).show());
    }

    private void addActiveTaskView(TaskModel task) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_planner_task, activeTasksContainer, false);
        TextView tvName = itemView.findViewById(R.id.tvTaskName);
        View checkBtn = itemView.findViewById(R.id.ivCheck);

        tvName.setText(task.getTitle());

        // Click check = mark complete
        checkBtn.setAlpha(1.0f);
        checkBtn.setOnClickListener(v -> {
            firebaseHelper.updateTaskStatus(task.getId(), true);
            Toast.makeText(this, "Task completed! ✅", Toast.LENGTH_SHORT).show();
            loadTasksFromFirebase();
        });

        // Long press = delete
        itemView.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Task")
                    .setMessage("\"" + task.getTitle() + "\" delete karna chahte ho?")
                    .setPositiveButton("Delete", (d, w) -> {
                        firebaseHelper.deleteTask(task.getId());
                        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                        loadTasksFromFirebase();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        activeTasksContainer.addView(itemView);
    }

    private void addCompletedTaskView(TaskModel task) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_planner_task_completed, completedTasksContainer,
                false);
        TextView tvName = itemView.findViewById(R.id.tvTaskName);
        View checkBtn = itemView.findViewById(R.id.ivCheck);

        tvName.setText(task.getTitle());
        tvName.setPaintFlags(tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        // Click = mark back to pending
        if (checkBtn != null) {
            checkBtn.setOnClickListener(v -> {
                firebaseHelper.updateTaskStatus(task.getId(), false);
                Toast.makeText(this, "Task moved to pending", Toast.LENGTH_SHORT).show();
                loadTasksFromFirebase();
            });
        }

        // Long press = delete
        itemView.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Task")
                    .setMessage("\"" + task.getTitle() + "\" delete karna chahte ho?")
                    .setPositiveButton("Delete", (d, w) -> {
                        firebaseHelper.deleteTask(task.getId());
                        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                        loadTasksFromFirebase();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        completedTasksContainer.addView(itemView);
    }

    private void initializeViews() {
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

        activeTasksContainer = findViewById(R.id.activeTasksContainer);
        completedTasksContainer = findViewById(R.id.completedTasksContainer);
        tabDailyTasks = findViewById(R.id.tabDailyTasks);
        tabTourReminders = findViewById(R.id.tabTourReminders);
        tvTodayDate = findViewById(R.id.tvTodayDate);
    }

    private void setupTabSwitching() {
        if (tabDailyTasks != null) {
            tabDailyTasks.setOnClickListener(v -> {
                currentFilterType = "TASK";
                updateTabUI();
                loadTasksFromFirebase();
            });
        }

        if (tabTourReminders != null) {
            tabTourReminders.setOnClickListener(v -> {
                currentFilterType = "TOUR";
                updateTabUI();
                loadTasksFromFirebase();
            });
        }
        updateTabUI(); // Call initially to set the correct UI state
    }

    private void updateTabUI() {
        if (tabDailyTasks == null || tabTourReminders == null)
            return;

        if (currentFilterType.equals("TASK")) {
            tabDailyTasks.setBackgroundResource(R.drawable.task_badge_bg);
            tabDailyTasks.setTextColor(Color.BLACK);
            tabDailyTasks.setTypeface(null, android.graphics.Typeface.BOLD);

            tabTourReminders.setBackground(null);
            tabTourReminders.setTextColor(Color.parseColor("#888888"));
            tabTourReminders.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            tabTourReminders.setBackgroundResource(R.drawable.task_badge_bg);
            tabTourReminders.setTextColor(Color.BLACK);
            tabTourReminders.setTypeface(null, android.graphics.Typeface.BOLD);

            tabDailyTasks.setBackground(null);
            tabDailyTasks.setTextColor(Color.parseColor("#888888"));
            tabDailyTasks.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            setActiveNavItem(navHome);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(PlannerActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });

        navPlanner.setOnClickListener(v -> {
            setActiveNavItem(navPlanner);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(PlannerActivity.this, AddPlannerActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });

        navMusic.setOnClickListener(v -> {
            setActiveNavItem(navMusic);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(PlannerActivity.this, MusicActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });

        navAbout.setOnClickListener(v -> {
            setActiveNavItem(navAbout);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(PlannerActivity.this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });
    }

    private void setActiveNavItem(LinearLayout activeItem) {
        android.transition.TransitionSet set = new android.transition.TransitionSet()
                .addTransition(new android.transition.ChangeBounds())
                .addTransition(new android.transition.Fade())
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.0f));
        android.transition.TransitionManager.beginDelayedTransition(bottomNavMenu, set);

        resetNavItem(navHome, ivHome, tvHomeLabel);
        resetNavItem(navPlanner, ivPlanner, tvPlannerLabel);
        resetNavItem(navMusic, ivMusic, tvMusicLabel);
        resetNavItem(navAbout, ivAbout, tvAboutLabel);

        activeItem.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
        activeItem.setScaleX(0.9f);
        activeItem.setScaleY(0.9f);
        activeItem.animate().scaleX(1.1f).scaleY(1.1f).setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2.0f)).start();

        if (activeItem == navHome) {
            ivHome.setColorFilter(Color.BLACK);
        } else if (activeItem == navPlanner) {
            ivPlanner.setColorFilter(Color.BLACK);
        } else if (activeItem == navMusic) {
            ivMusic.setColorFilter(Color.BLACK);
        } else if (activeItem == navAbout) {
            ivAbout.setColorFilter(Color.BLACK);
        }
    }

    private void resetNavItem(LinearLayout item, ImageView iv, TextView tv) {
        item.setBackground(null);
        item.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
        item.setScaleX(1.0f);
        item.setScaleY(1.0f);
        iv.setColorFilter(Color.parseColor("#888888"));
        if (tv != null)
            tv.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(PlannerActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
