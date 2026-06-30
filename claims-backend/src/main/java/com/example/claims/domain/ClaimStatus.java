package com.example.claims.domain;

/** Lifecycle state of a claim. Stored as its name (EnumType.STRING). */
public enum ClaimStatus {
    submitted,
    paid,
    denied,
    pending
}
