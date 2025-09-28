package com.example.entity;

import com.example.common.DatabaseConnection;
import static com.example.entity.generated.tables.User.USER;
import com.example.entity.generated.tables.records.UserRecord;

import java.sql.SQLException;

public class PostgreSQLUserDao extends GenericJooqUserDao<UserRecord, Integer> {
    public PostgreSQLUserDao(DatabaseConnection dbConnection) throws SQLException {
        super(JooqContext.getDSLContext(dbConnection), USER, USER.ID, USER.NAME, USER.EMAIL);
    }
}