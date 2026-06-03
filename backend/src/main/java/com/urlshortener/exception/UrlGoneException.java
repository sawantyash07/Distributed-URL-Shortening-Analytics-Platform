package com.urlshortener.exception;

public class UrlGoneException extends RuntimeException {

    public UrlGoneException(String message) {
        super(message);
    }
}
