package com.tastedivekafka.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO (Data Access Object) para la tabla "movies".
 *
 * Permite:
 * - Buscar películas existentes
 * - Crear nuevas películas si no existen
 * 
 * Se usa desde KafkaConsumerService para almacenar las películas
 * que reciben recomendaciones.
 */
public class MovieDAO {

    /**
     * Busca una película por nombre y la devuelve si existe,
     * o la crea si no existe. Devuelve el ID de la película.
     *
     * @param name nombre de la película
     * @return id de la película en la base de datos, -1 si falla
     * @throws SQLException si hay errores de base de datos
     */
    public int getOrCreateMovie(String name) throws SQLException {
        // SELECT usando LOWER para ignorar mayúsculas/minúsculas
        String select = "SELECT id FROM movies WHERE LOWER(name) = LOWER(?)";
        // INSERT con protección ON CONFLICT para no duplicar
        String insert = "INSERT INTO movies(name) VALUES(?) ON CONFLICT (LOWER(name)) DO NOTHING RETURNING id";

        try (Connection con = DBConnection.getConnection()) {

            // 1️⃣ Intentamos buscar primero
            try (PreparedStatement psSelect = con.prepareStatement(select)) {
                psSelect.setString(1, name.trim());
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) return rs.getInt("id"); // existe → devolvemos id
                }
            }

            // 2️⃣ Si no existe, insertamos
            // ON CONFLICT evita errores si dos hilos insertan al mismo tiempo
            try (PreparedStatement psInsert = con.prepareStatement(insert)) {
                psInsert.setString(1, name.trim());
                try (ResultSet rs = psInsert.executeQuery()) {
                    if (rs.next()) return rs.getInt("id"); // insert OK → devolvemos id
                }
            }

            // 3️⃣ Si el INSERT no devolvió nada (hubo conflicto), buscamos una última vez
            try (PreparedStatement psRetry = con.prepareStatement(select)) {
                psRetry.setString(1, name.trim());
                try (ResultSet rs = psRetry.executeQuery()) {
                    if (rs.next()) return rs.getInt("id");
                }
            }
        }

        // Falla todo → devolvemos -1
        return -1;
    }
}

