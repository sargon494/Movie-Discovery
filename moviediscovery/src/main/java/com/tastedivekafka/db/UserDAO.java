package com.tastedivekafka.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO (Data Access Object) para la gestión de usuarios.
 *
 * Se encarga del acceso a la base de datos relacionado con la autenticación de usuarios.
 */
public class UserDAO {

    /**
     * Verifica si un usuario puede iniciar sesión.
     *
     * Comprueba que exista un registro en la tabla "users"
     * con el nombre de usuario y contraseña indicados.
     *
     * @param username nombre de usuario introducido
     * @param password contraseña introducida
     * @return true si las credenciales son correctas, false en caso contrario
     */
    public boolean login(String username, String password) {

        // Log de depuración (útil durante desarrollo)
        System.out.println("Intentando login: " + username + " / " + password);

        // Consulta SQL: solo se comprueba la existencia del usuario
        String sql = "SELECT 1 FROM users WHERE username=? AND password=?";

        // try-with-resources para cerrar automáticamente la conexión y statement
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Confirmación de conexión correcta (debug)
            System.out.println("Conexión OK: " + con);

            // Asignar valores a los parámetros de la consulta
            ps.setString(1, username);
            ps.setString(2, password);

            // Ejecutar la consulta
            ResultSet rs = ps.executeQuery();

            // Si existe al menos un resultado, el login es válido
            boolean ok = rs.next();
            System.out.println("Resultado: " + ok);

            return ok;

        } catch (SQLException e) {
            // Manejo de errores de base de datos
            System.err.println("Error en login: " + e.getMessage());
            return false;
        }
    }
}