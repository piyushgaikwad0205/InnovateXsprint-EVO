package com.sagar.app;

import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChallengesActivity extends AppCompatActivity {

    // ── Icon palette (bg color / icon text color) ─────────────────────
    private static final int[] ICON_BG_COLORS = {
        0xFFE3F2FD, 0xFFE0F7FA, 0xFFF3E5F5, 0xFFE8F5E9, 0xFFFFF3E0,
        0xFFFCE4EC, 0xFFF1F8E9, 0xFFE8EAF6
    };
    private static final int[] ICON_TEXT_COLORS = {
        0xFF1976D2, 0xFF0097A7, 0xFF7B1FA2, 0xFF388E3C, 0xFFE65100,
        0xFFC62828, 0xFF558B2F, 0xFF283593
    };

    // ── Bottom nav ────────────────────────────────────────────────────
    private LinearLayout navHome, navPlanner, navMusic, navAbout;
    private ImageView ivHome, ivPlanner, ivMusic, ivAbout;
    private TextView tvHomeLabel, tvPlannerLabel, tvMusicLabel, tvAboutLabel;

    // ── Screen views ─────────────────────────────────────────────────
    private LinearLayout llChallengesContainer;
    private LinearLayout llEmptyState;
    private EditText etPlaylistUrl;
    private TextView btnAddChallenge;

    private FirebaseFirestore db;
    private String userId;

    // Keep loaded challenges for rebinding after mark-done
    private final List<ChallengeModel> loadedChallenges = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenges);

        // Auth guard
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        // Bottom nav
        navHome    = findViewById(R.id.navHome);
        navPlanner = findViewById(R.id.navPlanner);
        navMusic   = findViewById(R.id.navMusic);
        navAbout   = findViewById(R.id.navAbout);
        ivHome     = findViewById(R.id.ivHome);
        ivPlanner  = findViewById(R.id.ivPlanner);
        ivMusic    = findViewById(R.id.ivMusic);
        ivAbout    = findViewById(R.id.ivAbout);
        tvHomeLabel    = findViewById(R.id.tvHomeLabel);
        tvPlannerLabel = findViewById(R.id.tvPlannerLabel);
        tvMusicLabel   = findViewById(R.id.tvMusicLabel);
        tvAboutLabel   = findViewById(R.id.tvAboutLabel);

        // Screen views
        llChallengesContainer = findViewById(R.id.llChallengesContainer);
        llEmptyState          = findViewById(R.id.llEmptyState);
        etPlaylistUrl         = findViewById(R.id.etPlaylistUrl);
        btnAddChallenge       = findViewById(R.id.btnAddChallenge);

        setupBottomNavigation();
        setActiveNavItem();        // Challenges tab stays orange always
        updateStreakDisplay();

        // Streak nav click → show global streak toast
        LinearLayout navStreak = findViewById(R.id.navStreak);
        if (navStreak != null) {
            navStreak.setOnClickListener(v -> {
                int s = getSharedPreferences("StreakPrefs", MODE_PRIVATE)
                            .getInt("currentStreak", 0);
                Toast.makeText(this,
                    s + " day streak! 🔥 Keep going!", Toast.LENGTH_SHORT).show();
            });
        }

        // Inline add-challenge button
        btnAddChallenge.setOnClickListener(v -> {
            String url = etPlaylistUrl.getText().toString().trim();
            if (url.isEmpty()) {
                etPlaylistUrl.setError("Paste a YouTube playlist URL first");
                etPlaylistUrl.requestFocus();
                return;
            }
            if (!isValidYouTubeUrl(url)) {
                etPlaylistUrl.setError("Enter a valid YouTube URL");
                return;
            }
            showAddChallengeSheet(url);
        });

        loadChallenges();
    }

    // Flag to skip the first onResume load (onCreate already loaded)
    private boolean firstResume = true;

    @Override
    protected void onResume() {
        super.onResume();
        updateStreakDisplay();
        if (firstResume) {
            firstResume = false;
        } else {
            // Reload when returning from another screen
            loadChallenges();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  LOAD & RENDER
    // ─────────────────────────────────────────────────────────────────

    private void loadChallenges() {
        db.collection("users").document(userId)
            .collection("challenges")
            .get()
            .addOnSuccessListener(snaps -> {
                loadedChallenges.clear();
                for (QueryDocumentSnapshot doc : snaps) {
                    ChallengeModel c = doc.toObject(ChallengeModel.class);
                    c.setId(doc.getId());
                    loadedChallenges.add(c);
                }
                // Sort newest first in Java (avoids any Firestore index requirement)
                loadedChallenges.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                renderChallenges(loadedChallenges);
            })
            .addOnFailureListener(e -> {
                String err = e.getMessage() != null ? e.getMessage() : "Unknown error";
                android.util.Log.e("ChallengesDB", "Load failed: " + err, e);
                if (err.contains("PERMISSION_DENIED")) {
                    Toast.makeText(this,
                        "Permission denied – update Firestore rules to allow challenges.",
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Load error: " + err, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void renderChallenges(List<ChallengeModel> challenges) {
        llChallengesContainer.removeAllViews();

        if (challenges.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            syncGlobalStreakFromChallenges(challenges);
            return;
        }
        llEmptyState.setVisibility(View.GONE);
        syncGlobalStreakFromChallenges(challenges);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < challenges.size(); i++) {
            ChallengeModel c = challenges.get(i);
            View card = inflater.inflate(R.layout.item_challenge, llChallengesContainer, false);
            bindChallengeCard(card, c, i);

            // Tap card → open detailed challenge view
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChallengeDetailsActivity.class);
                intent.putExtra("challengeId", c.getId());
                intent.putExtra("challengeTitle", c.getTitle());
                intent.putExtra("playlistUrl", c.getPlaylistUrl());
                intent.putExtra("totalVideos", c.getTotalVideos());
                intent.putExtra("videosPerDay", c.getVideosPerDay());
                intent.putExtra("totalCompleted", c.getTotalCompleted());
                startActivity(intent);
            });

            // Long-press → confirm delete
            final ChallengeModel fc = c;
            card.setOnLongClickListener(v -> { confirmDeleteChallenge(fc); return true; });

            llChallengesContainer.addView(card);
        }
    }

    private void bindChallengeCard(View card, ChallengeModel c, int colorIndex) {
        // ── Views ─────────────────────────────────────────────────
        FrameLayout iconBox       = card.findViewById(R.id.iconBox);
        TextView tvIcon           = card.findViewById(R.id.tvChallengeIcon);
        TextView tvTitle          = card.findViewById(R.id.tvChallengeTitle);
        TextView tvStats          = card.findViewById(R.id.tvChallengeStats);
        TextView tvStreakNum      = card.findViewById(R.id.tvStreakNumber);
        TextView tvProgressVideos = card.findViewById(R.id.tvProgressVideos);
        TextView tvProgressPct    = card.findViewById(R.id.tvProgressPercent);
        ProgressBar progressBar   = card.findViewById(R.id.progressChallenge);
        TextView tvStatusPill     = card.findViewById(R.id.tvStatusPill);

        // ── Icon box color ─────────────────────────────────────────
        int idx = colorIndex % ICON_BG_COLORS.length;
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.RECTANGLE);
        iconBg.setCornerRadius(dp(12));
        iconBg.setColor(ICON_BG_COLORS[idx]);
        iconBox.setBackground(iconBg);

        // Icon text: first 2 chars of title (or ▶ fallback)
        String title = c.getTitle() != null ? c.getTitle() : "";
        String iconText = title.length() >= 2
            ? title.substring(0, 2).toUpperCase()
            : (title.isEmpty() ? "▶" : title.toUpperCase());
        tvIcon.setText(iconText);
        tvIcon.setTextColor(ICON_TEXT_COLORS[idx]);
        tvIcon.setTextSize(14);
        tvIcon.setTypeface(null, Typeface.BOLD);

        // ── Title + stats ──────────────────────────────────────────
        tvTitle.setText(title.isEmpty() ? "Untitled Challenge" : title);
        tvStats.setText(c.getTotalVideos() + " videos · " + c.getVideosPerDay() + "/day");

        // ── Streak badge ───────────────────────────────────────────
        tvStreakNum.setText(String.valueOf(c.getCurrentStreak()));

        // ── Progress ───────────────────────────────────────────────
        int done  = c.getTotalCompleted();
        int total = c.getTotalVideos() > 0 ? c.getTotalVideos() : 1;
        int pct   = c.getProgressPercent();
        tvProgressVideos.setText(done + "/" + total + " videos");
        tvProgressPct.setText(pct + "%");
        progressBar.setProgress(pct);

        // ── Status pill ────────────────────────────────────────────
        int vpd = c.getVideosPerDay() > 0 ? c.getVideosPerDay() : 1;

        if (c.isCompletedToday()) {
            tvStatusPill.setBackgroundResource(R.drawable.bg_status_done_pill);
            tvStatusPill.setTextColor(Color.parseColor("#2E7D32"));
            tvStatusPill.setText("✅  Today: " + vpd + "/" + vpd + " videos watched");
            tvStatusPill.setClickable(false);
        } else {
            tvStatusPill.setBackgroundResource(R.drawable.bg_status_pending_pill);
            tvStatusPill.setTextColor(Color.parseColor("#E65100"));
            tvStatusPill.setText("🕐  Today: 0/" + vpd + " videos – Start now!");
            tvStatusPill.setClickable(true);
            tvStatusPill.setFocusable(true);
            tvStatusPill.setOnClickListener(v -> markTodayDone(c, card, colorIndex));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  MARK TODAY DONE
    // ─────────────────────────────────────────────────────────────────

    private void markTodayDone(ChallengeModel challenge, View card, int colorIndex) {
        String today     = ChallengeModel.getTodayString();
        String lastDate  = challenge.getLastCompletedDate();
        String yesterday = getYesterdayString();

        int newStreak = challenge.getCurrentStreak();
        if (yesterday.equals(lastDate)) {
            newStreak++;
        } else if (today.equals(lastDate)) {
            return; // already done
        } else {
            newStreak = 1;
        }

        int newLongest   = Math.max(newStreak, challenge.getLongestStreak());
        int newCompleted = Math.min(
            challenge.getTotalCompleted() + challenge.getVideosPerDay(),
            challenge.getTotalVideos()
        );

        final int finalStreak    = newStreak;
        final int finalLongest   = newLongest;
        final int finalCompleted = newCompleted;

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentStreak",     finalStreak);
        updates.put("longestStreak",     finalLongest);
        updates.put("lastCompletedDate", today);
        updates.put("totalCompleted",    finalCompleted);

        db.collection("users").document(userId)
            .collection("challenges").document(challenge.getId())
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                challenge.setCurrentStreak(finalStreak);
                challenge.setLongestStreak(finalLongest);
                challenge.setLastCompletedDate(today);
                challenge.setTotalCompleted(finalCompleted);
                bindChallengeCard(card, challenge, colorIndex);

                String msg = finalStreak == 1
                    ? "Great start! 🎯 Day 1 streak!"
                    : "🔥 " + finalStreak + " day streak! Keep it up!";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                syncGlobalStreakFromChallenges(loadedChallenges);
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Update failed. Try again.", Toast.LENGTH_SHORT).show());
    }



    // ─────────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────────

    private void confirmDeleteChallenge(ChallengeModel challenge) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Delete Challenge")
            .setMessage("Delete \"" + challenge.getTitle() + "\"?")
            .setPositiveButton("Delete", (d, w) -> deleteChallenge(challenge))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteChallenge(ChallengeModel challenge) {
        db.collection("users").document(userId)
            .collection("challenges").document(challenge.getId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Challenge deleted.", Toast.LENGTH_SHORT).show();
                loadChallenges();
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed to delete.", Toast.LENGTH_SHORT).show());
    }

    // ─────────────────────────────────────────────────────────────────
    //  ADD CHALLENGE BOTTOM SHEET  (URL pre-filled from inline card)
    // ─────────────────────────────────────────────────────────────────

    private void showAddChallengeSheet(String prefilledUrl) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        dialog.getWindow().setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        View sheetView = buildSheetView(dialog, prefilledUrl);
        dialog.setContentView(sheetView);

        dialog.setOnShowListener(d -> {
            com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                    (View) sheetView.getParent());
            behavior.setState(
                com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);

            View header = sheetView.findViewWithTag("sheet_header");
            if (header != null) {
                header.setTranslationY(-dp(60));
                header.setAlpha(0f);
                header.animate()
                    .translationY(0f).alpha(1f).setDuration(260)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            }
        });

        dialog.show();
    }

    private View buildSheetView(BottomSheetDialog dialog, String prefilledUrl) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bottom_sheet_bg);

        // ── Pinned header ──────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setTag("sheet_header");
        int p = dp(20);
        header.setPadding(p, p, p, dp(16));
        header.setBackgroundColor(Color.WHITE);

        View handle = new View(this);
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColor(Color.parseColor("#DDDDDD"));
        hBg.setCornerRadius(dp(4));
        handle.setBackground(hBg);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(40), dp(4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.setMargins(0, 0, 0, dp(16));
        handle.setLayoutParams(hlp);
        header.addView(handle);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("New Challenge");
        tvTitle.setTextColor(Color.parseColor("#111111"));
        tvTitle.setTextSize(22);
        tvTitle.setTypeface(null, Typeface.BOLD);
        header.addView(tvTitle);

        // URL preview line
        TextView tvUrlPreview = new TextView(this);
        tvUrlPreview.setText("🔗  " + shortenUrl(prefilledUrl));
        tvUrlPreview.setTextColor(Color.parseColor("#888888"));
        tvUrlPreview.setTextSize(12);
        tvUrlPreview.setMaxLines(1);
        tvUrlPreview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams uvLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        uvLp.setMargins(0, dp(4), 0, 0);
        tvUrlPreview.setLayoutParams(uvLp);
        header.addView(tvUrlPreview);
        root.addView(header);

        // Thin divider
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#F0F0F0"));
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        root.addView(divider);

        // ── Scrollable fields ──────────────────────────────────────
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setFillViewport(true);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sv.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(20), dp(18), dp(20), dp(8));

        EditText etName       = makeEditText("Challenge name  (e.g. Learn DSA)", false);
        EditText etTotalVids  = makeEditText("Total videos in playlist", true);
        EditText etVpd        = makeEditText("Videos to watch per day", true);

        fields.addView(etName);
        fields.addView(etTotalVids);
        fields.addView(etVpd);
        sv.addView(fields);
        root.addView(sv);

        // ── Pinned create button ───────────────────────────────────
        LinearLayout btnWrapper = new LinearLayout(this);
        btnWrapper.setOrientation(LinearLayout.VERTICAL);
        btnWrapper.setBackgroundColor(Color.WHITE);
        btnWrapper.setPadding(dp(20), dp(12), dp(20), dp(28));

        TextView btnCreate = new TextView(this);
        btnCreate.setText("Create Challenge");
        btnCreate.setTextColor(Color.WHITE);
        btnCreate.setTextSize(15);
        btnCreate.setTypeface(null, Typeface.BOLD);
        btnCreate.setGravity(Gravity.CENTER);
        btnCreate.setBackgroundResource(R.drawable.bg_add_btn_black);
        btnCreate.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        btnCreate.setClickable(true);
        btnCreate.setFocusable(true);
        btnWrapper.addView(btnCreate);
        root.addView(btnWrapper);

        // ── Save logic ─────────────────────────────────────────────
        btnCreate.setOnClickListener(v -> {
            String name     = etName.getText().toString().trim();
            String totalStr = etTotalVids.getText().toString().trim();
            String vpdStr   = etVpd.getText().toString().trim();

            if (name.isEmpty())     { etName.setError("Required");      return; }
            if (totalStr.isEmpty()) { etTotalVids.setError("Required"); return; }
            if (vpdStr.isEmpty())   { etVpd.setError("Required");       return; }

            int total = Integer.parseInt(totalStr);
            int vpd   = Integer.parseInt(vpdStr);
            if (total <= 0) { etTotalVids.setError("Must be > 0"); return; }
            if (vpd <= 0)   { etVpd.setError("Must be > 0");       return; }

            saveChallenge(new ChallengeModel(name, prefilledUrl, total, vpd));
            etPlaylistUrl.setText("");   // clear inline field after save
            dialog.dismiss();
        });

        return root;
    }

    private EditText makeEditText(String hint, boolean isNumber) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(Color.parseColor("#AAAAAA"));
        et.setTextColor(Color.parseColor("#111111"));
        et.setTextSize(14);
        et.setBackgroundResource(R.drawable.bg_input_box_refined);
        if (isNumber) {
            et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, 0, 0, dp(12));
        et.setLayoutParams(lp);
        et.setPadding(dp(14), dp(4), dp(14), dp(4));
        return et;
    }

    // ─────────────────────────────────────────────────────────────────
    //  SAVE TO FIRESTORE
    // ─────────────────────────────────────────────────────────────────

    private void saveChallenge(ChallengeModel c) {
        Map<String, Object> data = new HashMap<>();
        data.put("title",             c.getTitle());
        data.put("playlistUrl",       c.getPlaylistUrl());
        data.put("totalVideos",       c.getTotalVideos());
        data.put("videosPerDay",      c.getVideosPerDay());
        data.put("currentStreak",     0);
        data.put("longestStreak",     0);
        data.put("lastCompletedDate", "");
        data.put("totalCompleted",    0);
        data.put("createdAt",         System.currentTimeMillis());

        db.collection("users").document(userId)
            .collection("challenges")
            .add(data)
            .addOnSuccessListener(ref -> {
                Toast.makeText(this, "Challenge created! 🏆", Toast.LENGTH_SHORT).show();
                loadChallenges();
            })
            .addOnFailureListener(e -> {
                String err = e.getMessage() != null ? e.getMessage() : "Unknown error";
                android.util.Log.e("ChallengesDB", "Save failed: " + err, e);
                if (err.contains("PERMISSION_DENIED")) {
                    Toast.makeText(this,
                        "Permission denied – add challenges rule in Firestore console.",
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Save failed: " + err, Toast.LENGTH_LONG).show();
                }
            });
    }

    // ─────────────────────────────────────────────────────────────────
    //  BOTTOM NAV
    // ─────────────────────────────────────────────────────────────────

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }, 100);
        });

        navPlanner.setOnClickListener(v -> {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(this, AddPlannerActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }, 100);
        });

        navMusic.setOnClickListener(v -> { /* already here */ });

        navAbout.setOnClickListener(v -> {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }, 100);
        });
    }

    /** Challenges tab is always orange; all others grey. */
    private void setActiveNavItem() {
        // Reset all to grey
        ivHome.setColorFilter(Color.parseColor("#888888"));
        tvHomeLabel.setTextColor(Color.parseColor("#888888"));
        ivPlanner.setColorFilter(Color.parseColor("#888888"));
        tvPlannerLabel.setTextColor(Color.parseColor("#888888"));
        ivAbout.setColorFilter(Color.parseColor("#888888"));
        tvAboutLabel.setTextColor(Color.parseColor("#888888"));

        // Challenges = active orange (already set in XML but also enforce here)
        ivMusic.setColorFilter(Color.parseColor("#FF6B35"));
        tvMusicLabel.setTextColor(Color.parseColor("#FF6B35"));
        tvMusicLabel.setTypeface(null, Typeface.BOLD);
    }

    // ─────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────

    private void updateStreakDisplay() {
        int streak = getSharedPreferences("StreakPrefs", MODE_PRIVATE)
                         .getInt("currentStreak", 0);

        // Nav badge
        TextView tvNavBadge = findViewById(R.id.tvStreakNavBadge);
        if (tvNavBadge != null) tvNavBadge.setText(String.valueOf(streak));

        // Nav label
        TextView tvStreakCount = findViewById(R.id.tvStreakCount);
        if (tvStreakCount != null) tvStreakCount.setText(streak + " day");

        // Header pill
        TextView tvHeaderStreak = findViewById(R.id.tvHeaderStreakCount);
        if (tvHeaderStreak != null) tvHeaderStreak.setText(String.valueOf(streak));
    }

    private void syncGlobalStreakFromChallenges(List<ChallengeModel> challenges) {
        int maxStreak = 0;
        for (ChallengeModel c : challenges) {
            maxStreak = Math.max(maxStreak, c.getCurrentStreak());
        }

        android.content.SharedPreferences prefs =
            getSharedPreferences("StreakPrefs", MODE_PRIVATE);
        prefs.edit()
             .putInt("currentStreak", maxStreak)
             .putLong("lastActiveTime", System.currentTimeMillis())
             .apply();
        updateStreakDisplay();
    }

    private String getYesterdayString() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DATE, -1);
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                   .format(cal.getTime());
    }

    private boolean isValidYouTubeUrl(String url) {
        return url.contains("youtube.com") || url.contains("youtu.be");
    }

    private String shortenUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        try {
            java.net.URL u = new java.net.URL(url);
            return u.getHost() + u.getPath();
        } catch (Exception e) {
            return url.length() > 50 ? url.substring(0, 50) + "…" : url;
        }
    }

    private int dp(int dpVal) {
        return Math.round(dpVal * getResources().getDisplayMetrics().density);
    }
}
