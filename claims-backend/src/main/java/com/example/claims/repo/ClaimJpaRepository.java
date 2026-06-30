package com.example.claims.repo;

import com.example.claims.domain.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data access for {@link Claim}. Kept behind the {@code ClaimStore} interface.
 *
 * <p>Filtering uses {@link JpaSpecificationExecutor} (Criteria API) rather than a JPQL query
 * with nullable parameters: that way an absent filter contributes no predicate, avoiding
 * Postgres's "could not determine data type / lower(bytea)" failures when a null is bound.
 */
public interface ClaimJpaRepository extends JpaRepository<Claim, String>, JpaSpecificationExecutor<Claim> {
}
