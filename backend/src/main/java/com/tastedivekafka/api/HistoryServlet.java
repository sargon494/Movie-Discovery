package com.tastedivekafka.api;

import com.tastedivekafka.db.DBConnection;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;

/**
 * Endpoints de historial de búsquedas.
 *
 * GET  /history   → últimas 50 búsquedas del usuario
 * POST /history   → registra una búsqueda   body: query
 *
 * Header requerido: X-Username
 */
public class HistoryServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getHeader("X-Username");
        if (username == null || username.isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = """
                SELECT h.query, h.searched_at
                FROM search_history h
                JOIN users u ON u.id = h.user_id
                WHERE u.username = ?
                ORDER BY h.searched_at DESC
                LIMIT 50
                """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(";;");
                    sb.append(rs.getString("query")).append("||")
                      .append(rs.getString("searched_at"));
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

        String query = req.getReader().readLine();
        if (query == null || query.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Obtener user_id
            int userId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM users WHERE username = ?")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                userId = rs.getInt("id");
            }

            // Guardar búsqueda
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO search_history (user_id, query) VALUES (?, ?)")) {
                ps.setInt(1, userId);
                ps.setString(2, query.trim());
                ps.executeUpdate();
            }

            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("HISTORY_SAVED");

        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}