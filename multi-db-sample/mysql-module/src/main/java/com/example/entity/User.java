package com.example.entity;

import org.jooq.Record;

public class User extends com.example.base.entity.User {
    public User() { super(); }
    public User(String name, String email) { super(name, email); }

    public static User fromRecord(Record record) {
        // 더미 Codegen 컬럼 매핑
        return new User("mysql_name", "mysql_email@example.com");
    }
}