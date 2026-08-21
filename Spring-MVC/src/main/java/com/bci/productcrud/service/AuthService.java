package com.bci.productcrud.service;

import com.bci.productcrud.model.User;

public interface AuthService {
    /** Checks username/password and returns the matching, active User, or throws InvalidCredentialsException. */
    User login(String username, String password);
}
