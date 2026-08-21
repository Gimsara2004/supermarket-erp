package com.bci.productcrud.exception;

public class DuplicateLocationNameException extends RuntimeException {
    public DuplicateLocationNameException(String message) {
        super(message);
    }
}
