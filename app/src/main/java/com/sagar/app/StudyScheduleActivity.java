package com.sagar.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudyScheduleActivity extends AppCompatActivity {

    private LinearLayout scheduleContainer;
    private StudyApi api;
    private EditText etGoals;
    private StudyResponse lastResponse;           // holds last AI response for saving
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_schedule);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        scheduleContainer = findViewById(R.id.scheduleContainer);
        etGoals = findViewById(R.id.etGoals);
        Button btnGenerate = findViewById(R.id.btnGenerate);
        api = StudyApiService.getClient().create(StudyApi.class);
        firebaseHelper = new FirebaseHelper();

        // "Add to Tasks" button — visible after AI plan is generated
        Button btnAddToTasks = findViewById(R.id.btnAddToTasks);
        if (btnAddToTasks != null) {
            btnAddToTasks.setOnClickListener(v -> saveAiPlanToTasks());
        }

        View tabTask = findViewById(R.id.tabTaskScheduler);
        View tabAi = findViewById(R.id.tabAiStudyPlan);

        if (tabTask != null) {
            tabTask.setOnClickListener(v -> {
                // Go back to planner task view
                Intent intent = new Intent(StudyScheduleActivity.this, PlannerActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            });
        }

        if (btnGenerate != null) {
            btnGenerate.setOnClickListener(v -> {
                String goal = etGoals != null ? etGoals.getText().toString().trim() : "";
                fetchSchedule(goal, "standard");
            });
        }

        View btnQuick = findViewById(R.id.btnQuickPlan);
        View btnWeekly = findViewById(R.id.btnWeekly);
        View btnDeep = findViewById(R.id.btnDeepFocus);

        View.OnClickListener presetClick = v -> {
            fetchSchedule(etGoals != null ? etGoals.getText().toString().trim() : "", presetNameForView(v));
        };
        if (btnQuick != null) btnQuick.setOnClickListener(presetClick);
        if (btnWeekly != null) btnWeekly.setOnClickListener(presetClick);
        if (btnDeep != null) btnDeep.setOnClickListener(presetClick);

        renderSampleSchedule();
    }

    private void fetchSchedule(String goal, String preset) {
        if (api == null) {
            renderSampleSchedule();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("goal", goal == null || goal.isEmpty() ? "General upskill" : goal);
        payload.put("dailyMinutes", 90);
        payload.put("days", 5);
        payload.put("preset", preset == null ? "standard" : preset);

        api.generateStudyPlan(payload).enqueue(new Callback<StudyResponse>() {
            @Override
            public void onResponse(Call<StudyResponse> call, Response<StudyResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StudyScheduleActivity.this, "AI unavailable, showing sample", Toast.LENGTH_SHORT).show();
                    renderSampleSchedule();
                    return;
                }
                lastResponse = response.body();
                renderApiSchedule(lastResponse);
                // Show the "Add to Tasks" button
                Button btnAdd = findViewById(R.id.btnAddToTasks);
                if (btnAdd != null) btnAdd.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<StudyResponse> call, Throwable t) {
                Toast.makeText(StudyScheduleActivity.this, "Network issue, showing sample", Toast.LENGTH_SHORT).show();
                renderSampleSchedule();
            }
        });
    }

    private String presetNameForView(View v) {
        int id = v.getId();
        if (id == R.id.btnQuickPlan) return "quick";
        if (id == R.id.btnWeekly) return "weekly";
        if (id == R.id.btnDeepFocus) return "deep";
        return "standard";
    }

    private void renderApiSchedule(StudyResponse data) {
        if (scheduleContainer == null || data == null || data.days == null) {
            renderSampleSchedule();
            return;
        }

        List<StudySlot> slots = new ArrayList<>();
        for (StudyResponse.Day day : data.days) {
            if (day == null || day.sessions == null) continue;
            for (StudyResponse.Session s : day.sessions) {
                if (s == null) continue;
                String focus = s.focus == null ? "" : s.focus;
                ColorPair colors = colorForFocus(focus);
                slots.add(new StudySlot(s.time != null ? s.time : "", s.title != null ? s.title : "Session",
                        s.durationMinutes + " mins", focus, colors.badgeColor, colors.badgeBg)
                        .withRaw(day.date != null ? day.date : "", s.durationMinutes, s.notes != null ? s.notes : ""));
            }
        }

        if (slots.isEmpty()) {
            Toast.makeText(this, "Empty AI plan, showing sample", Toast.LENGTH_SHORT).show();
            renderSampleSchedule();
            return;
        }

        renderSlots(slots);
    }

    private void renderSampleSchedule() {
        if (scheduleContainer == null) return;
        List<StudySlot> slots = Arrays.asList(
                new StudySlot("9:00 AM", "Python Fundamentals", "45 mins", "High focus", "#4E7BFE", "#EAF1FF"),
                new StudySlot("11:30 AM", "React Components", "30 mins", "Medium focus", "#FFA800", "#FFF4DF"),
                new StudySlot("3:00 PM", "Coding Practice", "60 mins", "Deep work", "#A56BFF", "#F3E9FF")
        );
        renderSlots(slots);
    }

    private void renderSlots(List<StudySlot> slots) {
        if (scheduleContainer == null) return;
        scheduleContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (StudySlot slot : slots) {
            View card = inflater.inflate(R.layout.item_study_slot, scheduleContainer, false);

            View timeBadge = card.findViewById(R.id.timeBadge);
            TextView tvTime = card.findViewById(R.id.tvTime);
            TextView tvTitle = card.findViewById(R.id.tvTitle);
            TextView tvDuration = card.findViewById(R.id.tvDuration);
            TextView tvFocus = card.findViewById(R.id.tvFocus);

            if (timeBadge != null) {
                timeBadge.setBackground(createRoundedColor(slot.badgeBg));
            }
            if (tvTime != null) tvTime.setText(slot.time);
            if (tvTitle != null) tvTitle.setText(slot.title);
            if (tvDuration != null) tvDuration.setText(slot.duration);
            if (tvFocus != null) {
                tvFocus.setText(slot.focus);
                tvFocus.setTextColor(android.graphics.Color.parseColor(slot.badgeColor));
            }

            TextView btnAddSlot = card.findViewById(R.id.btnAddSlot);
            if (btnAddSlot != null) {
                btnAddSlot.setOnClickListener(v -> {
                    String desc = slot.time
                            + (slot.rawDate != null && !slot.rawDate.isEmpty() ? " | " + slot.rawDate : "")
                            + " | " + slot.rawDurationMinutes + " mins"
                            + (slot.rawNotes != null && !slot.rawNotes.isEmpty() ? " | " + slot.rawNotes : "");
                    String priority = focusToPriority(slot.focus);
                    TaskModel task = new TaskModel(slot.title, desc, priority, "TASK");
                    firebaseHelper.addTask(task);
                    // Visual feedback — turn button green
                    btnAddSlot.setText("✓ Added");
                    btnAddSlot.setTextColor(android.graphics.Color.parseColor("#1B8E5B"));
                    btnAddSlot.setBackground(createRoundedColor("#E6F7F1"));
                    btnAddSlot.setClickable(false);
                    Toast.makeText(StudyScheduleActivity.this,
                            "\"" + slot.title + "\" added to tasks!", Toast.LENGTH_SHORT).show();
                });
            }

            scheduleContainer.addView(card);
        }
    }

    private ColorPair colorForFocus(String focus) {
        if (focus == null) return new ColorPair("#4E7BFE", "#EAF1FF");
        String f = focus.toLowerCase();
        if (f.contains("high")) return new ColorPair("#4E7BFE", "#EAF1FF");
        if (f.contains("medium")) return new ColorPair("#FFA800", "#FFF4DF");
        if (f.contains("deep")) return new ColorPair("#A56BFF", "#F3E9FF");
        return new ColorPair("#4E7BFE", "#EAF1FF");
    }

    private android.graphics.drawable.GradientDrawable createRoundedColor(String color) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(android.graphics.Color.parseColor(color));
        d.setCornerRadius(16f);
        return d;
    }

    private static class ColorPair {
        String badgeColor;
        String badgeBg;
        ColorPair(String badgeColor, String badgeBg) {
            this.badgeColor = badgeColor;
            this.badgeBg = badgeBg;
        }
    }

    private static class StudySlot {
        String time, title, duration, focus, badgeColor, badgeBg;
        // Raw data for per-card "Add to Task"
        String rawDate, rawNotes;
        int rawDurationMinutes;

        StudySlot(String time, String title, String duration, String focus, String badgeColor, String badgeBg) {
            this.time = time;
            this.title = title;
            this.duration = duration;
            this.focus = focus;
            this.badgeColor = badgeColor;
            this.badgeBg = badgeBg;
        }

        StudySlot withRaw(String date, int durationMinutes, String notes) {
            this.rawDate = date;
            this.rawDurationMinutes = durationMinutes;
            this.rawNotes = notes;
            return this;
        }
    }

    /** Saves each AI session as a TASK in Firestore (visible in Task Scheduler). */
    private void saveAiPlanToTasks() {
        if (lastResponse == null || lastResponse.days == null || lastResponse.days.isEmpty()) {
            Toast.makeText(this, "No AI plan to save. Generate one first.", Toast.LENGTH_SHORT).show();
            return;
        }

        int count = 0;
        for (StudyResponse.Day day : lastResponse.days) {
            if (day == null || day.sessions == null) continue;
            for (StudyResponse.Session s : day.sessions) {
                if (s == null) continue;
                String title = (s.title != null ? s.title : "Study Session");
                String dateStr = (day.date != null ? day.date : "");
                String timeStr = (s.time != null ? s.time : "");
                String desc = timeStr + (dateStr.isEmpty() ? "" : " | " + dateStr)
                        + " | " + s.durationMinutes + " mins"
                        + (s.notes != null && !s.notes.isEmpty() ? " | " + s.notes : "");
                String priority = focusToPriority(s.focus);
                TaskModel task = new TaskModel(title, desc, priority, "TASK");
                firebaseHelper.addTask(task);
                count++;
            }
        }

        int saved = count;
        Toast.makeText(this, saved + " sessions added to Task Scheduler!", Toast.LENGTH_LONG).show();

        // Navigate to planner so user can see them
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(StudyScheduleActivity.this, PlannerActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }, 1200);
    }

    private String focusToPriority(String focus) {
        if (focus == null) return "Medium";
        String f = focus.toLowerCase();
        if (f.contains("high")) return "High";
        if (f.contains("low")) return "Low";
        return "Medium";
    }
}
