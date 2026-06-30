package com.example.claims.dto;

import com.example.claims.domain.ClaimStatus;

import java.math.BigDecimal;

/** Row shape for the claims list (no image bytes). */
public record ClaimSummary(
        String claimId,
        String patientName,
        String memberId,
        String payer,
        ClaimStatus status,
        BigDecimal balance) {
}
