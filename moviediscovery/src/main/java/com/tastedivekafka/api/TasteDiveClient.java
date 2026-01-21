package com.tastedivekafka.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class TasteDiveClient {

    public List<String> getRecommendations(String movie) throws IOException, InterruptedException {

        movie = movie.replaceAll("[^a-zA-Z0-9\\s]", "").trim();

        if (movie.isEmpty()) {
            return new ArrayList<>();
        }

        String encodedMovie = URLEncoder.encode(movie, StandardCharsets.UTF_8);

        String url = "https://tastedive.com/api/similar?q=" + encodedMovie +
                "&type=movie&limit=5&k=1066479-MovieDis-CD78C85B";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());

        JSONObject similar = null;

        // ✅ acepta ambos formatos
        if (json.has("Similar")) {
            similar = json.getJSONObject("Similar");
        } else if (json.has("similar")) {
            similar = json.getJSONObject("similar");
        } else {
            System.out.println("TasteDive NO devolvió 'Similar'. Response: " + response.body());
            return new ArrayList<>();
        }

        // ✅ acepta resultados en mayúscula o minúscula
        JSONArray results;
        if (similar.has("Results")) {
            results = similar.getJSONArray("Results");
        } else if (similar.has("results")) {
            results = similar.getJSONArray("results");
        } else {
            System.out.println("TasteDive NO devolvió 'Results'. Response: " + response.body());
            return new ArrayList<>();
        }

        List<String> recs = new ArrayList<>();

        for (int i = 0; i < results.length(); i++) {
            recs.add(results.getJSONObject(i).getString("name")); // O "Name"
        }

        return recs;
    }
}
