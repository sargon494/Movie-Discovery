package com.tastedivekafka.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase utilitaria para gestionar la conexión a la base de datos PostgreSQL.
 *
 * Orden de prioridad:
 *  1. Variables de entorno DB_URL, DB_USER, DB_PASSWORD  ← usadas en Docker
 *  2. Valores por defecto hardcodeados                   ← usados en local
 *
 * En local PostgreSQL escucha en el puerto 5433 (mapeado en docker-compose).
 * En Docker los contenedores se hablan por el puerto interno 5432.
 */
public class DBConnection {

    // Lee variable de entorno, si no existe usa el valor local
    private static final String URL  = System.getenv().getOrDefault(
            "DB_URL",      "jdbc:postgresql://localhost:5433/mi_base_datos");

    private static final String USER = System.getenv().getOrDefault(
            "DB_USER",     "admin");

    private static final String PASS = System.getenv().getOrDefault(
            "DB_PASSWORD", "felipesql");

    /**
     * Obtiene una conexión nueva a la base de datos.
     *
     * @return objeto Connection listo para usar
     * @throws SQLException si hay errores de conexión
     */
    public static Connection getConnection() throws SQLException {
        System.out.println("DBConnection -> URL=" + URL + " USER=" + USER);
        return DriverManager.getConnection(URL, USER, PASS);
    }
}