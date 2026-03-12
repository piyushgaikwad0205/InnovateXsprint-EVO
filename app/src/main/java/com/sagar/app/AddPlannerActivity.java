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
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AddPlannerActivity extends AppCompatActivity {

    private ViewGroup bottomNavMenu;
    private LinearLayout navHome, navPlanner, navMusic, navAbout;
    private ImageView ivHome, ivPlanner, ivMusic, ivAbout;
    private TextView tvHomeLabel, tvPlannerLabel, tvMusicLabel, tvAboutLabel;
    private RelativeLayout btnBack, btnSave;
    private android.widget.EditText etTourAbout, etRemarks, etDate;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_planner);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firebaseHelper = new FirebaseHelper();
        initializeViews();
        setupBottomNavigation();
        setActiveNavItem(navPlanner);

        btnBack.setOnClickListener(v -> {
            onBackPressed();
        });

        btnSave.setOnClickListener(v -> {
            saveTask();
        });

        // Date Picker
        etDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        final java.util.Calendar c = java.util.Calendar.getInstance();
        int year = c.get(java.util.Calendar.YEAR);
        int month = c.get(java.util.Calendar.MONTH);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> etDate
                        .setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1),
                year, month, day);
        datePickerDialog.show();
    }

    private void saveTask() {
        String title = etTourAbout.getText().toString().trim();
        String description = etRemarks.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        if (title.isEmpty()) {
            android.widget.Toast.makeText(this, "Please enter a task title", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        TaskModel task = new TaskModel(title, description + " (Date: " + date + ")", "Medium", "TOUR");
        firebaseHelper.addTask(task);

        android.widget.Toast.makeText(this, "Task saved successfully!", android.widget.Toast.LENGTH_SHORT).show();

        // Navigate to PlannerActivity when save is clicked
        Intent intent = new Intent(AddPlannerActivity.this, PlannerActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void initializeViews() {
        etTourAbout = findViewById(R.id.etTourAbout);
        etRemarks = findViewById(R.id.etRemarks);
        etDate = findViewById(R.id.etDate);

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

        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            setActiveNavItem(navHome);
            Intent intent = new Intent(AddPlannerActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        navPlanner.setOnClickListener(v -> setActiveNavItem(navPlanner));

        navMusic.setOnClickListener(v -> {
            setActiveNavItem(navMusic);
            Intent intent = new Intent(AddPlannerActivity.this, MusicActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        navAbout.setOnClickListener(v -> {
            setActiveNavItem(navAbout);
            Intent intent = new Intent(AddPlannerActivity.this, ProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
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

        activeItem.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));

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
        item.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
        item.setScaleX(1.0f);
        item.setScaleY(1.0f);
        iv.setColorFilter(Color.parseColor("#888888"));
        if (tv != null)
            tv.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AddPlannerActivity.this, PlannerActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
