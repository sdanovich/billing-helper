package com.example.mcpaws;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * S3 operations exposed to the agent as MCP tools.
 *
 * <p>Each {@code @Tool} method becomes a callable tool; the description and parameter
 * descriptions are sent to the model so it knows when and how to invoke them.
 */
@Component
public class S3Tools {

    private static final Logger log = LoggerFactory.getLogger(S3Tools.class);

    private final S3Client s3;

    public S3Tools(S3Client s3) {
        this.s3 = s3;
    }

    @Tool(description = "List the names of all S3 buckets available in the environment.")
    public List<String> listBuckets() {
        log.info("tool:listBuckets");
        return s3.listBuckets().buckets().stream()
                .map(b -> b.name())
                .collect(Collectors.toList());
    }

    @Tool(description = "List the object keys stored in a given S3 bucket.")
    public List<String> listObjects(
            @ToolParam(description = "Name of the S3 bucket to list objects from") String bucket) {
        log.info("tool:listObjects bucket={}", bucket);
        try {
            return s3.listObjectsV2(r -> r.bucket(bucket)).contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (NoSuchBucketException e) {
            return List.of("ERROR: bucket '" + bucket + "' does not exist");
        }
    }

    @Tool(description = "Read the text content of a single object in an S3 bucket. "
            + "Use only for small UTF-8 text objects.")
    public String getObjectAsText(
            @ToolParam(description = "Name of the S3 bucket") String bucket,
            @ToolParam(description = "Key (path) of the object within the bucket") String key) {
        log.info("tool:getObjectAsText bucket={} key={}", bucket, key);
        try {
            ResponseBytes<GetObjectResponse> bytes = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return bytes.asString(StandardCharsets.UTF_8);
        } catch (NoSuchKeyException e) {
            return "ERROR: key '" + key + "' not found in bucket '" + bucket + "'";
        } catch (NoSuchBucketException e) {
            return "ERROR: bucket '" + bucket + "' does not exist";
        }
    }

    @Tool(description = "Write (create or overwrite) a text object in an S3 bucket and return a confirmation.")
    public String putObject(
            @ToolParam(description = "Name of the S3 bucket") String bucket,
            @ToolParam(description = "Key (path) of the object within the bucket") String key,
            @ToolParam(description = "UTF-8 text content to store") String content) {
        log.info("tool:putObject bucket={} key={} bytes={}", bucket, key, content.length());
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
                RequestBody.fromString(content, StandardCharsets.UTF_8));
        return "Stored " + content.getBytes(StandardCharsets.UTF_8).length
                + " bytes at s3://" + bucket + "/" + key;
    }
}
