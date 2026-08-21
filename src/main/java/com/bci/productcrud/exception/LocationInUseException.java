package com.bci.productcrud.exception;

public class LocationInUseException extends RuntimeException {

    public LocationInUseException(String message) {
        super(message);
    }
}
