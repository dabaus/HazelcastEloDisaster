package com.example.demo.factory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnectionFactoryImpl implements DbConnectionFactory {

    public Connection openConnection() {
        try {
            return DriverManager.getConnection("jdbc:postgresql:hazeldb", "postgres", "postgres");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
