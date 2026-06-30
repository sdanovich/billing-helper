package com.example.mcpaws;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Builds an S3 client wired to LocalStack.
 *
 * <p>LocalStack accepts any non-empty credentials and requires path-style addressing
 * (it does not resolve virtual-host {@code bucket.localhost} style URLs by default).
 */
@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client(
            @Value("${aws.endpoint:http://localhost:4566}") String endpoint,
            @Value("${aws.region:us-east-1}") String region,
            @Value("${aws.access-key:test}") String accessKey,
            @Value("${aws.secret-key:test}") String secretKey) {

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
