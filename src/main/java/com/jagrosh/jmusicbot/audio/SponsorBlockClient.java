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

    public List<Segment> fetchSegments(String videoId) {
        List<Segment> segments = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + videoId))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray arr = new JSONArray(response.body());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                JSONArray segs = obj.getJSONArray("segments");
                for (int j = 0; j < segs.length(); j++) {
                    JSONArray times = segs.getJSONArray(j);
                    double start = times.getDouble(0);
                    double end = times.getDouble(1);
                    segments.add(new Segment(start, end));
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log or handle error
        }
        return segments;
    }

    public static class Segment {
        public final double start;
        public final double end;
        public Segment(double start, double end) {
            this.start = start;
            this.end = end;
        }
    }
}
