package com.tastedivekafka.ui;

import com.tastedivekafka.session.AppSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client wrapper for all backend communication.
 *
 * Cambios respecto a la versión anterior:
 *  - Añadido header X-Username en todas las peticiones autenticadas
 *  - Métodos para perfil: getProfile, changeUsername, changePassword, deleteAccount
 *  - Métodos para favoritos: getFavorites, addFavorite, updateRating, removeFavorite
 *  - Métodos para historial: getHistory, recordSearch
 */
public class BackendClient {

    private static final String BASE_URL = System.getenv()
            .getOrDefault("BACKEND_URL", "https://movie-discovery-nf4s.onrender.com");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ── Auth ─────────────────────────────────────────────────────────────────

    public static boolean login(String username, String password) throws Exception {
        HttpResponse<String> response = post("/auth/login", username + ":" + password, false);
        return response.statusCode() == 200 && "LOGIN_SUCCESSFUL".equals(response.body());
    }

    public static boolean register(String username, String password) throws Exception {
        HttpResponse<String> response = post("/auth/register", username + ":" + password, false);
        if (response.statusCode() == 409) return false;
        if (response.statusCode() == 200) return true;
        throw new Exception("Unexpected status: " + response.statusCode());
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public static String search(String movieTitle) throws Exception {
        HttpResponse<String> response = post("/search", movieTitle, true);
        if (response.statusCode() == 200) return response.body();
        throw new Exception("Search failed with status: " + response.statusCode());
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    /**
     * @return "username||created_at||total_searches||total_favorites"
     */
    public static String getProfile() throws Exception {
        HttpResponse<String> response = get("/profile");
        if (response.statusCode() == 200) return response.body();
        throw new Exception("Profile error: " + response.statusCode());
    }

    /**
     * @return el nuevo username si tuvo éxito
     * @throws Exception si el username ya existe (409) u otro error
     */
    public static String changeUsername(String newUsername) throws Exception {
        HttpResponse<String> response = put("/profile/username", newUsername);
        if (response.statusCode() == 200) return response.body().replace("USERNAME_UPDATED:", "");
        if (response.statusCode() == 409) throw new Exception("El nombre de usuario ya existe");
        throw new Exception("Error: " + response.statusCode());
    }

    /**
     * @param oldPassword contraseña actual
     * @param newPassword nueva contraseña (mínimo 6 caracteres)
     */
    public static void changePassword(String oldPassword, String newPassword) throws Exception {
        HttpResponse<String> response = put("/profile/password", oldPassword + ":" + newPassword);
        if (response.statusCode() == 401) throw new Exception("Contraseña actual incorrecta");
        if (response.statusCode() == 400) throw new Exception("La contraseña debe tener al menos 6 caracteres");
        if (response.statusCode() != 200) throw new Exception("Error: " + response.statusCode());
    }

    public static void deleteAccount() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/profile"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .header("X-Username", AppSession.getCurrentUser())
                .DELETE()
                .build();
        HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ── Favorites ─────────────────────────────────────────────────────────────

    /**
     * @return "movieName||imageUrl||trailerUrl||rating||added_at;;..." o vacío
     */
    public static String getFavorites() throws Exception {
        HttpResponse<String> response = get("/favorites");
        if (response.statusCode() == 200) return response.body();
        throw new Exception("Favorites error: " + response.statusCode());
    }

    /**
     * @param movieName  título de la película
     * @param imageUrl   URL de la imagen
     * @param trailerUrl URL del trailer
     * @param rating     1-5 estrellas
     */
    public static void addFavorite(String movieName, String imageUrl,
                                    String trailerUrl, int rating) throws Exception {
        String body = movieName + "||" + imageUrl + "||" + trailerUrl + "||" + rating;
        HttpResponse<String> response = post("/favorites", body, true);
        if (response.statusCode() != 200) throw new Exception("Error añadiendo favorito");
    }

    public static void updateRating(String movieName, int rating) throws Exception {
        HttpResponse<String> response = put("/favorites", movieName + "||" + rating);
        if (response.statusCode() != 200) throw new Exception("Error actualizando rating");
    }

    public static void removeFavorite(String movieName) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/favorites"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .header("X-Username", AppSession.getCurrentUser())
                .method("DELETE", HttpRequest.BodyPublishers.ofString(movieName))
                .build();
        HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ── History ───────────────────────────────────────────────────────────────

    /**
     * @return "query||searched_at;;..." o vacío
     */
    public static String getHistory() throws Exception {
        HttpResponse<String> response = get("/history");
        if (response.statusCode() == 200) return response.body();
        throw new Exception("History error: " + response.statusCode());
    }

    public static void recordSearch(String query) {
        // Fire-and-forget — no bloquea el hilo de búsqueda
        new Thread(() -> {
            try {
                post("/history", query, true);
            } catch (Exception ignored) { }
        }, "history-recorder").start();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .header("X-Username", AppSession.getCurrentUser())
                .GET()
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> put(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .header("X-Username", AppSession.getCurrentUser())
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(String path, String body,
                                              boolean withAuth) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (withAuth && AppSession.getCurrentUser() != null) {
            builder.header("X-Username", AppSession.getCurrentUser());
        }

        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}