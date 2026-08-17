package com.bci.productcrud.service;

import com.bci.productcrud.model.User;

import java.util.List;

public interface UserService {
    User create(User user);
    List<User> findAll();
    User findById(Long id);
    User update(Long id, User user);
    void delete(Long id);
}
