package com.example.entity;

import com.example.common.DatabaseConnection;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import java.sql.Connection;
import java.sql.SQLException;

public class JooqContext {
    public static DSLContext getDSLContext(DatabaseConnection dbConnection) throws SQLException {
        Connection conn = dbConnection.getConnection();
        return DSL.using(conn, SQLDialect.POSTGRES);
    }
}