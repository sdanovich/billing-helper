package com.example.mcpaws;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * MCP server that exposes AWS S3 operations (against LocalStack) as MCP tools.
 *
 * <p>The Spring AI MCP server auto-configuration discovers every {@link ToolCallbackProvider}
 * bean and publishes its tools over the SSE transport (default endpoint {@code /sse}).
 */
@SpringBootApplication
public class McpAwsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpAwsServerApplication.class, args);
    }

    /**
     * Publishes the {@code @Tool}-annotated methods of {@link S3Tools} to the MCP server.
     */
    @Bean
    public ToolCallbackProvider s3ToolCallbacks(S3Tools s3Tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(s3Tools)
                .build();
    }
}
