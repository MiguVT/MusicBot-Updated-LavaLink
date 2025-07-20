package com.jagrosh.jmusicbot.audio;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Fetches SponsorBlock segments for YouTube videos.
 */
public class SponsorBlockClient {
    private static final String API_URL = "https://sponsor.ajay.app/api/skipSegments?videoID=";
    private final HttpClient client;

    public SponsorBlockClient() {
        this.client = HttpClient.newHttpClient();
    }

    public java.util.concurrent.CompletableFuture<List<Segment>> fetchSegmentsAsync(String videoId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + videoId))
            .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                List<Segment> segments = new ArrayList<>();
                String body = response.body();
                // Only skip non-music categories for music bots
                String[] skipCategories = {"music_offtopic", "music_nonmusic", "non-music"};
                try {
                    if (body != null && body.trim().startsWith("[")) {
                        JSONArray arr = new JSONArray(body);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            String category = obj.optString("category", "unknown");
                            boolean shouldSkip = false;
                            for (String cat : skipCategories) {
                                if (category.equalsIgnoreCase(cat)) {
                                    shouldSkip = true;
                                    break;
                                }
                            }
                            if (!shouldSkip) continue;
                            JSONArray seg = obj.getJSONArray("segment");
                            double start = seg.getDouble(0);
                            double end = seg.getDouble(1);
                            segments.add(new Segment(start, end, category));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(); // Log or handle error
                }
                return segments;
            });
    }

    public static class Segment {
        public final double start;
        public final double end;
        public final String category;
        public Segment(double start, double end, String category) {
            this.start = start;
            this.end = end;
            this.category = category;
        }
    }
}
