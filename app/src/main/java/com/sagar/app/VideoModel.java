package com.sagar.app;

public class VideoModel {
    private String id;
    private String title;
    private String duration;
    private String thumbnailUrl;
    private boolean completed;
    private int position; // 1-based position in playlist

    // Required empty constructor for Firestore
    public VideoModel() {}

    public VideoModel(String id, String title, String duration, String thumbnailUrl, int position) {
        this.id = id;
        this.title = title;
        this.duration = duration;
        this.thumbnailUrl = thumbnailUrl;
        this.position = position;
        this.completed = false;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}