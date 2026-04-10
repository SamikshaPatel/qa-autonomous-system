package com.qa.autonomous.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable context object passed between the orchestrator and every agent.
 * Carries the original user intent, accumulated knowledge from prior agents,
 * and metadata for traceability.
 */
@Data
@Builder
public class AgentRequest {

    /** Unique ID for the entire orchestration session. */
    @Builder.Default
    private final String sessionId = UUID.randomUUID().toString();

    /** The raw prompt submitted by the user. */
    private final String userPrompt;

    /** The specific task this agent must perform. */
    private final String agentTask;

    /** Role of the agent being invoked. */
    private final AgentRole targetAgent;

    /** Outputs from all previously executed agents, keyed by AgentRole. */
    @Builder.Default
    private final List<AgentOutput> priorOutputs = new ArrayList<>();

    /** Additional key-value context (e.g., baseUrl, browser type, test class name). */
    private final Map<String, String> context;

    /** Timestamp when this request was created. */
    @Builder.Default
    private final Instant createdAt = Instant.now();

    /** Maximum tokens to request from Ollama for this agent invocation. */
    @Builder.Default
    private final int maxTokens = 4096;

    /**
     * Returns the output of a specific prior agent, or null if not yet run.
     */
    public AgentOutput getPriorOutput(AgentRole role) {
        return priorOutputs.stream()
                .filter(o -> o.getRole() == role)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns concatenated summaries from all prior agents for LLM context injection.
     */
    public String buildPriorContext() {
        if (priorOutputs.isEmpty()) {
            return "No prior agent outputs available.";
        }
        StringBuilder sb = new StringBuilder();
        for (AgentOutput output : priorOutputs) {
            sb.append("=== ").append(output.getRole().getDisplayName()).append(" Output ===\n");
            sb.append(output.getContent()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
