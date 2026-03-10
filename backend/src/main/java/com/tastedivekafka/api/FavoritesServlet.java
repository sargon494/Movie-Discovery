package com.tastedivekafka.api;

import com.tastedivekafka.db.DBConnection;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;

/**
 * Endpoints de favoritos.
 *
 * GET  /favorites              → lista de favoritos del usuario
 * POST /favorites              → añadir favorito   body: movieName||imageUrl||trailerUrl||rating
 * PUT  /favorites              → actualizar rating  body: movieName||rating
 * DEL  /favorites              → quitar favorito    body: movieName
 *
 * Header requerido: X-Username
 */
public class FavoritesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getHeader("X-Username");
        if (username == null || username.isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = """
                SELECT f.movie_name, f.image_url, f.trailer_url, f.rating, f.added_at
                FROM user_favorites f
                JOIN users u ON u.id = f.user_id
                WHERE u.username = ?
                ORDER BY f.added_at DESC
                """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(";;");
                    sb.append(rs.getString("movie_name")).append("||")
                      .append(rs.getString("image_url")).append("||")
                      .append(rs.getString("trailer_url")).append("||")
                      .append(rs.getInt("rating")).append("||")
                      .append(rs.getString("added_at"));
                }

                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write(sb.toString());
            }
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getHeader("X-Username");
        if (username == null || username.isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String body = req.getReader().readLine();
        if (body == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // formato: movieName||imageUrl||trailerUrl||rating
        String[] parts = body.split("\\|\\|");
        if (parts.length < 4) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Formato inválido");
            return;
        }

        String movieName  = parts[0].trim();
        String imageUrl   = parts[1].trim();
        String trailerUrl = parts[2].trim();
        int    rating     = Integer.parseInt(parts[3].trim());

        try (Connection conn = DBConnection.getConnection()) {
            // Obtener user_id
            int userId = getUserId(conn, username);
            if (userId == -1) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado");
                return;
            }

            // INSERT OR UPDATE si ya existe (upsert)
            String sql = """
                INSERT INTO user_favorites (user_id, movie_name, image_url, trailer_url, rating)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (user_id, movie_name)
                DO UPDATE SET rating = EXCLUDED.rating, added_at = NOW()
                """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, movieName);
                ps.setString(3, imageUrl);
                ps.setString(4, trailerUrl);
                ps.setInt(5, rating);
                ps.executeUpdate();
            }

            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("FAVORITE_ADDED");

        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getHeader("X-Username");
        if (username == null || username.isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String body = req.getReader().readLine();
        if (body == null || !body.contains("||")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // formato: movieName||rating
        String[] parts    = body.split("\\|\\|");
        String   movieName = parts[0].trim();
        int      rating    = Integer.parseInt(parts[1].trim());

        try (Connection conn = DBConnection.getConnection()) {
            int userId = getUserId(conn, username);
            String sql = """
                UPDATE user_favorites SET rating = ?
                WHERE user_id = ? AND movie_name = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, rating);
                ps.setInt(2, userId);
                ps.setString(3, movieName);
                ps.executeUpdate();
            }
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("RATING_UPDATED");
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getHeader("X-Username");
        if (username == null || username.isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String movieName = req.getReader().readLine();
        if (movieName == null || movieName.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            int userId = getUserId(conn, username);
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM user_favorites WHERE user_id = ? AND movie_name = ?")) {
                ps.setInt(1, userId);
                ps.setString(2, movieName.trim());
                ps.executeUpdate();
            }
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("FAVORITE_REMOVED");
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private int getUserId(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }
}