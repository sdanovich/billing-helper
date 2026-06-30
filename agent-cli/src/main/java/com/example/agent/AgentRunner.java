package com.example.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Interactive agent loop.
 *
 * <p>Each user line is sent to Claude with the MCP-provided tools attached. Spring AI runs the
 * full tool-call loop internally (model asks to call a tool, the framework invokes it against the
 * MCP server, feeds the result back) and returns the final natural-language answer.
 */
@Component
public class AgentRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentRunner.class);

    private static final String SYSTEM_PROMPT = """
            You are an AWS operations assistant for a proof-of-concept.
            You can inspect and modify objects in S3 (running on LocalStack) using the provided tools.
            Prefer calling a tool over guessing. When you list or read data, summarize it clearly.
            If a tool returns an error string, report it honestly instead of inventing a result.
            """;

    private final ChatClient chatClient;
    private final List<Message> history = new ArrayList<>();

    public AgentRunner(ChatClient.Builder chatClientBuilder, List<ToolCallbackProvider> toolProviders) {
        ToolCallback[] tools = toolProviders.stream()
                .flatMap(p -> Arrays.stream(p.getToolCallbacks()))
                .toArray(ToolCallback[]::new);

        log.info("Connected MCP tools: {}",
                Arrays.stream(tools).map(t -> t.getToolDefinition().name()).toList());

        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(tools)
                .build();
    }

    @Override
    public void run(String... args) {
        System.out.println();
        System.out.println("=== AI Agent PoC (Claude + MCP + LocalStack S3) ===");
        System.out.println("Ask things like:  what buckets exist?  |  list objects in poc-bucket");
        System.out.println("                  read notes/welcome.txt from poc-bucket");
        System.out.println("                  put 'hello' into poc-bucket at notes/hello.txt");
        System.out.println("Type 'exit' or 'quit' to leave.");
        System.out.println();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("you> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }
                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    break;
                }

                try {
                    history.add(new UserMessage(input));
                    String answer = chatClient.prompt()
                            .messages(history)
                            .call()
                            .content();
                    history.add(new AssistantMessage(answer));
                    System.out.println("agent> " + answer);
                    System.out.println();
                } catch (Exception e) {
                    log.error("Request failed", e);
                    System.out.println("agent> [error] " + e.getMessage());
                    System.out.println();
                }
            }
        }
        System.out.println("Bye.");
    }
}
