package com.example.dao;

import com.example.entity.User;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Table;
import org.jooq.Field;
import org.jooq.impl.DSL;
import java.util.List;


public class JooqUserDao {
    private final DSLContext create;
    private final Table<?> usersTable;
    private final Field<Integer> idField;
    private final Field<String> nameField;
    private final Field<Integer> ageField;
    private final Field<String> statusField;

    public JooqUserDao(DSLContext create) {
        this.create = create;
        // Use unqualified names so the connection's default schema is used
        this.usersTable = DSL.table(DSL.name("users"));
        this.idField = DSL.field(DSL.name("id"), Integer.class);
        this.nameField = DSL.field(DSL.name("name"), String.class);
        this.ageField = DSL.field(DSL.name("age"), Integer.class);
        this.statusField = DSL.field(DSL.name("status"), String.class);
    }

    public void insertUser(int id, String name, int age, String status) {
        create.insertInto(usersTable, idField, nameField, ageField, statusField)
              .values(id, name, age, status)
              .execute();
    }

    public void insertUsers(List<User> users) {
        // Create an array of queries and execute as a batch
        Query[] inserts = users.stream()
                .map(u -> create.insertInto(usersTable, idField, nameField, ageField, statusField)
                        .values(u.getId(), u.getName(), u.getAge(), u.getStatus()))
                .toArray(Query[]::new);
        create.batch(inserts).execute();
    }

    public void truncateUsers() {
        // Use an unqualified DELETE so the default schema for the connection is used
        create.deleteFrom(usersTable).execute();
    }

    public List<User> getUsersOlderThan(int age) {
        return create.selectFrom(usersTable)
                     .where(ageField.gt(age))
                     .fetchInto(User.class);
    }

    public void updateUserStatus(int id, String status) {
        create.update(usersTable)
              .set(statusField, status)
              .where(idField.eq(id))
              .execute();
    }

    public void deleteUser(int id) {
        create.deleteFrom(usersTable)
              .where(idField.eq(id))
              .execute();
    }
}