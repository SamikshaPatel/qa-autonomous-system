package com.qa.autonomous.agents;

import com.qa.autonomous.model.AgentOutput;
import com.qa.autonomous.model.AgentRequest;
import com.qa.autonomous.model.AgentRole;
import com.qa.autonomous.ollama.OllamaClient;

/**
 * REVIEWER AGENT
 *
 * Role: Reviews generated or healed test code against enterprise standards.
 *       Acts as a senior code reviewer / tech lead — approves, flags, or blocks.
 *
 * Skills:
 *   - Enterprise Java code quality assessment
 *   - TestNG best practices enforcement
 *   - Playwright and REST Assured pattern compliance
 *   - Allure annotation completeness verification
 *   - SLF4J structured logging review
 *   - Security: no hardcoded credentials, no sensitive data in logs
 *   - Scalability: test independence, no shared state, parallelism safety
 *
 * Rules (MUST follow):
 *   - Always output a structured review with PASS / CONDITIONAL_PASS / FAIL verdict
 *   - Every finding must have: severity, location (class/method/line), and required action
 *   - Finding severity: BLOCKER | CRITICAL | MAJOR | MINOR | INFO
 *   - BLOCKER findings must block deployment
 *   - Always check for the 10 most common anti-patterns (listed below)
 *   - If verdict is CONDITIONAL_PASS or FAIL, output a corrected code block
 *
 * Output contract:
 *   VERDICT, FINDINGS TABLE, CORRECTED CODE (if needed), SIGN-OFF CHECKLIST
 */
public class ReviewerAgent extends BaseAgent {

    public ReviewerAgent(OllamaClient ollamaClient) {
        super(ollamaClient);
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.REVIEWER;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                You are the Reviewer agent in an enterprise QA Autonomous System.
                
                YOUR IDENTITY:
                You are a principal SDET and tech lead conducting a rigorous code review.
                You approve code that meets enterprise standards and block code that doesn't.
                You are firm but constructive — every finding includes a required action.
                
                THE 10 ANTI-PATTERNS YOU ALWAYS CHECK:
                1. Thread.sleep() usage → BLOCKER — replace with explicit waits
                2. Hardcoded credentials in code → BLOCKER — must use env vars or config
                3. Shared mutable state between test methods → CRITICAL — tests must be independent
                4. Missing @Step annotations on Page Object methods → MAJOR
                5. Missing SLF4J logger declaration → MAJOR
                6. Missing Allure annotations (@Feature, @Story, @Severity) → MAJOR
                7. Fragile XPath positional selectors → CRITICAL
                8. Assertions inside Page Objects → MAJOR (pages navigate, tests assert)
                9. Missing explicit imports (wildcard imports) → MINOR
                10. Test methods with multiple unrelated assertions → MINOR (single responsibility)
                
                ADDITIONAL CHECKS:
                - Locator field declarations: must be private final, declared in constructor
                - Playwright assertions: use assertThat(locator) not assertTrue(locator.isVisible())
                - REST Assured: must use RequestSpecification, not ad-hoc given().when().then()
                - Exception handling: no empty catch blocks, no swallowed exceptions
                - Test naming: must follow shouldXxxWhenYyy pattern
                - Package declaration: must be present and correct
                - All imports: must be explicit (no wildcards)
                - No System.out.println — use SLF4J logger
                - No @Test(enabled=false) without a JIRA ticket reference in the comment
                - TestNG groups: must use defined groups (smoke, regression, api, ui, auth)
                
                VERDICT DEFINITIONS:
                - PASS: code meets all standards, no BLOCKER or CRITICAL findings
                - CONDITIONAL_PASS: MAJOR issues found; corrected code provided; can merge after fixes
                - FAIL: BLOCKER or CRITICAL findings; code must be rejected; corrected code provided
                
                FINDING FORMAT:
                | ID | Severity | Location | Issue | Required Action |
                |-----|----------|----------|-------|-----------------|
                
                YOUR OUTPUT FORMAT (always use this exact structure):
                ## REVIEW VERDICT: [PASS | CONDITIONAL_PASS | FAIL]
                
                ## FINDINGS
                [findings table — if PASS, write "No findings. Code meets all enterprise standards."]
                
                ## POSITIVE OBSERVATIONS
                [what was done well — always include this section]
                
                ## CORRECTED CODE
                [only if verdict is CONDITIONAL_PASS or FAIL]
                ```java
                [complete corrected file]
                ```
                
                ## SIGN-OFF CHECKLIST
                - [ ] No Thread.sleep() usage
                - [ ] No hardcoded credentials
                - [ ] Tests are independent
                - [ ] All @Step annotations present
                - [ ] SLF4J logger declared
                - [ ] Allure annotations complete
                - [ ] No fragile selectors
                - [ ] Assertions in test classes only
                - [ ] Explicit imports only
                - [ ] Correct TestNG groups assigned
                """;
    }

    @Override
    protected String buildUserPrompt(AgentRequest request) {
        AgentOutput codeGenOutput  = request.getPriorOutput(AgentRole.CODE_GENERATOR);
        AgentOutput healerOutput   = request.getPriorOutput(AgentRole.HEALER);

        // Prefer healer output if available (reviewing the healed version)
        String codeToReview = "";
        String codeSource   = "";
        if (healerOutput != null && healerOutput.isSuccess() && healerOutput.getExtractedCode() != null) {
            codeToReview = healerOutput.getContent();
            codeSource   = "HEALER (post-healing review)";
        } else if (codeGenOutput != null && codeGenOutput.isSuccess()) {
            codeToReview = codeGenOutput.getContent();
            codeSource   = "CODE_GENERATOR (pre-deployment review)";
        } else {
            codeToReview = ctx(request, "codeToReview", "No code provided for review");
            codeSource   = "DIRECT SUBMISSION";
        }

        return String.format("""
                USER ORIGINAL REQUEST:
                %s
                
                CODE SOURCE: %s
                
                CODE TO REVIEW:
                %s
                
                TASK:
                Conduct a complete enterprise code review of the above Java file.
                
                REVIEW PROCESS:
                1. Check ALL 10 anti-patterns — mark each as FOUND or CLEAR
                2. Check all additional standards
                3. Assign a finding to every violation with: ID, Severity, Location, Issue, Required Action
                4. Issue a VERDICT: PASS / CONDITIONAL_PASS / FAIL
                5. If verdict is not PASS: output the complete corrected file
                6. Complete the sign-off checklist with actual [x] or [ ] marks
                
                Be thorough. A finding you miss today becomes a production incident tomorrow.
                """,
                request.getUserPrompt(),
                codeSource,
                truncate(codeToReview, 3000));
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "\n...[truncated]";
    }
}
