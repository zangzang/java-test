package com.example.entity;

import com.example.common.DatabaseConnection;
import static com.example.entity.generated.tables.User.USER;

import java.sql.SQLException;

public class MySQLUserDao extends GenericJooqUserDao<org.jooq.Record, Integer> {
    public MySQLUserDao(DatabaseConnection dbConnection) throws SQLException {
        super(JooqContext.getDSLContext(dbConnection), USER, USER.ID, USER.NAME, USER.EMAIL);
    }
}