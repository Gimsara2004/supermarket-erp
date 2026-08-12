package com.bci.productcrud.exception;

public class DuplicateReceiptNumberException extends RuntimeException {
    public DuplicateReceiptNumberException(String message) {
        super(message);
    }
}
