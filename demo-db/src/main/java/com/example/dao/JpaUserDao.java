package com.example.dao;

import com.example.entity.User;
import jakarta.persistence.EntityManager;
import java.util.List;

public class JpaUserDao {
    private final EntityManager em;

    public JpaUserDao(EntityManager em) { this.em = em; }

    public void insertUser(User user) {
        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();
    }

    public void insertUsers(List<User> users) {
        em.getTransaction().begin();
        for (User user : users) {
            em.persist(user);
        }
        em.getTransaction().commit();
    }

    public void truncateUsers() {
        em.getTransaction().begin();
        em.createNativeQuery("TRUNCATE TABLE users").executeUpdate();
        em.getTransaction().commit();
    }

    public List<User> getUsersOlderThan(int age) {
        return em.createQuery("SELECT u FROM User u WHERE u.age > :age", User.class)
                 .setParameter("age", age)
                 .getResultList();
    }

    public void updateUserStatus(int id, String status) {
        em.getTransaction().begin();
        User user = em.find(User.class, id);
        if (user != null) user.setStatus(status);
        em.getTransaction().commit();
    }

    public void deleteUser(int id) {
        em.getTransaction().begin();
        User user = em.find(User.class, id);
        if (user != null) em.remove(user);
        em.getTransaction().commit();
    }
}
