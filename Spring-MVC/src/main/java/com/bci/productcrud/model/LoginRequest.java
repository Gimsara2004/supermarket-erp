package com.bci.productcrud.model;

import jakarta.validation.constraints.NotBlank;

/** Plain request body for POST /api/auth/login - not a JPA entity, not a database table. */
public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    public LoginRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
