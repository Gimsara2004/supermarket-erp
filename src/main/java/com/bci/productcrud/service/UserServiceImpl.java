package com.bci.productcrud.service;

import com.bci.productcrud.exception.DuplicateUserEmailException;
import com.bci.productcrud.exception.DuplicateUsernameException;
import com.bci.productcrud.exception.UserNotFoundException;
import com.bci.productcrud.model.Role;
import com.bci.productcrud.model.User;
import com.bci.productcrud.repository.UserRepository;
import com.bci.productcrud.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;

    public UserServiceImpl(UserRepository userRepository, RoleService roleService) {
        this.userRepository = userRepository;
        this.roleService = roleService;
    }

    @Override
    public User create(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateUsernameException("A user with username " + user.getUsername() + " already exists");
        }
        if (user.getEmail() != null && !user.getEmail().isBlank() && userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateUserEmailException("A user with email " + user.getEmail() + " already exists");
        }
        user.setId(null);
        user.setRole(resolveRole(user.getRole()));
        user.setPassword(PasswordUtil.hash(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + id));
    }

    @Override
    public User update(Long id, User request) {
        User user = findById(id);

        userRepository.findByUsername(request.getUsername())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateUsernameException("A user with username " + request.getUsername() + " already exists");
                });

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            userRepository.findByEmail(request.getEmail())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new DuplicateUserEmailException("A user with email " + request.getEmail() + " already exists");
                    });
        }

        user.setFullName(request.getFullName());
        user.setUsername(request.getUsername());
        user.setRole(resolveRole(request.getRole()));
        user.setEmail(request.getEmail());
        user.setContactNo(request.getContactNo());
        user.setStatus(request.getStatus() == null ? user.getStatus() : request.getStatus());

        // Only re-hash if the client actually sent a new (non-empty, not-already-hashed) password.
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(PasswordUtil.hash(request.getPassword()));
        }

        return userRepository.save(user);
    }

    @Override
    public void delete(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    private Role resolveRole(Role requested) {
        if (requested == null || requested.getId() == null) {
            throw new IllegalArgumentException("A role must be selected for the user");
        }
        return roleService.findById(requested.getId());
    }
}
