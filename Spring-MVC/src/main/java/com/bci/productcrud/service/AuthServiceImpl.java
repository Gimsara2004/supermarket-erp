package com.bci.productcrud.service;

import com.bci.productcrud.exception.InvalidCredentialsException;
import com.bci.productcrud.model.User;
import com.bci.productcrud.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Incorrect username or password"));

        // NOTE: plain-text comparison, matching how the password is stored (see User.java).
        // Fine for this demo/coursework project; never do this in anything handling real users.
        if (!user.getPassword().equals(password)) {
            throw new InvalidCredentialsException("Incorrect username or password");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new InvalidCredentialsException("This account is not active");
        }

        return user;
    }
}
