package com.tastedivekafka.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

private static final String URL = "jdbc:postgresql://localhost:5433/mi_base_datos";
private static final String USER = "appuser";
private static final String PASS = "1234";

    public static Connection getConnection() throws SQLException {
    System.out.println("DBConnection -> USER=" + USER + " PASS=" + PASS);
    return DriverManager.getConnection(URL, USER, PASS);
}
}
