package com.sagar.app;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeApiHelper {
    private static final String TAG = "YouTubeApi";
    
    // You'll need to get this from Google Cloud Console
    private static final String API_KEY = "YOUR_YOUTUBE_API_KEY_HERE";
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/playlistItems";

    public interface OnPlaylistLoadedListener {
        void onSuccess(List<VideoModel> videos);
        void onError(String error);
    }

    public static void extractPlaylistVideos(String playlistUrl, OnPlaylistLoadedListener listener) {
        String playlistId = extractPlaylistId(playlistUrl);
        if (playlistId == null || playlistId.isEmpty()) {
            listener.onError("Invalid playlist URL");
            return;
        }

        // For now, create mock data since we don't have API key
        // In production, you'd use the real API
        createMockPlaylistData(playlistId, listener);
    }

    private static void createMockPlaylistData(String playlistId, OnPlaylistLoadedListener listener) {
        new AsyncTask<Void, Void, List<VideoModel>>() {
            @Override
            protected List<VideoModel> doInBackground(Void... voids) {
                List<VideoModel> videos = new ArrayList<>();
                
                // Create mock video data based on common programming topics
                String[] titles = {
                    "Introduction to Programming",
                    "Variables and Data Types", 
                    "Control Flow - If/Else",
                    "Loops - For and While",
                    "Functions and Methods",
                    "Data Structures - Lists",
                    "Object-Oriented Programming",
                    "Error Handling",
                    "File I/O Operations",
                    "Working with APIs",
                    "Database Connections",
                    "Testing and Debugging",
                    "Best Practices",
                    "Advanced Topics",
                    "Project Building"
                };
                
                String[] durations = {
                    "8:45", "12:34", "15:20", "18:45", "22:15",
                    "16:30", "25:10", "14:55", "19:20", "21:05",
                    "27:40", "13:25", "20:15", "32:50", "45:30"
                };

                for (int i = 0; i < Math.min(titles.length, 23); i++) {
                    VideoModel video = new VideoModel(
                        "video_" + (i + 1),
                        titles[i % titles.length],
                        durations[i % durations.length],
                        "https://img.youtube.com/vi/mock/maxresdefault.jpg",
                        i + 1
                    );
                    videos.add(video);
                }
                
                return videos;
            }

            @Override
            protected void onPostExecute(List<VideoModel> videos) {
                listener.onSuccess(videos);
            }
        }.execute();
    }

    private static String extractPlaylistId(String url) {
        try {
            Pattern pattern = Pattern.compile("[&?]list=([^&]+)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting playlist ID", e);
        }
        return null;
    }

    // Real API implementation (for when you have API key)
    private static void fetchRealPlaylistData(String playlistId, OnPlaylistLoadedListener listener) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String urlString = BASE_URL + 
                        "?part=snippet,contentDetails" +
                        "&maxResults=50" +
                        "&playlistId=" + URLEncoder.encode(playlistId, "UTF-8") +
                        "&key=" + API_KEY;
                    
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();
                    
                    return result.toString();
                } catch (Exception e) {
                    Log.e(TAG, "API request failed", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String response) {
                if (response == null) {
                    listener.onError("Failed to fetch playlist data");
                    return;
                }

                try {
                    JSONObject json = new JSONObject(response);
                    JSONArray items = json.getJSONArray("items");
                    List<VideoModel> videos = new ArrayList<>();

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        JSONObject snippet = item.getJSONObject("snippet");
                        
                        String videoId = snippet.getJSONObject("resourceId").getString("videoId");
                        String title = snippet.getString("title");
                        String thumbnail = snippet.getJSONObject("thumbnails")
                            .getJSONObject("medium").getString("url");
                        
                        VideoModel video = new VideoModel(
                            videoId, title, "0:00", thumbnail, i + 1);
                        videos.add(video);
                    }

                    listener.onSuccess(videos);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing response", e);
                    listener.onError("Failed to parse playlist data");
                }
            }
        }.execute();
    }
}