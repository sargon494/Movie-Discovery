package com.tastedivekafka.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) para la tabla "recommendations".
 *
 * Permite:
 * - Guardar recomendaciones asociadas a una película
 * - Consultar recomendaciones por nombre de película
 */
public class RecommendationDAO {

    /**
     * Guarda una recomendación para una película concreta.
     *
     * @param movieId ID de la película original
     * @param recommendedName nombre de la película recomendada
     * @throws SQLException si hay error en la base de datos
     */
    public void save(int movieId, String recommendedName) throws SQLException {
        String sql = "INSERT INTO recommendations(movie_id, recommended_name) VALUES(?,?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Asignamos parámetros
            ps.setInt(1, movieId);
            ps.setString(2, recommendedName);

            // Ejecutamos insert
            ps.executeUpdate();
        }
    }

    /**
     * Obtiene todas las recomendaciones asociadas a un nombre de película.
     *
     * @param movieName nombre de la película
     * @return lista de nombres de películas recomendadas
     * @throws SQLException si hay error en la base de datos
     */
    public List<String> getByMovieName(String movieName) throws SQLException {
        String sql = "SELECT r.recommended_name " +
                     "FROM recommendations r " +
                     "JOIN movies m ON m.id = r.movie_id " +
                     "WHERE m.name = ?";

        List<String> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Asignamos parámetro
            ps.setString(1, movieName);

            // Ejecutamos consulta
            ResultSet rs = ps.executeQuery();

            // Recorremos resultados y los añadimos a la lista
            while (rs.next()) {
                list.add(rs.getString("recommended_name"));
            }
        }
        return list;
    }
}
