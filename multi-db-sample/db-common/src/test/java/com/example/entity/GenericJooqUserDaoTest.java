package com.example.entity;

import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GenericJooqUserDaoTest {

    // Minimal fake Field implementation using jOOQ's DSL to create fields tied to a dummy table
    static class DummyTable {
        static final org.jooq.Table<?> TABLE = DSL.table(DSL.name("dummy"));
    }

    @Test
    void convertId_integer() {
        Field<Integer> idField = DSL.field(DSL.name("id"), SQLDataType.INTEGER);
        GenericJooqUserDao<?, Integer> dao = new GenericJooqUserDao<>(DummyTable.TABLE, idField, DSL.field("name", String.class), DSL.field("email", String.class));

        Integer converted = dao.convertId(123L);
        assertEquals(123, converted);
    }

    @Test
    void convertId_long() {
        Field<Long> idField = DSL.field(DSL.name("id"), SQLDataType.BIGINT);
        GenericJooqUserDao<?, Long> dao = new GenericJooqUserDao<>(DummyTable.TABLE, idField, DSL.field("name", String.class), DSL.field("email", String.class));

        Long converted = dao.convertId(123L);
        assertEquals(123L, converted);
    }

    @Test
    void convertNumberToLong_withInteger() {
        Field<Integer> idField = DSL.field(DSL.name("id"), SQLDataType.INTEGER);
        GenericJooqUserDao<?, Integer> dao = new GenericJooqUserDao<>(DummyTable.TABLE, idField, DSL.field("name", String.class), DSL.field("email", String.class));

        long l = dao.convertNumberToLong(42);
        assertEquals(42L, l);
    }

    @Test
    void convertNumberToLong_withStringNumber() {
        Field<String> idField = DSL.field(DSL.name("id"), String.class);
        GenericJooqUserDao<?, String> dao = new GenericJooqUserDao<>(DummyTable.TABLE, idField, DSL.field("name", String.class), DSL.field("email", String.class));

        long l = dao.convertNumberToLong("55");
        assertEquals(55L, l);
    }
}
