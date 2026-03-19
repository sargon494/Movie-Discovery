package com.tastedivekafka.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

/**
 * DAO para gestión de usuarios.
 *
 * Cambios respecto a la versión anterior:
 * - register() acepta email además de username y password
 * - register() genera token de verificación y lo devuelve para enviarlo por email
 * - login() acepta email o username indistintamente
 * - login() bloquea si la cuenta no está verificada
 */
public class UserDAO {

    /**
     * Registra un nuevo usuario.
     *
     * @param username      nombre de usuario
     * @param email         correo electrónico
     * @param plainPassword contraseña en texto plano
     * @return token de verificación generado, o null si el usuario/email ya existe
     * @throws SQLException si hay error de base de datos
     */
    public String register(String username, String email, String plainPassword)
            throws SQLException {

        // Verificar que no exista el username ni el email
        String checkSql = "SELECT id FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setString(1, username);
            check.setString(2, email);
            if (check.executeQuery().next()) return null; // ya existe
        }

        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

        // Insertar usuario
        String insertUser = """
            INSERT INTO users (username, email, password_hash, email_verified)
            VALUES (?, ?, ?, FALSE)
            RETURNING id
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertUser)) {
            ps.setString(1, username);
            ps.setString(2, email.toLowerCase().trim());
            ps.setString(3, hash);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            int userId = rs.getInt("id");

            // Generar token de verificación (expira en 24h)
            String token = UUID.randomUUID().toString();
            Timestamp expires = Timestamp.from(Instant.now().plus(24, ChronoUnit.HOURS));

            String insertToken = """
                INSERT INTO verification_tokens (user_id, token, expires_at)
                VALUES (?, ?, ?)
                """;
            try (PreparedStatement pt = conn.prepareStatement(insertToken)) {
                pt.setInt(1, userId);
                pt.setString(2, token);
                pt.setTimestamp(3, expires);
                pt.executeUpdate();
            }

            return token;
        }
    }

    /**
     * Resultado del intento de login.
     */
    public enum LoginResult {
        SUCCESS,
        INVALID_CREDENTIALS,
        NOT_VERIFIED
    }

    /**
     * Intenta hacer login con email o username.
     *
     * @param identifier email o username
     * @param password   contraseña en texto plano
     * @return LoginResult con el resultado
     */
    public LoginResult login(String identifier, String password) {
        // Buscar por username o email
        String sql = """
            SELECT username, password_hash, email_verified
            FROM users
            WHERE username = ? OR email = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, identifier);
            ps.setString(2, identifier.toLowerCase().trim());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return LoginResult.INVALID_CREDENTIALS;

            String storedHash = rs.getString("password_hash");
            boolean verified  = rs.getBoolean("email_verified");

            if (!BCrypt.checkpw(password, storedHash)) return LoginResult.INVALID_CREDENTIALS;
            if (!verified) return LoginResult.NOT_VERIFIED;

            return LoginResult.SUCCESS;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error login: " + e.getMessage());
            return LoginResult.INVALID_CREDENTIALS;
        }
    }

    /**
     * Devuelve el username a partir del identifier (email o username).
     * Necesario para guardar en sesión tras login con email.
     */
    public String getUsernameByIdentifier(String identifier) {
        String sql = "SELECT username FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identifier);
            ps.setString(2, identifier.toLowerCase().trim());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("username") : null;
        } catch (SQLException e) {
            return null;
        }
    }
}