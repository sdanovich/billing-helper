package com.example.claims.store;

import com.example.claims.domain.Claim;
import com.example.claims.domain.ClaimImage;
import com.example.claims.domain.ClaimStatus;
import com.example.claims.dto.ClaimDetail;
import com.example.claims.dto.ClaimFilter;
import com.example.claims.dto.ClaimSummary;
import com.example.claims.dto.IngestClaimRequest;
import com.example.claims.error.DuplicateClaimException;
import com.example.claims.repo.ClaimImageJpaRepository;
import com.example.claims.repo.ClaimJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Postgres-backed {@link ClaimStore}. */
@Service
public class JpaClaimStore implements ClaimStore {

    private final ClaimJpaRepository claims;
    private final ClaimImageJpaRepository images;

    public JpaClaimStore(ClaimJpaRepository claims, ClaimImageJpaRepository images) {
        this.claims = claims;
        this.images = images;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClaimSummary> search(ClaimFilter filter, Pageable pageable) {
        Specification<Claim> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            String payer = blankToNull(filter.payer());
            if (payer != null) {
                predicates.add(cb.equal(cb.lower(root.get("payer")), payer.toLowerCase()));
            }
            String memberId = blankToNull(filter.memberId());
            if (memberId != null) {
                predicates.add(cb.equal(root.get("memberId"), memberId));
            }
            String q = blankToNull(filter.q());
            if (q != null) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("patientName")), like),
                        cb.like(cb.lower(root.get("claimId")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return claims.findAll(spec, pageable)
                .map(c -> new ClaimSummary(
                        c.getClaimId(), c.getPatientName(), c.getMemberId(),
                        c.getPayer(), c.getStatus(), c.getBalance()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClaimDetail> findDetail(String claimId) {
        return claims.findById(claimId).map(ClaimDetail::of);
    }

    @Override
    @Transactional
    public ClaimDetail ingest(IngestClaimRequest req, byte[] imageBytes, String contentType, String filename) {
        validate(req);
        if (claims.existsById(req.claimId())) {
            throw new DuplicateClaimException("claim " + req.claimId() + " already exists");
        }

        UUID imageId = null;
        if (imageBytes != null && imageBytes.length > 0) {
            ClaimImage saved = images.save(new ClaimImage(imageBytes, contentType, filename));
            imageId = saved.getId();
        }

        BigDecimal billed = orZero(req.billedAmount());
        BigDecimal paid = orZero(req.paidAmount());
        BigDecimal balance = req.balance() != null ? req.balance() : billed.subtract(paid);

        Claim c = new Claim();
        c.setClaimId(req.claimId().trim());
        c.setPatientName(req.patientName().trim());
        c.setMemberId(req.memberId().trim());
        c.setPayer(req.payer().trim());
        c.setCptCode(blankToNull(req.cptCode()));
        c.setIcdCode(blankToNull(req.icdCode()));
        c.setBilledAmount(billed);
        c.setPaidAmount(paid);
        c.setBalance(balance);
        c.setStatus(req.status());
        c.setDenialReason(req.status() == ClaimStatus.denied ? blankToNull(req.denialReason()) : null);
        c.setImageId(imageId);
        c.setCreatedAt(Instant.now());

        return ClaimDetail.of(claims.save(c));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImageBlob> findImage(String claimId) {
        return claims.findById(claimId)
                .map(Claim::getImageId)
                .filter(Objects::nonNull)
                .flatMap(images::findById)
                .map(img -> new ImageBlob(img.getContent(), img.getContentType()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String claimId) {
        return claims.existsById(claimId);
    }

    /** Required-field checks. 400 on failure. */
    private static void validate(IngestClaimRequest r) {
        require(notBlank(r.claimId()), "claimId is required");
        require(notBlank(r.patientName()), "patientName is required");
        require(notBlank(r.memberId()), "memberId is required");
        require(notBlank(r.payer()), "payer is required");
        require(r.status() != null, "status is required");
        if (r.status() == ClaimStatus.denied) {
            require(notBlank(r.denialReason()), "denialReason is required when status=denied");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
