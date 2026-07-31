package com.bci.productcrud.exception;

public class DuplicateSupplierEmailException extends RuntimeException {

    public DuplicateSupplierEmailException(String message) {
        super(message);
    }
}
