package com.example.claims.store;

import com.example.claims.dto.ClaimDetail;
import com.example.claims.dto.ClaimFilter;
import com.example.claims.dto.ClaimSummary;
import com.example.claims.dto.IngestClaimRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Storage-agnostic access to claims and their scanned images. The controller depends only
 * on this interface so the backing store (Postgres now) stays swappable.
 */
public interface ClaimStore {

    Page<ClaimSummary> search(ClaimFilter filter, Pageable pageable);

    Optional<ClaimDetail> findDetail(String claimId);

    /** Persist a parsed claim and (optionally) its scanned image. Returns the stored detail. */
    ClaimDetail ingest(IngestClaimRequest req, byte[] imageBytes, String contentType, String filename);

    /** The scanned image bytes for a claim, if one is attached. */
    Optional<ImageBlob> findImage(String claimId);

    boolean exists(String claimId);

    /** Image bytes plus their content type, for streaming back to the client. */
    record ImageBlob(byte[] content, String contentType) {
    }
}
