package com.example.claims.error;

/** Thrown when ingesting a claim id that already exists. Mapped to HTTP 409. */
public class DuplicateClaimException extends RuntimeException {
    public DuplicateClaimException(String message) {
        super(message);
    }
}
