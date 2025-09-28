package com.example.common;

import com.example.base.dao.UserDao;

public interface DatabaseFactory {
    UserDao createUserDao();
    DatabaseConnection createDatabaseConnection(String url, String user, String password);
}