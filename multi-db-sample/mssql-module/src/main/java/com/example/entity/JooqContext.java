package com.example.entity;

import com.example.common.DatabaseConnection;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.SQLException;

public class JooqContext {
    public static DSLContext getDSLContext(DatabaseConnection dbConnection) throws SQLException {
        Connection connection = dbConnection.getConnection();
        // MSSQL의 경우 SQLDialect.DEFAULT 사용 (SQL Server 호환)
        return DSL.using(connection, SQLDialect.DEFAULT);
    }
}