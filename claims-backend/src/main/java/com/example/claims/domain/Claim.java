package com.example.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A synthetic claim — the structured system-of-record row.
 *
 * <p>{@code imageId} is a plain nullable reference to a row in {@code claim_images}, not a
 * JPA association: keeping them decoupled means a query for claims never joins or loads
 * image bytes, which preserves the boundary rule that scanned images never leave via the
 * agent's read path.
 */
@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @Column(name = "claim_id")
    private String claimId;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(nullable = false)
    private String payer;

    @Column(name = "cpt_code")
    private String cptCode;

    @Column(name = "icd_code")
    private String icdCode;

    @Column(name = "billed_amount", nullable = false)
    private BigDecimal billedAmount;

    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount;

    @Column(nullable = false)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(name = "denial_reason")
    private String denialReason;

    /** Nullable id of the scanned source image in {@code claim_images}; null if none. */
    @Column(name = "image_id")
    private UUID imageId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Claim() {
    }

    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getPayer() { return payer; }
    public void setPayer(String payer) { this.payer = payer; }

    public String getCptCode() { return cptCode; }
    public void setCptCode(String cptCode) { this.cptCode = cptCode; }

    public String getIcdCode() { return icdCode; }
    public void setIcdCode(String icdCode) { this.icdCode = icdCode; }

    public BigDecimal getBilledAmount() { return billedAmount; }
    public void setBilledAmount(BigDecimal billedAmount) { this.billedAmount = billedAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }

    public String getDenialReason() { return denialReason; }
    public void setDenialReason(String denialReason) { this.denialReason = denialReason; }

    public UUID getImageId() { return imageId; }
    public void setImageId(UUID imageId) { this.imageId = imageId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
