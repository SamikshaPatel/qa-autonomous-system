package com.qa.autonomous.model;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * The complete result of a full multi-agent orchestration run.
 * Aggregates all agent outputs, final artifacts, and execution metrics.
 */
@Data
@Builder
public class OrchestratorResult {

    /** Session ID shared across all agents in this run. */
    private final String sessionId;

    /** The original user prompt that triggered this run. */
    private final String userPrompt;

    /** Ordered list of outputs from each agent that was invoked. */
    private final List<AgentOutput> agentOutputs;

    /** Final generated test code (from CodeGenerator), if applicable. */
    private final String generatedTestCode;

    /** Review notes (from Reviewer agent), if applicable. */
    private final String reviewNotes;

    /** Whether the overall orchestration succeeded. */
    private final boolean success;

    /** Error summary if orchestration failed partway through. */
    private final String failureReason;

    /** Total wall-clock time for the entire orchestration. */
    private final Duration totalExecutionTime;

    /** Timestamp when the orchestration started. */
    private final Instant startedAt;

    /** Total tokens consumed across all agents. */
    public int getTotalTokensUsed() {
        if (agentOutputs == null) return 0;
        return agentOutputs.stream().mapToInt(AgentOutput::getTotalTokens).sum();
    }

    /**
     * Returns a formatted execution summary for Allure and logs.
     */
    public String toExecutionSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ORCHESTRATION SUMMARY ===\n");
        sb.append("Session : ").append(sessionId).append("\n");
        sb.append("Status  : ").append(success ? "SUCCESS" : "FAILED").append("\n");
        sb.append("Duration: ").append(totalExecutionTime != null ? totalExecutionTime.toSeconds() : "?").append("s\n");
        sb.append("Tokens  : ").append(getTotalTokensUsed()).append("\n\n");

        if (agentOutputs != null) {
            sb.append("--- Agent Execution Log ---\n");
            for (AgentOutput o : agentOutputs) {
                sb.append("  ").append(o.toSummary()).append("\n");
            }
        }

        if (!success && failureReason != null) {
            sb.append("\nFailure Reason: ").append(failureReason).append("\n");
        }
        return sb.toString();
    }
}
