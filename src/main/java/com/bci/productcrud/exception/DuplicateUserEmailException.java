package com.bci.productcrud.exception;

public class DuplicateUserEmailException extends RuntimeException {

    public DuplicateUserEmailException(String message) {
        super(message);
    }
}
