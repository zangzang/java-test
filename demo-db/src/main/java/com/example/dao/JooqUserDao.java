package com.example.dao;

import com.example.entity.User;
import org.jooq.DSLContext;
import static com.example.generated.Tables.USERS;
import java.util.List;


public class JooqUserDao {
    private final DSLContext create;

    public JooqUserDao(DSLContext create) { this.create = create; }

    public void insertUser(int id, String name, int age, String status) {
        create.insertInto(USERS, USERS.ID, USERS.NAME, USERS.AGE, USERS.STATUS)
              .values(id, name, age, status)
              .execute();
    }

    public void insertUsers(List<User> users) {
        var inserts = users.stream()
                .map(user -> create.insertInto(USERS, USERS.ID, USERS.NAME, USERS.AGE, USERS.STATUS)
                        .values(user.getId(), user.getName(), user.getAge(), user.getStatus()))
                .toArray(org.jooq.Insert[]::new);
        create.batch(inserts).execute();
    }

    public void truncateUsers() {
        create.truncate(USERS).execute();
    }

    public List<User> getUsersOlderThan(int age) {
        return create.selectFrom(USERS)
                     .where(USERS.AGE.gt(age))
                     .fetchInto(User.class);
    }

    public void updateUserStatus(int id, String status) {
        create.update(USERS)
              .set(USERS.STATUS, status)
              .where(USERS.ID.eq(id))
              .execute();
    }

    public void deleteUser(int id) {
        create.deleteFrom(USERS)
              .where(USERS.ID.eq(id))
              .execute();
    }
}