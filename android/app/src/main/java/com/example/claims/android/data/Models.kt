package com.example.claims.android.data

/** Stable page envelope matching the backend's VIA_DTO page serialization. */
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val page: PageMeta = PageMeta()
)

data class PageMeta(
    val size: Int = 0,
    val number: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0
)

data class ClaimSummary(
    val claimId: String,
    val patientName: String,
    val memberId: String,
    val payer: String,
    val status: String,
    val balance: Double
)

data class ClaimDetail(
    val claimId: String,
    val patientName: String,
    val memberId: String,
    val payer: String,
    val cptCode: String?,
    val icdCode: String?,
    val billedAmount: Double?,
    val paidAmount: Double?,
    val balance: Double?,
    val status: String,
    val denialReason: String?,
    val imageId: String?,
    val createdAt: String?
)

/** Structured fields the scan flow submits (the JSON `claim` part of the multipart POST). */
data class IngestClaim(
    val claimId: String,
    val patientName: String,
    val memberId: String,
    val payer: String,
    val cptCode: String?,
    val icdCode: String?,
    val billedAmount: Double?,
    val paidAmount: Double?,
    val balance: Double?,
    val status: String,
    val denialReason: String?
)

val CLAIM_STATUSES = listOf("submitted", "paid", "denied", "pending")
