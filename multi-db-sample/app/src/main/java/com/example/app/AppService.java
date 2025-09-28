package com.example.app;

import com.example.base.entity.User;
import com.example.common.DatabaseFactory;
import com.example.base.dao.UserDao;
import java.util.List;
import java.util.Optional;

public class AppService {
    private UserDao userDao;

    public AppService(DatabaseFactory factory) {
        // PostgreSQL connection info (in real app, load from config)
        String url = "jdbc:postgresql://10.100.10.122:5432/testdb";
        String user = "juro";
        String password = "jurodb_-1q2w3e4r5t";

        // Create database connection first
        factory.createDatabaseConnection(url, user, password);
        // Then create user dao (which uses the connection)
        this.userDao = factory.createUserDao();
    }

    public void testCRUD() {
        try {
            // Create table if not exists (for demo)
            createTableIfNotExists();

            // Create
            User newUser = new User("Test User", "test@example.com");
            userDao.save(newUser);
            System.out.println("Created user: " + newUser.getId() + " - " + newUser.getName());

            // Read
            Optional<User> readUserOpt = userDao.findById(newUser.getId());
            if (readUserOpt.isPresent()) {
                User readUser = readUserOpt.get();
                System.out.println("Read user: " + readUser.getId() + " - " + readUser.getName() + " <" + readUser.getEmail() + ">");
            }

            // Update
            readUserOpt.ifPresent(readUser -> {
                readUser.setName("Updated User");
                userDao.update(readUser);
                System.out.println("Updated user: " + readUser.getId() + " - " + readUser.getName());
            });

            // Read All
            List<User> users = userDao.findAll();
            System.out.println("All users:");
            for (User u : users) {
                System.out.println("  " + u.getId() + " - " + u.getName() + " <" + u.getEmail() + ">");
            }

            // Delete
            userDao.deleteById(newUser.getId());
            System.out.println("Deleted user: " + newUser.getId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTableIfNotExists() throws Exception {
        // For demo, assume table exists or create via SQL
        // In real app, use migration tools like Flyway
        System.out.println("Table 'user' assumed to exist.");
    }

    public static void main(String[] args) {
        // Choose DB implementation
        DatabaseFactory factory = new com.example.entity.PostgreSQLFactory();
        AppService app = new AppService(factory);
        app.testCRUD();
    }
}