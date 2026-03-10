package com.tastedivekafka.api;

import com.tastedivekafka.db.DBConnection;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.*;

/**
 * Endpoints de perfil de usuario.
 *
 * GET  /profile           → stats: username, created_at, total búsquedas, total favoritos
 * PUT  /profile/username  → cambiar nombre de usuario   body: newUsername
 * PUT  /profile/password  → cambiar contraseña          body: oldPassword:newPassword
 * DEL  /profile           → borrar cuenta
 *
 * Todos los endpoints identifican al usuario via header X-Username.
 */
public class ProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getHeader("X-Username");
        if (username == null || username.isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sin sesión");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Datos del usuario + stats en una sola query
            String sql = """
                SELECT u.username,
                       u.created_at,
                       (SELECT COUNT(*) FROM search_history  WHERE user_id = u.id) AS total_searches,
                       (SELECT COUNT(*) FROM user_favorites  WHERE user_id = u.id) AS total_favorites
                FROM users u
                WHERE u.username = ?
                """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado");
                    return;
                }

                // Formato: username||created_at||total_searches||total_favorites
                String result = rs.getString("username") + "||"
                    + rs.getString("created_at") + "||"
                    + rs.getLong("total_searches") + "||"
                    + rs.getLong("total_favorites");

                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write(result);
            }
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getHeader("X-Username");
        if (username == null || username.isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sin sesión");
            return;
        }

        String pathInfo = req.getPathInfo(); // /username o /password

        if ("/username".equals(pathInfo)) {
            changeUsername(req, resp, username);
        } else if ("/password".equals(pathInfo)) {
            changePassword(req, resp, username);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void changeUsername(HttpServletRequest req, HttpServletResponse resp,
                                 String currentUsername) throws IOException {
        String newUsername = req.getReader().readLine();
        if (newUsername == null || newUsername.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Nombre vacío");
            return;
        }
        newUsername = newUsername.trim();

        try (Connection conn = DBConnection.getConnection()) {
            // Verificar que no existe
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT id FROM users WHERE username = ?")) {
                check.setString(1, newUsername);
                if (check.executeQuery().next()) {
                    resp.sendError(HttpServletResponse.SC_CONFLICT, "USERNAME_EXISTS");
                    return;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET username = ? WHERE username = ?")) {
                ps.setString(1, newUsername);
                ps.setString(2, currentUsername);
                ps.executeUpdate();
            }

            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("USERNAME_UPDATED:" + newUsername);

        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void changePassword(HttpServletRequest req, HttpServletResponse resp,
                                 String username) throws IOException {
        String body = req.getReader().readLine();
        if (body == null || !body.contains(":")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Formato: oldPassword:newPassword");
            return;
        }

        int sep = body.indexOf(":");
        String oldPassword = body.substring(0, sep);
        String newPassword = body.substring(sep + 1);

        if (newPassword.isBlank() || newPassword.length() < 6) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "La contraseña debe tener al menos 6 caracteres");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Verificar contraseña actual
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT password_hash FROM users WHERE username = ?")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (!rs.next() || !BCrypt.checkpw(oldPassword, rs.getString("password_hash"))) {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Contraseña actual incorrecta");
                    return;
                }
            }

            // Actualizar con nuevo hash
            String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET password_hash = ? WHERE username = ?")) {
                ps.setString(1, newHash);
                ps.setString(2, username);
                ps.executeUpdate();
            }

            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("PASSWORD_UPDATED");

        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getHeader("X-Username");
        if (username == null || username.isBlank()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sin sesión");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // CASCADE borra automáticamente search_history y user_favorites
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM users WHERE username = ?")) {
                ps.setString(1, username);
                ps.executeUpdate();
            }
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("ACCOUNT_DELETED");
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}