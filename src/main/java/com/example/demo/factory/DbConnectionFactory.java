package com.example.demo.factory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public interface DbConnectionFactory {

    public Connection openConnection();
}
