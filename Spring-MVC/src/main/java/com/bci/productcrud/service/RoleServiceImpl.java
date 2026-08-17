package com.bci.productcrud.service;

import com.bci.productcrud.exception.DuplicateRoleNameException;
import com.bci.productcrud.exception.RoleNotFoundException;
import com.bci.productcrud.model.Role;
import com.bci.productcrud.repository.RoleRepository;
import com.bci.productcrud.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleServiceImpl(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Role create(Role role) {
        if (roleRepository.existsByName(role.getName())) {
            throw new DuplicateRoleNameException("A role named " + role.getName() + " already exists");
        }
        role.setId(null);
        return roleRepository.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Role findById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with id " + id));
    }

    @Override
    public Role update(Long id, Role request) {
        Role role = findById(id);
        roleRepository.findByName(request.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateRoleNameException("A role named " + request.getName() + " already exists");
                });
        role.setName(request.getName());
        return roleRepository.save(role);
    }

    @Override
    public void delete(Long id) {
        Role role = findById(id);
        if (userRepository.existsByRoleId(id)) {
            throw new IllegalArgumentException("Cannot delete role " + role.getName() + " - one or more users still have this role");
        }
        roleRepository.delete(role);
    }
}
