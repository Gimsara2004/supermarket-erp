package com.bci.productcrud.exception;

public class RoleInUseException extends RuntimeException {

    public RoleInUseException(String message) {
        super(message);
    }
}
