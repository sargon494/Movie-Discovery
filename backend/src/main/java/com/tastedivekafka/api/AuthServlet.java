package com.tastedivekafka.api;

import java.io.IOException;

import com.tastedivekafka.db.UserDAO;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
        String path = req.getPathInfo();
        String body = req.getReader().readLine();

        if (body == null || !body.contains(":")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "El cuerpo debe tener el formato 'username:password'");
            return;
        }

        int sep = body.indexOf(':');
        String username = body.substring(0, sep).trim();
        String password = body.substring(sep + 1).trim();

        if (username.isBlank() || password.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username y password no pueden estar vacíos");
            return;
        }

        resp.setContentType("text/plain;charset=UTF-8");

        if ("/login".equals(path)) {

            handleLogin(username, password, resp);

        } else if ("/register".equals(path)) {
            handleRegister(username, password, resp);

        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Endpoint no encontrado");
        }
    }

    private void handleLogin(String username, String password, HttpServletResponse resp) throws IOException {
        try {
            boolean ok = userDAO.login(username, password);
            if(ok) {
                resp.getWriter().write("LOGIN_SUCCESSFUL");
            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("INVALID_CREDENTIALS");
            }
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de servidor: " + e.getMessage());
        }
    }

    private void handleRegister(String username, String password, HttpServletResponse resp) throws IOException {
        try {
            boolean ok = userDAO.register(username, password);
            if(ok) {
                resp.getWriter().write("REGISTER_SUCCESSFUL");
            } else {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("USER_EXISTS");
            }
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de servidor: " + e.getMessage());
        }
    }
    
}
