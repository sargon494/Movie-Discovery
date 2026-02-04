package com.tastedivekafka.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

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

    public boolean register(String username, String plainPassword){
        // Hashear la contraseña antes de guardarla
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashedPassword);

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Error en registro: " + e.getMessage());
            return false;
        }
    }

    public boolean login(String username, String password) {

        // Log de depuración 
        System.out.println("Intentando login: " + username + " / " + password);

        // Consulta SQL: solo se comprueba la existencia del usuario
        String sql = "SELECT password_hash FROM users WHERE username=?";

        // try-with-resources para cerrar automáticamente la conexión y statement
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Confirmación de conexión correcta (debug)
            System.out.println("Conexión OK: " + con);

            // Asignar valores a los parámetros de la consulta
            ps.setString(1, username);

            // Ejecutar la consulta
            ResultSet rs = ps.executeQuery();

            // Usuario no encontrado
            if (!rs.next()) {
                System.out.println("Usuario no encontrado: " + username);
                return false;
            } 

            // Obtener el hash de la contraseña almacenada
            String storedHash = rs.getString("password_hash");
            
            // Comparar la contraseña introducida con el hash almacenado
            if (BCrypt.checkpw(password, storedHash)) {
                System.out.println("Login exitoso para: " + username);
                return true; // Contraseña correcta
            } else {
                System.out.println("Contraseña incorrecta para: " + username);
                return false; // Contraseña incorrecta
            }
            

        } catch (SQLException e) {
            // Manejo de errores de base de datos
            System.err.println("Error en login: " + e.getMessage());
            return false;
        }
    }
}