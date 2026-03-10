package com.tastedivekafka.ui;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client wrapper for all backend communication.
 *
 * All frontend classes import THIS instead of any backend package.
 * If the backend URL ever changes, only this file needs updating.
 *
 * Base URL is read from the environment variable BACKEND_URL so it
 * works both locally (localhost:8090) and inside Docker (backend:8090).
 */
public class BackendClient {

    // Reads BACKEND_URL env var; falls back to localhost for local dev
    private static final String BASE_URL = System.getenv()
            .getOrDefault("BACKEND_URL", "https://movie-discovery-nf4s.onrender.com");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * POST /auth/login
     * @return true if credentials are valid
     * @throws Exception on network / server error
     */
    public static boolean login(String username, String password) throws Exception {
        String body = username + ":" + password;
        HttpResponse<String> response = post("/auth/login", body);
        return response.statusCode() == 200 && "LOGIN_SUCCESSFUL".equals(response.body());
    }

    /**
     * POST /auth/register
     * @return true if registration succeeded
     * @throws Exception if user already exists or network error
     */
    public static boolean register(String username, String password) throws Exception {
        String body = username + ":" + password;
        HttpResponse<String> response = post("/auth/register", body);
        if (response.statusCode() == 409) return false;   // USER_EXISTS
        if (response.statusCode() == 200) return true;
        throw new Exception("Unexpected status: " + response.statusCode());
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * POST /search
     * Sends a movie title and returns the raw recommendation string.
     * Format: "Title||imageURL||trailerURL;;Title2||imageURL2||trailerURL2"
     *
     * @param movieTitle the title typed by the user
     * @return raw response string (same format MainFrame already parses)
     * @throws Exception on timeout or network error
     */
    public static String search(String movieTitle) throws Exception {
        HttpResponse<String> response = post("/search", movieTitle);
        if (response.statusCode() == 200) return response.body();
        throw new Exception("Search failed with status: " + response.statusCode());
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private static HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }
}