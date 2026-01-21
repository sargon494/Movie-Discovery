package com.tastedivekafka.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public boolean login(String username, String password) {
    System.out.println("Intentando login: " + username + " / " + password);

    String sql = "SELECT 1 FROM users WHERE username=? AND password=?";
    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        System.out.println("Conexión OK: " + con);

        ps.setString(1, username);
        ps.setString(2, password);

        ResultSet rs = ps.executeQuery();

        boolean ok = rs.next();
        System.out.println("Resultado: " + ok);
        return ok;

    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

}
