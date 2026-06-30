package com.example.claims.repo;

import com.example.claims.domain.ClaimImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data access for scanned image blobs. */
public interface ClaimImageJpaRepository extends JpaRepository<ClaimImage, UUID> {
}
