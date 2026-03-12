package com.sagar.app;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChallengeDetailsActivity extends AppCompatActivity {

    private ChallengeModel challenge;
    private List<VideoModel> allVideos = new ArrayList<>();
    private List<VideoModel> completedVideos = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId;

    // Views
    private TextView tvChallengeTitle;
    private TextView tvProgressText;
    private TextView tvProgressPercent;
    private ProgressBar progressBar;
    private LinearLayout todayVideosContainer;
    private LinearLayout completedVideosContainer;
    private LinearLayout helpfulLinksContainer;
    
    // Tab views
    private TextView tabVideos, tabLinks, tabNotes, tabSaved;
    private LinearLayout videosContent, linksContent, notesContent, savedContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_details);

        // Get challenge data from intent
        String challengeId = getIntent().getStringExtra("challengeId");
        String challengeTitle = getIntent().getStringExtra("challengeTitle");
        String playlistUrl = getIntent().getStringExtra("playlistUrl");
        int totalVideos = getIntent().getIntExtra("totalVideos", 0);
        int videosPerDay = getIntent().getIntExtra("videosPerDay", 1);
        int totalCompleted = getIntent().getIntExtra("totalCompleted", 0);

        challenge = new ChallengeModel(challengeTitle, playlistUrl, totalVideos, videosPerDay);
        challenge.setId(challengeId);
        challenge.setTotalCompleted(totalCompleted);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupTabs();
        loadPlaylistData();
        setupHelpfulLinks();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvChallengeTitle = findViewById(R.id.tvChallengeTitle);
        tvProgressText = findViewById(R.id.tvProgressText);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        progressBar = findViewById(R.id.progressBar);
        todayVideosContainer = findViewById(R.id.todayVideosContainer);
        completedVideosContainer = findViewById(R.id.completedVideosContainer);
        helpfulLinksContainer = findViewById(R.id.helpfulLinksContainer);

        // Tab views
        tabVideos = findViewById(R.id.tabVideos);
        tabLinks = findViewById(R.id.tabLinks);
        tabNotes = findViewById(R.id.tabNotes);
        tabSaved = findViewById(R.id.tabSaved);

        videosContent = findViewById(R.id.videosContent);
        linksContent = findViewById(R.id.linksContent);
        notesContent = findViewById(R.id.notesContent);
        savedContent = findViewById(R.id.savedContent);

        // Set initial data
        tvChallengeTitle.setText(challenge.getTitle());
        updateProgressDisplay();
    }

    private void setupTabs() {
        tabVideos.setOnClickListener(v -> switchToTab(0));
        tabLinks.setOnClickListener(v -> switchToTab(1));
        tabNotes.setOnClickListener(v -> switchToTab(2));
        tabSaved.setOnClickListener(v -> switchToTab(3));

        // Start with Videos tab active
        switchToTab(0);
    }

    private void switchToTab(int tabIndex) {
        // Reset all tabs
        resetTab(tabVideos);
        resetTab(tabLinks);
        resetTab(tabNotes);
        resetTab(tabSaved);

        // Hide all content
        videosContent.setVisibility(View.GONE);
        linksContent.setVisibility(View.GONE);
        notesContent.setVisibility(View.GONE);
        savedContent.setVisibility(View.GONE);

        // Activate selected tab
        switch (tabIndex) {
            case 0:
                activateTab(tabVideos);
                videosContent.setVisibility(View.VISIBLE);
                break;
            case 1:
                activateTab(tabLinks);
                linksContent.setVisibility(View.VISIBLE);
                break;
            case 2:
                activateTab(tabNotes);
                notesContent.setVisibility(View.VISIBLE);
                break;
            case 3:
                activateTab(tabSaved);
                savedContent.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void resetTab(TextView tab) {
        tab.setBackgroundColor(Color.TRANSPARENT);
        tab.setTextColor(Color.parseColor("#888888"));
    }

    private void activateTab(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_add_btn_black);
        tab.setTextColor(Color.WHITE);
    }

    private void loadPlaylistData() {
        YouTubeApiHelper.extractPlaylistVideos(challenge.getPlaylistUrl(), 
            new YouTubeApiHelper.OnPlaylistLoadedListener() {
                @Override
                public void onSuccess(List<VideoModel> videos) {
                    allVideos.clear();
                    allVideos.addAll(videos);
                    
                    // Load completion status from Firestore
                    loadVideoCompletionStatus();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(ChallengeDetailsActivity.this, 
                        "Failed to load videos: " + error, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void loadVideoCompletionStatus() {
        db.collection("users").document(userId)
            .collection("challenges").document(challenge.getId())
            .collection("videos")
            .get()
            .addOnSuccessListener(snaps -> {
                completedVideos.clear();
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snaps) {
                    String videoId = doc.getId();
                    boolean completed = doc.getBoolean("completed") == Boolean.TRUE;
                    
                    // Find matching video and update status
                    for (VideoModel video : allVideos) {
                        if (video.getId().equals(videoId)) {
                            video.setCompleted(completed);
                            if (completed) {
                                completedVideos.add(video);
                            }
                            break;
                        }
                    }
                }
                renderTodayVideos();
                renderCompletedVideos();
            });
    }

    private void renderTodayVideos() {
        todayVideosContainer.removeAllViews();
        
        int startIndex = completedVideos.size();
        int videosPerDay = Math.max(1, challenge.getVideosPerDay());
        int endIndex = Math.min(allVideos.size(), startIndex + videosPerDay);

        TextView tvTodayCount = findViewById(R.id.tvTodayCount);
        int todayCount = Math.max(0, endIndex - startIndex);
        tvTodayCount.setText(todayCount + " videos");

        if (startIndex >= allVideos.size()) {
            // All videos completed
            TextView completedMsg = new TextView(this);
            completedMsg.setText("✅ All videos completed! Great work!");
            completedMsg.setTextColor(Color.parseColor("#2E7D32"));
            completedMsg.setTextSize(16);
            completedMsg.setPadding(16, 16, 16, 16);
            todayVideosContainer.addView(completedMsg);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        
        for (int i = startIndex; i < endIndex && i < allVideos.size(); i++) {
            VideoModel video = allVideos.get(i);
            View videoCard = inflater.inflate(R.layout.item_video, todayVideosContainer, false);
            bindVideoCard(videoCard, video, i == endIndex - 1); // Highlight last video
            todayVideosContainer.addView(videoCard);
        }
    }

    private void bindVideoCard(View card, VideoModel video, boolean isNext) {
        TextView tvTitle = card.findViewById(R.id.tvVideoTitle);
        TextView tvDuration = card.findViewById(R.id.tvVideoDuration);
        TextView tvStatus = card.findViewById(R.id.tvVideoStatus);

        tvTitle.setText(video.getTitle());
        tvDuration.setText(video.getDuration() + " mins");

        if (video.isCompleted()) {
            tvStatus.setText("Done");
            tvStatus.setBackgroundResource(R.drawable.bg_status_done_pill);
            tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            card.setAlpha(0.7f);
        } else if (isNext) {
            tvStatus.setText("Watch");
            tvStatus.setBackgroundResource(R.drawable.bg_status_pending_pill);
            tvStatus.setTextColor(Color.parseColor("#1565C0"));
            
            // Add blue border for next video
            card.setBackgroundResource(R.drawable.bg_status_pending_pill);
            card.setAlpha(1.0f);
        } else {
            tvStatus.setText("Pending");
            tvStatus.setBackgroundColor(Color.parseColor("#F5F5F5"));
            tvStatus.setTextColor(Color.parseColor("#888888"));
            card.setAlpha(0.5f);
        }

        // Click to open video
        card.setOnClickListener(v -> {
            if (!video.isCompleted()) {
                openVideo(video);
            }
        });
    }

    private void renderCompletedVideos() {
        completedVideosContainer.removeAllViews();
        
        TextView tvCompletedCount = findViewById(R.id.tvCompletedCount);
        if (tvCompletedCount != null) {
            tvCompletedCount.setText(completedVideos.size() + " videos");
        }
        
        if (completedVideos.isEmpty()) {
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("No videos completed yet");
            emptyMsg.setTextColor(Color.parseColor("#888888"));
            emptyMsg.setTextSize(14);
            emptyMsg.setPadding(16, 16, 16, 16);
            completedVideosContainer.addView(emptyMsg);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        
        for (VideoModel video : completedVideos) {
            View videoCard = inflater.inflate(R.layout.item_video, completedVideosContainer, false);
            bindCompletedVideoCard(videoCard, video);
            completedVideosContainer.addView(videoCard);
        }
    }

    private void bindCompletedVideoCard(View card, VideoModel video) {
        TextView tvTitle = card.findViewById(R.id.tvVideoTitle);
        TextView tvDuration = card.findViewById(R.id.tvVideoDuration);
        TextView tvStatus = card.findViewById(R.id.tvVideoStatus);

        tvTitle.setText(video.getTitle());
        tvDuration.setText(video.getDuration() + " mins");

        tvStatus.setText("Rewatch");
        tvStatus.setBackgroundResource(R.drawable.bg_status_pending_pill);
        tvStatus.setTextColor(Color.parseColor("#1565C0"));
        
        card.setAlpha(0.9f);

        // Click to rewatch video
        card.setOnClickListener(v -> openVideo(video));
    }

    private void openVideo(VideoModel video) {
        String url = "https://www.youtube.com/watch?v=" + video.getId() + 
                    "&list=" + extractListId(challenge.getPlaylistUrl()) +
                    "&index=" + video.getPosition();
        
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            
            // Mark as completed after opening
            markVideoCompleted(video);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open video", Toast.LENGTH_SHORT).show();
        }
    }

    private void markVideoCompleted(VideoModel video) {
        video.setCompleted(true);
        completedVideos.add(video);

        // Save to Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("completed", true);
        data.put("completedAt", System.currentTimeMillis());

        db.collection("users").document(userId)
            .collection("challenges").document(challenge.getId())
            .collection("videos").document(video.getId())
            .set(data);

        // Update challenge progress
        updateChallengeProgress();
        renderTodayVideos();
        renderCompletedVideos();
    }

    private void updateChallengeProgress() {
        int newCompleted = completedVideos.size();
        challenge.setTotalCompleted(newCompleted);
        
        // Update main challenge document
        Map<String, Object> updates = new HashMap<>();
        updates.put("totalCompleted", newCompleted);

        db.collection("users").document(userId)
            .collection("challenges").document(challenge.getId())
            .update(updates);

        updateProgressDisplay();
    }

    private void updateProgressDisplay() {
        int completed = challenge.getTotalCompleted();
        int total = challenge.getTotalVideos();
        int percent = total > 0 ? (completed * 100 / total) : 0;

        tvProgressText.setText(completed + "/" + total + " videos completed");
        tvProgressPercent.setText(percent + "%");
        progressBar.setProgress(percent);
    }

    private void setupHelpfulLinks() {
        helpfulLinksContainer.removeAllViews();
        
        // Add common helpful links
        addHelpfulLink("📺 Open Full Playlist", challenge.getPlaylistUrl());
        addHelpfulLink("📋 Copy Playlist Link", challenge.getPlaylistUrl(), true);
        
        // Add topic-specific links based on title
        String title = challenge.getTitle().toLowerCase();
        if (title.contains("python")) {
            addHelpfulLink("📖 Official Python Docs", "https://docs.python.org/");
            addHelpfulLink("📄 Python Cheat Sheet", "https://github.com/gto76/python-cheatsheet");
        } else if (title.contains("java")) {
            addHelpfulLink("📖 Oracle Java Docs", "https://docs.oracle.com/en/java/");
            addHelpfulLink("📄 Java Cheat Sheet", "https://introcs.cs.princeton.edu/java/11cheatsheet/");
        } else if (title.contains("javascript")) {
            addHelpfulLink("📖 MDN Web Docs", "https://developer.mozilla.org/en-US/docs/Web/JavaScript");
            addHelpfulLink("📄 JavaScript Cheat Sheet", "https://htmlcheatsheet.com/js/");
        }
    }

    private void addHelpfulLink(String title, String url) {
        addHelpfulLink(title, url, false);
    }

    private void addHelpfulLink(String title, String url, boolean isCopyAction) {
        TextView link = new TextView(this);
        link.setText(title);
        link.setTextColor(Color.parseColor("#1565C0"));
        link.setTextSize(14);
        link.setPadding(16, 12, 16, 12);
        link.setBackground(getResources().getDrawable(R.drawable.bg_challenge_card));
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 12);
        link.setLayoutParams(lp);

        if (isCopyAction) {
            link.setOnClickListener(v -> {
                android.content.ClipboardManager cm = 
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("url", url));
                Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
            });
        } else {
            link.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
                }
            });
        }

        helpfulLinksContainer.addView(link);
    }

    private String extractListId(String url) {
        try {
            Uri uri = Uri.parse(url);
            return uri.getQueryParameter("list");
        } catch (Exception e) {
            return "";
        }
    }
}