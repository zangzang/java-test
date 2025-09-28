package com.example.entity;

import com.example.common.DatabaseFactory;
import com.example.base.dao.UserDao;
import com.example.common.DatabaseConnection;
import com.example.common.DefaultDatabaseConnection;

import java.sql.SQLException;

public class PostgreSQLFactory implements DatabaseFactory {
    private DatabaseConnection dbConnection;

    @Override
    public UserDao createUserDao() {
        try {
            return new PostgreSQLUserDao(dbConnection);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create PostgreSQL UserDao", e);
        }
    }

    @Override
    public DatabaseConnection createDatabaseConnection(String url, String user, String password) {
        this.dbConnection = new DefaultDatabaseConnection(url, user, password);
        return dbConnection;
    }
}