package com.example.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the interactive CLI agent.
 *
 * <p>The app is a non-web Spring Boot application (see {@code spring.main.web-application-type=none})
 * whose {@link AgentRunner} drives a read-eval-print loop against Claude, with tools provided
 * by the MCP server over SSE.
 */
@SpringBootApplication
public class AgentCliApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentCliApplication.class, args);
    }
}
