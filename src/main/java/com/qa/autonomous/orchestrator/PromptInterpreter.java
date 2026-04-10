package com.qa.autonomous.orchestrator;

import com.qa.autonomous.ollama.OllamaClient;
import com.qa.autonomous.ollama.OllamaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PromptInterpreter — uses Ollama to classify a raw user prompt into a PipelineMode.
 *
 * This is a lightweight classifier call that uses minimal tokens.
 * It returns a single keyword that maps to a PipelineMode enum value.
 *
 * Keywords the model must return:
 *   FULL | GENERATE | HEAL | REVIEW | EXPLORE | PLAN
 */
public class PromptInterpreter {

    private static final Logger log = LoggerFactory.getLogger(PromptInterpreter.class);

    private final OllamaClient ollamaClient;

    private static final String SYSTEM_PROMPT = """
            You are a routing classifier for a QA test automation system.
            
            Your ONLY job is to read a user prompt and return exactly ONE word from this list:
            FULL | GENERATE | HEAL | REVIEW | EXPLORE | PLAN
            
            ROUTING RULES:
            - FULL:     user wants end-to-end test creation including healing and review
            - GENERATE: user wants tests generated (write tests, create tests, build tests)
            - HEAL:     user mentions a failing/broken/flaky test that needs to be fixed
            - REVIEW:   user wants existing code reviewed for quality
            - EXPLORE:  user wants to understand or map the system (what endpoints, what elements)
            - PLAN:     user wants a test plan, test strategy, or architecture document only
            
            EXAMPLES:
            "Write API tests for the login endpoint" → GENERATE
            "My login test is failing with a NoSuchElementException" → HEAL
            "Review this test class for best practices" → REVIEW
            "What endpoints does the user service have?" → EXPLORE
            "Create a test plan for the checkout feature" → PLAN
            "Build a complete automated test suite for authentication" → FULL
            
            OUTPUT RULE: Return ONLY the single routing word. No explanation. No punctuation.
            """;

    public PromptInterpreter(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    /**
     * Interprets the user prompt and returns the appropriate pipeline mode.
     * Falls back to GENERATE if classification fails.
     */
    public AgentOrchestrator.PipelineMode interpret(String userPrompt, String sessionId) {
        log.debug("Interpreting prompt for routing | session={}", sessionId);

        OllamaResponse response = ollamaClient.generate(SYSTEM_PROMPT, userPrompt, sessionId);

        if (!response.isSuccess()) {
            log.warn("Prompt interpretation failed, defaulting to GENERATE | session={}", sessionId);
            return AgentOrchestrator.PipelineMode.GENERATE;
        }

        String raw = response.getContent().trim().toUpperCase()
                .replaceAll("[^A-Z_]", ""); // strip punctuation

        log.info("Prompt classified as: '{}' | session={}", raw, sessionId);

        try {
            return AgentOrchestrator.PipelineMode.valueOf(raw);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown pipeline mode '{}', falling back to GENERATE | session={}", raw, sessionId);
            return AgentOrchestrator.PipelineMode.GENERATE;
        }
    }
}
