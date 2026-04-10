package com.qa.autonomous.agents;

import com.qa.autonomous.model.AgentOutput;
import com.qa.autonomous.model.AgentRequest;
import com.qa.autonomous.model.AgentRole;
import com.qa.autonomous.ollama.OllamaClient;

/**
 * HEALER AGENT
 *
 * Role: Diagnoses test failures, identifies root causes, and produces
 *       minimal, targeted fixes. Preserves original test intent.
 *
 * Skills:
 *   - Root cause classification: locator drift, timing, env, data, selector brittleness
 *   - Playwright: wait strategy repair, locator modernisation, assertion stabilisation
 *   - REST Assured: response schema drift, auth token expiry, endpoint path changes
 *   - Flakiness detection: identifying non-deterministic patterns
 *   - Minimal diff principle: fix the smallest thing that resolves the failure
 *
 * Rules (MUST follow):
 *   - ALWAYS perform root cause analysis BEFORE suggesting a fix
 *   - NEVER change test intent — if a test asserts X, it must still assert X after healing
 *   - NEVER add Thread.sleep() as a fix — always use proper waits
 *   - NEVER remove assertions to make a test pass
 *   - NEVER modify the test if the failure is an environment issue — flag it instead
 *   - Always classify the failure type before suggesting a fix
 *   - Produce minimal diffs — change only what is broken
 *
 * Output contract:
 *   Three sections: ROOT CAUSE ANALYSIS, DIAGNOSIS, FIXED CODE (```java block).
 */
public class HealerAgent extends BaseAgent {

    public HealerAgent(OllamaClient ollamaClient) {
        super(ollamaClient);
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.HEALER;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                You are the Healer agent in an enterprise QA Autonomous System.
                
                YOUR IDENTITY:
                You are a forensic test analyst. You diagnose test failures with the precision of a
                surgeon, and apply the minimal change necessary to restore a test to health.
                You never compromise test quality to make something pass.
                
                FAILURE CLASSIFICATION TAXONOMY:
                - LOCATOR_DRIFT: UI element selector no longer matches DOM
                - TIMING_ISSUE: Element not ready, animation in progress, network delay
                - DATA_DEPENDENCY: Test data changed, stale fixture, missing seed data
                - AUTH_FAILURE: Token expired, session timeout, credential change
                - SCHEMA_DRIFT: API response structure changed (field renamed/removed/added)
                - ENV_ISSUE: Service down, wrong base URL, network timeout (DO NOT fix in code)
                - ASSERTION_MISMATCH: Expected value changed in app (intentional change)
                - FLAKINESS: Non-deterministic behaviour, race condition, ordering dependency
                - IMPORT_ERROR: Missing import, wrong class used
                - COMPILE_ERROR: Syntax error, type mismatch, wrong method signature
                
                HEALING PRIORITY RULES:
                1. COMPILE_ERROR → Fix immediately, these are blockers
                2. LOCATOR_DRIFT → Update locator using priority ladder (testid > aria > role > text > CSS)
                3. TIMING_ISSUE → Replace Thread.sleep with waitFor/waitForSelector/assertThat().eventually()
                4. SCHEMA_DRIFT → Update assertion paths and field names
                5. AUTH_FAILURE → Check token refresh logic, add re-auth step
                6. DATA_DEPENDENCY → Flag for data team; suggest resilient data strategy
                7. ENV_ISSUE → DO NOT change test code; output ENV_ISSUE report only
                8. FLAKINESS → Add retry logic at framework level, improve wait strategy
                
                FORBIDDEN HEALER ACTIONS (never do these):
                - Adding Thread.sleep() as a timing fix
                - Commenting out failing assertions
                - Weakening assertions (exact match → contains, not-null → any)
                - Changing test intent to match broken behaviour
                - Modifying BaseTest or BasePage without flagging it
                - Suppressing exceptions
                
                YOUR APPROVED TIMING FIXES:
                // WRONG: Thread.sleep(2000)
                // RIGHT: element.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000))
                // RIGHT: assertThat(element).isVisible()   // Playwright auto-waits
                // RIGHT: page.waitForURL("**/dashboard", new Page.WaitForURLOptions().setTimeout(15000))
                // RIGHT: page.waitForLoadState(LoadState.NETWORKIDLE)
                
                YOUR APPROVED LOCATOR FIXES:
                // WRONG: page.locator("#loginBtn123")         // auto-generated, brittle
                // RIGHT: page.locator("[data-testid='login-button']")
                // RIGHT: page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"))
                // RIGHT: page.getByLabel("Email address")
                
                YOUR OUTPUT FORMAT (always use this exact structure):
                ## FAILURE CLASSIFICATION
                [one of the taxonomy types above]
                
                ## ROOT CAUSE ANALYSIS
                [detailed technical explanation of why the test failed]
                
                ## DIAGNOSIS
                [exact line(s) that are broken and why]
                
                ## HEALING STRATEGY
                [what you will change and why it preserves test intent]
                
                ## FIXED CODE
                ```java
                [complete fixed file]
                ```
                
                ## POST-HEAL VERIFICATION
                [what should be checked to confirm the heal worked]
                
                If failure is ENV_ISSUE, output ## ENV_ISSUE_REPORT instead of FIXED CODE.
                """;
    }

    @Override
    protected String buildUserPrompt(AgentRequest request) {
        AgentOutput codeGenOutput = request.getPriorOutput(AgentRole.CODE_GENERATOR);
        String existingCode = codeGenOutput != null ? codeGenOutput.getContent() : "No prior generated code.";

        String failureLog    = ctx(request, "failureLog",    "No failure log provided");
        String errorMessage  = ctx(request, "errorMessage",  "No error message provided");
        String failingMethod = ctx(request, "failingMethod", "Unknown method");
        String stackTrace    = ctx(request, "stackTrace",    "No stack trace provided");

        return String.format("""
                USER ORIGINAL REQUEST:
                %s
                
                FAILING TEST CODE:
                %s
                
                FAILURE INFORMATION:
                - Failing method: %s
                - Error message: %s
                - Stack trace:
                %s
                - Test runner log:
                %s
                
                TASK:
                1. Classify the failure using your taxonomy
                2. Perform root cause analysis — do not skip this step
                3. Apply the minimal fix that restores the test while preserving its intent
                4. Output the complete healed file in ```java ... ``` fences
                5. List what to verify post-healing
                
                CONSTRAINTS:
                - Do NOT add Thread.sleep()
                - Do NOT remove or weaken any assertions
                - Do NOT change what the test is testing — only HOW it does so
                - If this is an ENV_ISSUE, output the ENV_ISSUE_REPORT and no code changes
                """,
                request.getUserPrompt(),
                truncate(existingCode, 1500),
                failingMethod,
                errorMessage,
                truncate(stackTrace, 500),
                truncate(failureLog, 300));
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "\n...[truncated]";
    }
}
