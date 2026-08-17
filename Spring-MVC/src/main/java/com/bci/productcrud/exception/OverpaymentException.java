package com.bci.productcrud.exception;

public class OverpaymentException extends RuntimeException {
    public OverpaymentException(String message) {
        super(message);
    }
}
