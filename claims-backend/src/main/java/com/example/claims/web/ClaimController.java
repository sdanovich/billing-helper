package com.example.claims.web;

import com.danovich.platform.login.CurrentUser;
import com.example.claims.domain.ClaimStatus;
import com.example.claims.dto.ClaimDetail;
import com.example.claims.dto.ClaimFilter;
import com.example.claims.dto.ClaimSummary;
import com.example.claims.dto.IngestClaimRequest;
import com.example.claims.store.ClaimStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Claims API for the clerk app. Every route is under {@code /api/} and so is enforced by the
 * platform-stack {@code JwtAuthFilter}; {@code @CurrentUser} additionally resolves the
 * authenticated clerk's id from the JWT (per-clerk identity for audit).
 *
 * <p>Image bytes are served ONLY from {@code GET /api/claims/{id}/image} here — they are never
 * exposed through the agent's MCP tunnel.
 */
@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private static final Logger log = LoggerFactory.getLogger(ClaimController.class);

    private final ClaimStore store;

    public ClaimController(ClaimStore store) {
        this.store = store;
    }

    /** Filtered, paged list. e.g. {@code /api/claims?status=denied&payer=...&q=smith&page=0&size=20}. */
    @GetMapping
    public Page<ClaimSummary> list(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) String payer,
            @RequestParam(name = "member_id", required = false) String memberId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @CurrentUser UUID clerkId) {
        return store.search(new ClaimFilter(status, payer, memberId, q), pageable);
    }

    @GetMapping("/{claimId}")
    public ResponseEntity<ClaimDetail> detail(@PathVariable String claimId, @CurrentUser UUID clerkId) {
        return store.findDetail(claimId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Ingest a parsed claim, optionally with its scanned image. Multipart: a JSON {@code claim}
     * part plus an optional {@code image} file part.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClaimDetail> ingest(
            @RequestPart("claim") IngestClaimRequest claim,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @CurrentUser UUID clerkId) throws IOException {

        byte[] bytes = (image != null && !image.isEmpty()) ? image.getBytes() : null;
        String contentType = image != null ? image.getContentType() : null;
        String filename = image != null ? image.getOriginalFilename() : null;

        ClaimDetail saved = store.ingest(claim, bytes, contentType, filename);
        log.info("clerk {} ingested claim {} (image={})", clerkId, saved.claimId(), bytes != null);
        return ResponseEntity.status(201).body(saved);
    }

    /** Stream the scanned document image from Postgres. */
    @GetMapping("/{claimId}/image")
    public ResponseEntity<byte[]> image(@PathVariable String claimId, @CurrentUser UUID clerkId) {
        return store.findImage(claimId)
                .map(blob -> ResponseEntity.ok()
                        .contentType(parseType(blob.contentType()))
                        .body(blob.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static MediaType parseType(String contentType) {
        try {
            return contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
