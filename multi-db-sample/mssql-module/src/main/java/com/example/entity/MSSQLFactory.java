package com.example.entity;

import com.example.common.DatabaseFactory;
import com.example.base.dao.UserDao;
import com.example.common.DatabaseConnection;
import com.example.common.DefaultDatabaseConnection;

import java.sql.SQLException;

public class MSSQLFactory implements DatabaseFactory {
    private DatabaseConnection dbConnection;

    @Override
    public UserDao createUserDao() {
        try {
            return new MSSQLUserDao(dbConnection);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create MSSQL UserDao", e);
        }
    }

    @Override
    public DatabaseConnection createDatabaseConnection(String url, String user, String password) {
        this.dbConnection = new DefaultDatabaseConnection(url, user, password);
        return dbConnection;
    }
}