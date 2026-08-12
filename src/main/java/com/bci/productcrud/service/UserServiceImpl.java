package com.bci.productcrud.service;

import com.bci.productcrud.exception.DuplicateUserEmailException;
import com.bci.productcrud.exception.DuplicateUsernameException;
import com.bci.productcrud.exception.UserNotFoundException;
import com.bci.productcrud.model.Role;
import com.bci.productcrud.model.User;
import com.bci.productcrud.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final com.bci.productcrud.repository.SalesPaymentReceiptRepository salesPaymentReceiptRepository;

    public UserServiceImpl(UserRepository userRepository, RoleService roleService,
                            com.bci.productcrud.repository.SalesPaymentReceiptRepository salesPaymentReceiptRepository) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.salesPaymentReceiptRepository = salesPaymentReceiptRepository;
    }

    @Override
    public User create(User request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException("A user with username " + request.getUsername() + " already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserEmailException("A user with email " + request.getEmail() + " already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRole(resolveRole(request.getRole()));
        user.setEmail(request.getEmail());
        user.setContactNo(request.getContactNo());
        user.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

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

        user.setFullName(request.getFullName());
        user.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(request.getPassword());
        }
        user.setRole(resolveRole(request.getRole()));
        user.setEmail(request.getEmail());
        user.setContactNo(request.getContactNo());
        user.setStatus(request.getStatus());

        return userRepository.save(user);
    }

    @Override
    public void delete(Long id) {
        User user = findById(id);
        if (salesPaymentReceiptRepository.existsByCashierId(id)) {
            throw new IllegalArgumentException(
                    "Cannot delete user " + user.getUsername() + " - they are recorded as the cashier on one or more sales receipts");
        }
        userRepository.delete(user);
    }

    private Role resolveRole(Role requested) {
        if (requested == null || requested.getId() == null) {
            throw new IllegalArgumentException("A role must be selected for the user");
        }
        return roleService.findById(requested.getId());
    }
}
