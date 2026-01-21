package com.tastedivekafka.db;

import java.sql.*;

public class MovieDAO {

    public int getOrCreateMovie(String name) throws SQLException {
        String select = "SELECT id FROM movies WHERE name=?";
        String insert = "INSERT INTO movies(name) VALUES(?) RETURNING id";

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(select);
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getInt("id");

            ps = con.prepareStatement(insert);
            ps.setString(1, name);
            rs = ps.executeQuery();
            rs.next();
            return rs.getInt("id");
        }
    }
}

