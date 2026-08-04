package com.bci.productcrud.exception;

public class OverReceiptException extends RuntimeException {
    public OverReceiptException(String message) {
        super(message);
    }
}
