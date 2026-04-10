package com.qa.autonomous.ollama;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * Typed response wrapper from OllamaClient.
 * Carries the LLM content, token usage, and execution metadata.
 */
@Data
@Builder
public class OllamaResponse {

    private final String content;
    private final boolean success;
    private final String errorMessage;
    private final int promptTokens;
    private final int completionTokens;
    private final Duration executionTime;

    public int getTotalTokens() {
        return promptTokens + completionTokens;
    }

    public static OllamaResponse failure(String errorMessage) {
        return OllamaResponse.builder()
                .content("")
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
