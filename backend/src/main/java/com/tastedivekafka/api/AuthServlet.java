package com.tastedivekafka.api;

import java.io.IOException;

import com.tastedivekafka.db.UserDAO;
import com.tastedivekafka.db.UserDAO.LoginResult;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Endpoints de autenticación.
 *
 * POST /auth/login    → body: "identifier:password"
 *                       identifier puede ser email o username
 *                       Respuestas: LOGIN_SUCCESSFUL:username | INVALID_CREDENTIALS | NOT_VERIFIED
 *
 * POST /auth/register → body: "username:email:password"
 *                       Respuestas: VERIFY_EMAIL | USER_EXISTS
 */
public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        String body = req.getReader().readLine();

        if (body == null || body.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cuerpo vacío");
            return;
        }

        resp.setContentType("text/plain;charset=UTF-8");

        if (null == path) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else switch (path) {
            case "/login" -> handleLogin(body.trim(), resp);
            case "/register" -> handleRegister(body.trim(), resp);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    private void handleLogin(String body, HttpServletResponse resp) throws IOException {
        // formato: identifier:password
        int sep = body.indexOf(':');
        if (sep < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Formato: identifier:password");
            return;
        }

        String identifier = body.substring(0, sep).trim();
        String password   = body.substring(sep + 1).trim();

        if (identifier.isBlank() || password.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Campos vacíos");
            return;
        }

        try {
            LoginResult result = userDAO.login(identifier, password);
            switch (result) {
                case SUCCESS -> {
                    // Devolvemos también el username para guardarlo en sesión
                    String username = userDAO.getUsernameByIdentifier(identifier);
                    resp.getWriter().write("LOGIN_SUCCESSFUL:" + username);
                }
                case NOT_VERIFIED -> {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.getWriter().write("NOT_VERIFIED");
                }
                case INVALID_CREDENTIALS -> {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.getWriter().write("INVALID_CREDENTIALS");
                }
            }
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // ─── Register ────────────────────────────────────────────────────────────

    private void handleRegister(String body, HttpServletResponse resp) throws IOException {
        // formato: username:email:password
        // El password puede contener ':', así que solo dividimos por los dos primeros ':'
        String[] parts = body.split(":", 3);
        if (parts.length < 3) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Formato: username:email:password");
            return;
        }

        String username = parts[0].trim();
        String email    = parts[1].trim();
        String password = parts[2].trim();

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Campos vacíos");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email inválido");
            return;
        }

        try {
            String token = userDAO.register(username, email, password);
            if (token == null) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("USER_EXISTS");
                return;
            }

            // Enviar email de verificación
            EmailService.sendVerificationEmail(email, username, token);

            resp.getWriter().write("VERIFY_EMAIL");

        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
