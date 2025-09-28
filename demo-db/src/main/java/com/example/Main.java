package com.example;

import com.example.dao.JpaUserDao;
import com.example.dao.JooqUserDao;
import com.example.entity.User;
import jakarta.persistence.*;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.jpa.extensions.DefaultAnnotatedPojoMemberProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
// removed unused imports
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) throws SQLException {
        String db = System.getProperty("db", "postgres");
        // Set MDC context for structured logs (db and runId)
        String runId = java.util.UUID.randomUUID().toString();
        MDC.put("db", db);
        MDC.put("runId", runId);
    String dbUrl, dbUser, dbPassword, dbDriver, dbDialect;
        int iterations;
        if (db.equals("mssql")) {
            dbUrl = "jdbc:sqlserver://10.100.10.122:1433;databaseName=JwjangDB;encrypt=false;trustServerCertificate=true";
            dbUser = "sa";
            dbPassword = "jurodb_-1q2w3e4r5t";
            dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            dbDialect = "org.hibernate.dialect.SQLServerDialect";
            
            iterations = 1000;
        } else if (db.equals("mysql")) {
            dbUrl = "jdbc:mysql://10.100.10.122:3306/juro";
            dbUser = "juro";
            dbPassword = "jurodb_-1q2w3e4r5t";
            dbDriver = "com.mysql.cj.jdbc.Driver";
            dbDialect = "org.hibernate.dialect.MySQLDialect";
            
            iterations = 1000;
        } else {
            dbUrl = "jdbc:postgresql://10.100.10.122:5432/testdb";
            dbUser = "juro";
            dbPassword = "jurodb_-1q2w3e4r5t";
            dbDriver = "org.postgresql.Driver";
            dbDialect = "org.hibernate.dialect.PostgreSQLDialect";
            
            iterations = 1000;
        }

        // Create JPA properties programmatically
        Map<String, String> jpaProps = new HashMap<>();
        jpaProps.put("jakarta.persistence.jdbc.url", dbUrl);
        jpaProps.put("jakarta.persistence.jdbc.user", dbUser);
        jpaProps.put("jakarta.persistence.jdbc.password", dbPassword);
        jpaProps.put("jakarta.persistence.jdbc.driver", dbDriver);
        jpaProps.put("hibernate.dialect", dbDialect);
        jpaProps.put("hibernate.hbm2ddl.auto", "update");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("my-persistence-unit", jpaProps);
        EntityManager em = emf.createEntityManager();
        JpaUserDao jpaDao = new JpaUserDao(em);

        // Truncate tables to start fresh
        jpaDao.truncateUsers();

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

        // Use generated jOOQ classes (codegen) via JooqUserDao
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            DSLContext create = DSL.using(conn)
                    .configuration()
                    .derive(new DefaultAnnotatedPojoMemberProvider())
                    .dsl();
            JooqUserDao jooqDao = new JooqUserDao(create);

            jooqDao.truncateUsers();

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

        // Close JPA resources
        em.close();
        emf.close();

        // Register a shutdown hook to ensure cleanup runs even if JVM is terminated abruptly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("[shutdown-hook] starting cleanup, db={}", db);
            try {
                cleanup();
                log.info("[shutdown-hook] cleanup completed");
            } catch (Exception e) {
                log.error("[shutdown-hook] error during cleanup: {}", e.getMessage(), e);
            }
        }));

        // Also invoke cleanup now (for the happy path)
        cleanup();
        // Clear MDC after cleanup
        MDC.clear();
    }

    // Extracted cleanup logic so it can be reused from a shutdown hook
    private static void cleanup() {
        // Cleanup JDBC drivers
        log.info("[cleanup] deregistering JDBC drivers");
        try {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                try {
                    String driverInfo = driver.getClass().getName();
                    DriverManager.deregisterDriver(driver);
                    log.info("[cleanup] deregistered driver: {}", driverInfo);
                } catch (SQLException e) {
                    log.warn("[cleanup] warning deregistering driver: {} -> {}", driver.getClass().getName(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.warn("[cleanup] warning during driver deregistration: {}", e.getMessage(), e);
        }

        // Try to shutdown MySQL abandoned connection cleanup thread if present
        try {
            Class<?> cleanupClass = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
            java.lang.reflect.Method m = cleanupClass.getMethod("checkedShutdown");
            m.invoke(null);
        } catch (ClassNotFoundException cnfe) {
            log.debug("[cleanup] MySQL abandoned connection cleanup class not found; skipping");
        } catch (Throwable t) {
            log.warn("[cleanup] Could not shutdown MySQL cleanup thread: {}", t.getMessage(), t);
        }
        // Clear MDC in case cleanup called from shutdown hook
        try {
            MDC.clear();
        } catch (Exception e) {
            log.debug("[cleanup] MDC clear failed: {}", e.getMessage(), e);
        }
    }
}
