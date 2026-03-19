package com.tastedivekafka.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.tastedivekafka.db.DBConnection;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * GET /verify?token=xxx
 *
 * Verifica el token, marca la cuenta como verificada y devuelve
 * una página HTML simple con el resultado.
 */
public class VerificationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getParameter("token");

        if (token == null || token.isBlank()) {
            sendPage(resp, false, "Token no proporcionado.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {

            // Buscar token válido, no usado y no expirado
            String sql = """
                SELECT vt.id, vt.user_id, vt.used, vt.expires_at
                FROM verification_tokens vt
                WHERE vt.token = ?
                """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, token);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    sendPage(resp, false, "El enlace no es válido.");
                    return;
                }

                if (rs.getBoolean("used")) {
                    sendPage(resp, false, "Este enlace ya fue utilizado.");
                    return;
                }

                if (rs.getTimestamp("expires_at").toInstant()
                        .isBefore(java.time.Instant.now())) {
                    sendPage(resp, false, "El enlace ha expirado. Regístrate de nuevo.");
                    return;
                }

                int tokenId = rs.getInt("id");
                int userId  = rs.getInt("user_id");

                // Marcar usuario como verificado
                try (PreparedStatement upUser = conn.prepareStatement(
                        "UPDATE users SET email_verified = TRUE WHERE id = ?")) {
                    upUser.setInt(1, userId);
                    upUser.executeUpdate();
                }

                // Marcar token como usado
                try (PreparedStatement upToken = conn.prepareStatement(
                        "UPDATE verification_tokens SET used = TRUE WHERE id = ?")) {
                    upToken.setInt(1, tokenId);
                    upToken.executeUpdate();
                }

                sendPage(resp, true, "Cuenta verificada correctamente. Ya puedes iniciar sesión.");
            }

        } catch (SQLException e) {
            sendPage(resp, false, "Error interno: " + e.getMessage());
        }
    }

    private void sendPage(HttpServletResponse resp, boolean success, String message)
            throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        String color  = success ? "#639bff" : "#dc3c3c";
        String title  = success ? "Cuenta verificada" : "Error de verificación";
        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>MovieDiscovery — %s</title></head>
            <body style="font-family:sans-serif;background:#12121a;color:#e0e0e8;
                         display:flex;justify-content:center;align-items:center;
                         height:100vh;margin:0;">
              <div style="text-align:center;max-width:400px;padding:32px;
                          background:#1a1a22;border-radius:12px;
                          border:1px solid #2d2d3c;">
                <h2 style="color:%s;margin-top:0">%s</h2>
                <p>%s</p>
                <p style="margin-top:24px;color:#78788a;font-size:13px">
                  Puedes cerrar esta pestaña.
                </p>
              </div>
            </body>
            </html>
            """.formatted(title, color, title, message);
        resp.getWriter().write(html);
    }
}