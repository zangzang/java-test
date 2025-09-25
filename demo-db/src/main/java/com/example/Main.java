package com.example;

import com.example.dao.JpaUserDao;
import com.example.dao.JooqUserDao;
import com.example.entity.User;
import jakarta.persistence.*;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.jpa.extensions.DefaultAnnotatedPojoMemberProvider;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws SQLException {
        Properties props = new Properties();
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String dbUrl = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/testdb");
        String dbUser = props.getProperty("db.user", "username");
        String dbPassword = props.getProperty("db.password", "password");
        int iterations = Integer.parseInt(props.getProperty("app.iterations", "1000"));

        // Create JPA properties programmatically
        Map<String, String> jpaProps = new HashMap<>();
        jpaProps.put("jakarta.persistence.jdbc.url", dbUrl);
        jpaProps.put("jakarta.persistence.jdbc.user", dbUser);
        jpaProps.put("jakarta.persistence.jdbc.password", dbPassword);
        jpaProps.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        jpaProps.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProps.put("hibernate.hbm2ddl.auto", "update");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("my-persistence-unit", jpaProps);
        EntityManager em = emf.createEntityManager();
        JpaUserDao jpaDao = new JpaUserDao(em);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            DSLContext create = DSL.using(conn, SQLDialect.POSTGRES)
                    .configuration()
                    .derive(new DefaultAnnotatedPojoMemberProvider())
                    .dsl();
            JooqUserDao jooqDao = new JooqUserDao(create);

            // Truncate tables to start fresh
            jpaDao.truncateUsers();
            jooqDao.truncateUsers();

            long start, end;

            start = System.currentTimeMillis();
            for (int i = 1001; i <= 1000 + iterations; i++) {
            User user = new User();
            user.setId(i);
            user.setName("User" + i);
            user.setAge(20 + (i % 50));
            user.setStatus("Active");
            jpaDao.insertUser(user);
        }
        end = System.currentTimeMillis();
        System.out.println("JPA INSERT: " + (end - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 2001; i <= 2000 + iterations; i++) {
            jooqDao.insertUser(i, "User" + i, 20 + (i % 50), "Active");
        }
        end = System.currentTimeMillis();
        System.out.println("jOOQ INSERT: " + (end - start) + "ms");

        // JPA List Insert Benchmark
        List<User> jpaUserList = new ArrayList<>();
        for (int i = 3001; i <= 3000 + iterations; i++) {
            User user = new User();
            user.setId(i);
            user.setName("User" + i);
            user.setAge(20 + (i % 50));
            user.setStatus("Active");
            jpaUserList.add(user);
        }
        start = System.currentTimeMillis();
        jpaDao.insertUsers(jpaUserList);
        end = System.currentTimeMillis();
        System.out.println("JPA LIST INSERT: " + (end - start) + "ms");

        // jOOQ List Insert Benchmark
        List<User> jooqUserList = new ArrayList<>();
        for (int i = 4001; i <= 4000 + iterations; i++) {
            User user = new User();
            user.setId(i);
            user.setName("User" + i);
            user.setAge(20 + (i % 50));
            user.setStatus("Active");
            jooqUserList.add(user);
        }
        start = System.currentTimeMillis();
        jooqDao.insertUsers(jooqUserList);
        end = System.currentTimeMillis();
        System.out.println("jOOQ LIST INSERT: " + (end - start) + "ms");

        start = System.currentTimeMillis();
        List<User> jpaUsers = jpaDao.getUsersOlderThan(30);
        end = System.currentTimeMillis();
        System.out.println("JPA SELECT: " + (end - start) + "ms, count=" + jpaUsers.size());

            start = System.currentTimeMillis();
            List<User> jooqUsers = jooqDao.getUsersOlderThan(30);
            end = System.currentTimeMillis();
            System.out.println("jOOQ SELECT: " + (end - start) + "ms, count=" + jooqUsers.size());
        }

        em.close();
        emf.close();
    }
}
