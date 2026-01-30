package com.tastedivekafka.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase utilitaria para gestionar la conexión a la base de datos PostgreSQL.
 *
 * Se usa desde los DAOs (MovieDAO, RecommendationDAO, UserDAO) para obtener
 * conexiones de manera centralizada.
 */
public class DBConnection {

    // URL de conexión a la base de datos
    private static final String URL = "jdbc:postgresql://localhost:5433/mi_base_datos";
    // Usuario de la base de datos
    private static final String USER = "appuser";
    // Contraseña del usuario
    private static final String PASS = "1234";

    /**
     * Obtiene una conexión nueva a la base de datos.
     *
     * @return objeto Connection listo para usar
     * @throws SQLException si hay errores de conexión
     */
    public static Connection getConnection() throws SQLException {
        // Log de depuración (muestra usuario y contraseña que se usarán)
        System.out.println("DBConnection -> USER=" + USER + " PASS=" + PASS);
        // Se devuelve una nueva conexión usando DriverManager
        return DriverManager.getConnection(URL, USER, PASS);
    }
}