package com.example.base.dao;

import com.example.base.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<User> findById(Long id);
    List<User> findAll();
    void save(User user);
    void update(User user);
    void deleteById(Long id);
}