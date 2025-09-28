package com.example.dao;

import org.jooq.DSLContext;
import org.jooq.BatchBindStep;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;

import com.example.entity.User;

import java.util.List;
import java.util.ArrayList;

public class JooqDslUserDao {
    private final DSLContext dsl;

    public JooqDslUserDao(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void truncateUsers() {
        dsl.execute("TRUNCATE TABLE users");
    }

    public void insertUser(int id, String name, int age, String status) {
        dsl.insertInto(DSL.table("users"),
                DSL.field("id"), DSL.field("name"), DSL.field("age"), DSL.field("status"))
            .values(id, name, age, status)
            .execute();
    }

    public void insertUsers(List<User> users) {
        if (users == null || users.isEmpty()) return;
        BatchBindStep batch = dsl.batch(
            dsl.insertInto(DSL.table("users"),
                    DSL.field("id"), DSL.field("name"), DSL.field("age"), DSL.field("status"))
               .values((Integer) null, null, null, null)
        );
        for (User u : users) {
            batch.bind(u.getId(), u.getName(), u.getAge(), u.getStatus());
        }
        batch.execute();
    }

    public List<User> getUsersOlderThan(int age) {
        List<User> out = new ArrayList<>();
        Result<Record> res = dsl.select().from(DSL.table("users")).where(DSL.field("age").gt(age)).fetch();
        for (Record r : res) {
            User u = new User();
            u.setId(r.get("id", Integer.class));
            u.setName(r.get("name", String.class));
            u.setAge(r.get("age", Integer.class));
            u.setStatus(r.get("status", String.class));
            out.add(u);
        }
        return out;
    }
}
