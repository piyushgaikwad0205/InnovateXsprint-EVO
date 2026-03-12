package com.sagar.app;

import java.util.List;

public class StudyResponse {
    public List<Day> days;
    public List<String> tips;

    public static class Day {
        public String date;
        public List<Session> sessions;
    }

    public static class Session {
        public String time;
        public String title;
        public int durationMinutes;
        public String focus;
        public String notes;
    }
}
