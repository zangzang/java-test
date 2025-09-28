package com.example.entity;

import com.example.base.entity.User;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class GenericJooqUserDaoIntegrationTest {
    private Connection conn;
    private DSLContext dsl;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dsl = DSL.using(conn);
    // create a simple user table (quote table and column names to match jOOQ's quoted identifiers)
    dsl.execute("CREATE TABLE \"users\" (\"id\" INT AUTO_INCREMENT PRIMARY KEY, \"name\" VARCHAR(255), \"email\" VARCHAR(255))");
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (dsl != null) dsl.execute("DROP TABLE IF EXISTS users");
        if (conn != null) conn.close();
    }

    @Test
    void crudFlow() {
    org.jooq.Table<Record> usersTable = DSL.table(DSL.name("users"));
    Field<Integer> idField = DSL.field(DSL.name("users", "id"), Integer.class);
    Field<String> nameField = DSL.field(DSL.name("users", "name"), String.class);
    Field<String> emailField = DSL.field(DSL.name("users", "email"), String.class);

        GenericJooqUserDao<Record, Integer> dao = new GenericJooqUserDao<>(dsl, usersTable, idField, nameField, emailField) {};

        User u = new User();
        u.setName("Alice");
        u.setEmail("alice@example.com");

        dao.save(u);
        assertTrue(u.getId() > 0, "saved user should have generated id");

        var fetched = dao.findById(u.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Alice", fetched.get().getName());

        u.setName("Alice Smith");
        dao.update(u);

        var fetched2 = dao.findById(u.getId());
        assertTrue(fetched2.isPresent());
        assertEquals("Alice Smith", fetched2.get().getName());

        dao.deleteById(u.getId());
        var afterDelete = dao.findById(u.getId());
        assertFalse(afterDelete.isPresent());
    }
}
