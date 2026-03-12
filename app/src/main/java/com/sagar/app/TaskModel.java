package com.sagar.app;

import java.util.HashMap;
import java.util.Map;

public class TaskModel {
    private String id;
    private String title;
    private String description;
    private String priority;
    private boolean completed;
    private long timestamp;

    private String type; // "TASK" or "TOUR"

    public TaskModel() {
    } // Required for Firestore

    public TaskModel(String title, String description, String priority, String type) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.type = type;
        this.completed = false;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("description", description);
        result.put("priority", priority);
        result.put("completed", completed);
        result.put("timestamp", timestamp);
        result.put("type", type);
        return result;
    }
}
