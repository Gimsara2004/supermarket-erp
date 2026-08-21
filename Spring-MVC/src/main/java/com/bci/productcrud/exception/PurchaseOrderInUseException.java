package com.bci.productcrud.exception;

public class PurchaseOrderInUseException extends RuntimeException {
    public PurchaseOrderInUseException(String message) {
        super(message);
    }
}
