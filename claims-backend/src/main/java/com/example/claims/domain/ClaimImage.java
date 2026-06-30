package com.example.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Bytes of a scanned claim document, stored in Postgres as {@code bytea}.
 *
 * <p>Deliberately a separate table from {@code claims}: image bytes are only ever read
 * through the authenticated {@code GET /api/claims/{id}/image} endpoint, never selected by
 * the structured-claims read path the agent uses over the MCP tunnel.
 *
 * <p>Plain {@code byte[]} (no {@code @Lob}) maps to Postgres {@code bytea} under Hibernate 6,
 * not a large-object OID.
 */
@Entity
@Table(name = "claim_images")
public class ClaimImage {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "content", nullable = false)
    private byte[] content;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "filename")
    private String filename;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ClaimImage() {
    }

    public ClaimImage(byte[] content, String contentType, String filename) {
        this.content = content;
        this.contentType = contentType;
        this.filename = filename;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
