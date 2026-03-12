package com.sagar.app;

public class ChallengeModel {
    private String id;
    private String title;
    private String playlistUrl;
    private int totalVideos;
    private int videosPerDay;
    private int currentStreak;
    private int longestStreak;
    private String lastCompletedDate; // format: yyyy-MM-dd
    private int totalCompleted;
    private long createdAt;

    // Required empty constructor for Firestore
    public ChallengeModel() {}

    public ChallengeModel(String title, String playlistUrl, int totalVideos, int videosPerDay) {
        this.title = title;
        this.playlistUrl = playlistUrl;
        this.totalVideos = totalVideos;
        this.videosPerDay = videosPerDay;
        this.currentStreak = 0;
        this.longestStreak = 0;
        this.lastCompletedDate = "";
        this.totalCompleted = 0;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPlaylistUrl() { return playlistUrl; }
    public void setPlaylistUrl(String playlistUrl) { this.playlistUrl = playlistUrl; }

    public int getTotalVideos() { return totalVideos; }
    public void setTotalVideos(int totalVideos) { this.totalVideos = totalVideos; }

    public int getVideosPerDay() { return videosPerDay; }
    public void setVideosPerDay(int videosPerDay) { this.videosPerDay = videosPerDay; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public String getLastCompletedDate() { return lastCompletedDate; }
    public void setLastCompletedDate(String lastCompletedDate) { this.lastCompletedDate = lastCompletedDate; }

    public int getTotalCompleted() { return totalCompleted; }
    public void setTotalCompleted(int totalCompleted) { this.totalCompleted = totalCompleted; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /**
     * Returns total days required to complete the playlist
     */
    public int getTotalDays() {
        if (videosPerDay <= 0) return 0;
        return (int) Math.ceil((double) totalVideos / videosPerDay);
    }

    /**
     * Returns today's date string in yyyy-MM-dd format
     */
    public static String getTodayString() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }

    /**
     * Returns true if user has already marked today as done
     */
    public boolean isCompletedToday() {
        return getTodayString().equals(lastCompletedDate);
    }

    /**
     * Returns progress percentage (0-100)
     */
    public int getProgressPercent() {
        if (totalVideos <= 0) return 0;
        return Math.min(100, (int) ((totalCompleted * 100.0) / totalVideos));
    }
}
