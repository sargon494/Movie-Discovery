package com.tastedivekafka.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecommendationDAO {

    public void save(int movieId, String recommendedName) throws SQLException {
        String sql = "INSERT INTO recommendations(movie_id, recommended_name) VALUES(?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, movieId);
            ps.setString(2, recommendedName);
            ps.executeUpdate();
        }
    }

    public List<String> getByMovieName(String movieName) throws SQLException {
        String sql = "SELECT r.recommended_name " +
                     "FROM recommendations r " +
                     "JOIN movies m ON m.id = r.movie_id " +
                     "WHERE m.name = ?";

        List<String> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, movieName);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(rs.getString("recommended_name"));
            }
        }
        return list;
    }
}
