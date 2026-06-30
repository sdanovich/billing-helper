package com.example.claims.dto;

import com.example.claims.domain.ClaimStatus;

import java.math.BigDecimal;

/**
 * Parsed claim fields submitted by the scan flow. The Android app extracts these on-device
 * (ML Kit OCR + deterministic parse), the clerk confirms/corrects, then POSTs them. Any
 * accompanying image is uploaded as a separate multipart part, not in this body.
 */
public record IngestClaimRequest(
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
        String denialReason) {
}
