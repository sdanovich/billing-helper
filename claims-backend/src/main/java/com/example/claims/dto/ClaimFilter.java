package com.example.claims.dto;

import com.example.claims.domain.ClaimStatus;

/** Optional list filters. Any null field is ignored by the query. */
public record ClaimFilter(
        ClaimStatus status,
        String payer,
        String memberId,
        String q) {
}
