package com.bci.productcrud.service;

import com.bci.productcrud.model.Role;

import java.util.List;

public interface RoleService {
    Role create(Role role);
    List<Role> findAll();
    Role findById(Long id);
    Role update(Long id, Role role);
    void delete(Long id);
}
