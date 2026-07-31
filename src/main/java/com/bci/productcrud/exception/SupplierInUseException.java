package com.bci.productcrud.exception;

public class SupplierInUseException extends RuntimeException {

    public SupplierInUseException(String message) {
        super(message);
    }
}
