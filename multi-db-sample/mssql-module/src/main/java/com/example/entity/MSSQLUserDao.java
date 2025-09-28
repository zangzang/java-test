package com.example.entity;

import com.example.common.DatabaseConnection;
import static com.example.entity.generated.tables.User.USER;

import java.sql.SQLException;

public class MSSQLUserDao extends GenericJooqUserDao<org.jooq.Record, Integer> {
    public MSSQLUserDao(DatabaseConnection dbConnection) throws SQLException {
        super(JooqContext.getDSLContext(dbConnection), USER, USER.ID, USER.NAME, USER.EMAIL);
    }
}