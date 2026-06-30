package com.example.claims.dto;

import com.example.claims.domain.Claim;
import com.example.claims.domain.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Full claim record. {@code imageId} is non-null when a scanned document is attached. */
public record ClaimDetail(
        String claimId,
        String patientName,
        String memberId,
        String payer,
        String cptCode,
        String icdCode,
        BigDecimal billedAmount,
        BigDecimal paidAmount,
        BigDecimal balance,
        ClaimStatus status,
        String denialReason,
        UUID imageId,
        Instant createdAt) {

    public static ClaimDetail of(Claim c) {
        return new ClaimDetail(
                c.getClaimId(), c.getPatientName(), c.getMemberId(), c.getPayer(),
                c.getCptCode(), c.getIcdCode(), c.getBilledAmount(), c.getPaidAmount(),
                c.getBalance(), c.getStatus(), c.getDenialReason(), c.getImageId(),
                c.getCreatedAt());
    }
}
