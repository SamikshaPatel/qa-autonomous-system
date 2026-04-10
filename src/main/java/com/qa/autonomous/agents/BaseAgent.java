package com.qa.autonomous.agents;

import com.qa.autonomous.model.AgentOutput;
import com.qa.autonomous.model.AgentRequest;
import com.qa.autonomous.model.AgentRole;
import com.qa.autonomous.ollama.OllamaClient;
import com.qa.autonomous.ollama.OllamaResponse;
import com.qa.autonomous.util.CodeExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Abstract base for all QA agents.
 *
 * Contract every concrete agent must fulfil:
 *   1. Declare its role via getRole()
 *   2. Provide a zero-ambiguity system prompt via buildSystemPrompt()
 *   3. Build a focused user prompt via buildUserPrompt(AgentRequest)
 *
 * The base class handles:
 *   - MDC setup for structured logging
 *   - OllamaClient invocation with retry
 *   - Token counting and execution time tracking
 *   - Code extraction from markdown fences
 *   - AgentOutput construction
 */
public abstract class BaseAgent {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final OllamaClient ollamaClient;

    protected BaseAgent(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    /**
     * Returns the role this agent fulfils in the system.
     */
    public abstract AgentRole getRole();

    /**
     * Returns the immutable system prompt that defines this agent's identity,
     * skills, rules, and output format.
     *
     * REQUIREMENTS:
     *   - Must be deterministic (no randomness).
     *   - Must specify output format precisely.
     *   - Must state what the agent WILL NOT do.
     *   - Must be under 1500 tokens to leave headroom for user prompts.
     */
    protected abstract String buildSystemPrompt();

    /**
     * Builds the task-specific user prompt from the inbound AgentRequest.
     * Must incorporate prior agent outputs where relevant.
     */
    protected abstract String buildUserPrompt(AgentRequest request);

    /**
     * Executes the agent: builds prompts, calls Ollama, returns structured output.
     * This method is called by the orchestrator — do not override it.
     */
    public final AgentOutput execute(AgentRequest request) {
        MDC.put("agentName", getRole().getDisplayName());
        MDC.put("sessionId", request.getSessionId());
        Instant start = Instant.now();

        log.info("Agent starting | role={} session={}", getRole().getDisplayName(), request.getSessionId());

        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt   = buildUserPrompt(request);

            log.debug("Agent invoking Ollama | systemPrompt_len={} userPrompt_len={}",
                    systemPrompt.length(), userPrompt.length());

            OllamaResponse response = ollamaClient.generate(systemPrompt, userPrompt, request.getSessionId());
            Duration elapsed = Duration.between(start, Instant.now());

            if (!response.isSuccess()) {
                log.error("Agent failed | role={} error={}", getRole().getDisplayName(), response.getErrorMessage());
                return AgentOutput.failure(getRole(), response.getErrorMessage(), elapsed);
            }

            String content = response.getContent();
            String extractedCode = CodeExtractor.extractFirstJavaBlock(content);

            log.info("Agent completed | role={} tokens={} time={}ms",
                    getRole().getDisplayName(), response.getTotalTokens(), elapsed.toMillis());

            return AgentOutput.builder()
                    .role(getRole())
                    .content(content)
                    .success(true)
                    .promptTokens(response.getPromptTokens())
                    .completionTokens(response.getCompletionTokens())
                    .executionTime(elapsed)
                    .extractedCode(extractedCode)
                    .build();

        } catch (Exception e) {
            Duration elapsed = Duration.between(start, Instant.now());
            log.error("Agent threw unexpected exception | role={} error={}",
                    getRole().getDisplayName(), e.getMessage(), e);
            return AgentOutput.failure(getRole(), "Unexpected error: " + e.getMessage(), elapsed);
        } finally {
            MDC.remove("agentName");
            MDC.remove("sessionId");
        }
    }

    /**
     * Helper: formats prior agent outputs for injection into user prompts.
     * Keeps context concise to minimise token waste.
     */
    protected String formatPriorContext(AgentRequest request) {
        return request.buildPriorContext();
    }

    /**
     * Helper: safely reads a context value with a fallback.
     */
    protected String ctx(AgentRequest request, String key, String fallback) {
        if (request.getContext() == null) return fallback;
        return request.getContext().getOrDefault(key, fallback);
    }
}
