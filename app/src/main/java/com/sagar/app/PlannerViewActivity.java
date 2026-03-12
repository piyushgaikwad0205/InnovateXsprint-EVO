package com.sagar.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PlannerViewActivity extends AppCompatActivity {

    private ViewGroup bottomNavMenu;
    private LinearLayout navHome, navPlanner, navMusic, navAbout;
    private ImageView ivHome, ivPlanner, ivMusic, ivAbout;
    private TextView tvHomeLabel, tvPlannerLabel, tvMusicLabel, tvAboutLabel;

    private FirebaseHelper firebaseHelper;
    private android.widget.LinearLayout taskListContainer;
    private android.widget.TextView tvTaskCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planner_view);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        firebaseHelper = new FirebaseHelper();
        initializeViews();
        setupBottomNavigation();
        setActiveNavItem(navPlanner);
        loadUserData();
        loadRecentTasks();

        // Add tour button → go to AddPlannerActivity
        View btnAddTour = findViewById(R.id.btnAddTour);
        if (btnAddTour != null) {
            btnAddTour.setOnClickListener(v -> {
                Intent intent = new Intent(PlannerViewActivity.this, AddPlannerActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentTasks();
    }

    private void loadRecentTasks() {
        // Find container in the layout for tasks
        if (taskListContainer == null) {
            taskListContainer = findViewById(R.id.recentTasksContainer);
        }
        if (taskListContainer == null)
            return;

        taskListContainer.removeAllViews();

        com.google.firebase.firestore.Query query = firebaseHelper.getTasksQuery();
        if (query == null)
            return;

        query.limit(10).get().addOnSuccessListener(queryDocumentSnapshots -> {
            int pending = 0, done = 0;
            taskListContainer.removeAllViews();

            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                TaskModel task = doc.toObject(TaskModel.class);
                task.setId(doc.getId());

                int layoutId = task.isCompleted() ? R.layout.item_planner_task_completed : R.layout.item_planner_task;

                android.view.View itemView = android.view.LayoutInflater.from(this)
                        .inflate(layoutId, taskListContainer, false);
                android.widget.TextView tvName = itemView.findViewById(R.id.tvTaskName);
                if (tvName != null) {
                    tvName.setText(task.getTitle());
                    if (task.isCompleted()) {
                        tvName.setPaintFlags(tvName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                        done++;
                    } else {
                        pending++;
                    }
                }
                taskListContainer.addView(itemView);
            }

            if (tvTaskCount != null) {
                tvTaskCount.setText(pending + " pending • " + done + " done");
            }

            if (queryDocumentSnapshots.isEmpty()) {
                android.widget.TextView empty = new android.widget.TextView(this);
                empty.setText("No tasks yet. Tap '+ Add tour' to create one!");
                empty.setTextColor(android.graphics.Color.parseColor("#888888"));
                empty.setTextSize(14f);
                empty.setPadding(0, 24, 0, 24);
                taskListContainer.addView(empty);
            }
        });
    }

    private void loadUserData() {
        // Set today's date
        TextView tvTodayDate = findViewById(R.id.tvTodayDate);
        if (tvTodayDate != null) {
            String today = new java.text.SimpleDateFormat("'Today''s' , dd MMM yyyy", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            tvTodayDate.setText(today);
        }

        // Set time-based greeting
        TextView tvGreetingText = findViewById(R.id.tvGreetingText);
        if (tvGreetingText != null) {
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            String greeting;
            if (hour >= 5 && hour < 12)
                greeting = "Good Morning";
            else if (hour >= 12 && hour < 17)
                greeting = "Good Afternoon";
            else if (hour >= 17 && hour < 21)
                greeting = "Good Evening";
            else
                greeting = "Good Night";
            tvGreetingText.setText(greeting);
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

        // Dynamic task list views
        tvTaskCount = findViewById(R.id.tvTaskCount);
        taskListContainer = findViewById(R.id.recentTasksContainer);

    }

    private void setupBottomNavigation() {
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                setActiveNavItem(navHome);
                Intent intent = new Intent(PlannerViewActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
                finish();
            });
        }

        if (navPlanner != null) {
            navPlanner.setOnClickListener(v -> setActiveNavItem(navPlanner));
        }

        if (navMusic != null) {
            navMusic.setOnClickListener(v -> {
                setActiveNavItem(navMusic);
                Intent intent = new Intent(PlannerViewActivity.this, MusicActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
                finish();
            });
        }

        if (navAbout != null) {
            navAbout.setOnClickListener(v -> {
                setActiveNavItem(navAbout);
                Intent intent = new Intent(PlannerViewActivity.this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
                finish();
            });
        }
    }

    private void setActiveNavItem(LinearLayout activeItem) {
        if (activeItem == null || bottomNavMenu == null)
            return;

        // High-end bouncy transition for the bottom nav island
        android.transition.TransitionSet set = new android.transition.TransitionSet()
                .addTransition(new android.transition.ChangeBounds())
                .addTransition(new android.transition.Fade())
                .setDuration(350)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f));

        android.transition.TransitionManager.beginDelayedTransition(bottomNavMenu, set);

        // Reset all items
        resetNavItem(navHome, ivHome, tvHomeLabel);
        resetNavItem(navPlanner, ivPlanner, tvPlannerLabel);
        resetNavItem(navMusic, ivMusic, tvMusicLabel);
        resetNavItem(navAbout, ivAbout, tvAboutLabel);

        // Set active item with bouncy layout
        activeItem.setBackgroundResource(R.drawable.active_tab_bg);
        activeItem.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1.8f));

        // Micro-animation for the icon: subtle scale pop
        activeItem.setScaleX(0.9f);
        activeItem.setScaleY(0.9f);
        activeItem.animate().scaleX(1.0f).scaleY(1.0f).setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2.0f)).start();

        if (activeItem == navHome) {
            ivHome.setColorFilter(Color.WHITE);
            tvHomeLabel.setVisibility(View.VISIBLE);
        } else if (activeItem == navPlanner) {
            ivPlanner.setColorFilter(Color.WHITE);
            tvPlannerLabel.setVisibility(View.VISIBLE);
        } else if (activeItem == navMusic) {
            ivMusic.setColorFilter(Color.WHITE);
            tvMusicLabel.setVisibility(View.VISIBLE);
        } else if (activeItem == navAbout) {
            ivAbout.setColorFilter(Color.WHITE);
            tvAboutLabel.setVisibility(View.VISIBLE);
        }
    }

    private void resetNavItem(LinearLayout item, ImageView iv, TextView tv) {
        item.setBackground(null);
        item.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 0.8f));
        iv.setColorFilter(Color.parseColor("#888888"));
        tv.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(PlannerViewActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
