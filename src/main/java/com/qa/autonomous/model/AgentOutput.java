package com.qa.autonomous.model;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

/**
 * Structured output produced by a single agent invocation.
 * Captures the full content, execution metadata, and token usage
 * for audit trails and Allure reporting.
 */
@Data
@Builder
public class AgentOutput {

    /** Which agent produced this output. */
    private final AgentRole role;

    /** The full text content returned by the agent. */
    private final String content;

    /** Whether the agent completed successfully. */
    @Builder.Default
    private final boolean success = true;

    /** Error message if the agent failed. */
    private final String errorMessage;

    /** Number of prompt tokens consumed. */
    @Builder.Default
    private final int promptTokens = 0;

    /** Number of completion tokens generated. */
    @Builder.Default
    private final int completionTokens = 0;

    /** Total time taken for this agent invocation. */
    private final Duration executionTime;

    /** Timestamp when this output was produced. */
    @Builder.Default
    private final Instant producedAt = Instant.now();

    /** Optional: extracted code blocks, if any. */
    private final String extractedCode;

    /**
     * Total tokens used (prompt + completion).
     */
    public int getTotalTokens() {
        return promptTokens + completionTokens;
    }

    /**
     * Returns a brief summary suitable for logging.
     */
    public String toSummary() {
        return String.format("[%s] success=%s tokens=%d time=%dms content_length=%d",
                role.getDisplayName(),
                success,
                getTotalTokens(),
                executionTime != null ? executionTime.toMillis() : -1,
                content != null ? content.length() : 0);
    }

    /**
     * Factory method for a failed agent output.
     */
    public static AgentOutput failure(AgentRole role, String errorMessage, Duration executionTime) {
        return AgentOutput.builder()
                .role(role)
                .content("")
                .success(false)
                .errorMessage(errorMessage)
                .executionTime(executionTime)
                .build();
    }
}
