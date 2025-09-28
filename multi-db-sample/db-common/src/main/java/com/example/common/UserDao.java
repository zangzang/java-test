package com.example.common;

import com.example.base.entity.User;
import java.util.List;

public interface UserDao {
    void create(User user) throws Exception;
    User read(Long id) throws Exception;
    List<User> readAll() throws Exception;
    void update(User user) throws Exception;
    void delete(Long id) throws Exception;
}