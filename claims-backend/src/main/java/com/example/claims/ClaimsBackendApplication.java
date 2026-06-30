package com.example.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * App #2 data-side backend: the HTTP API the clerk's Android app talks to.
 *
 * <p>Owns the claims system of record (Postgres) and the scanned-document image blobs
 * (also Postgres, in a separate {@code claim_images} table so the agent's read-only MCP
 * path never selects image bytes). This is a separate door from the agent's MCP tunnel.
 *
 * <p>Entity/repository scan is widened to include the platform-stack login module so its
 * {@code users}/{@code refresh_tokens} entities and repositories are picked up; auth wiring
 * itself loads via the module autoconfigurations (backend-auth + backend-login).
 */
@SpringBootApplication
@EntityScan(basePackages = {"com.example.claims.domain", "com.danovich.platform.login.domain"})
@EnableJpaRepositories(basePackages = {"com.example.claims.repo", "com.danovich.platform.login.repo"})
public class ClaimsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimsBackendApplication.class, args);
    }
}
