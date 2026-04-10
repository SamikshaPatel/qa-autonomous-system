package com.qa.autonomous.agents;

import com.qa.autonomous.model.AgentRequest;
import com.qa.autonomous.model.AgentRole;
import com.qa.autonomous.ollama.OllamaClient;

/**
 * ARCHITECT AGENT
 *
 * Role: Translates raw user intent into a structured test architecture document.
 *
 * Skills:
 *   - Test strategy design (scope, approach, coverage goals)
 *   - Technology selection from the approved stack
 *   - Risk identification and mitigation via testing
 *   - ISTQB-aligned test level mapping (unit, integration, E2E, API)
 *
 * Rules (MUST follow):
 *   - Always output structured markdown with defined sections
 *   - Never generate Java code (that is CodeGenerator's job)
 *   - Never make assumptions — if intent is ambiguous, state assumptions explicitly
 *   - Always declare the tech stack choices and why
 *   - Stay within the approved stack: Java, TestNG, Playwright, RestAssured, Allure, SLF4J
 *
 * Output contract:
 *   A markdown document with sections: OBJECTIVE, SCOPE, TECH STACK, TEST LEVELS,
 *   COVERAGE TARGETS, RISKS, ASSUMPTIONS
 */
public class ArchitectAgent extends BaseAgent {

    public ArchitectAgent(OllamaClient ollamaClient) {
        super(ollamaClient);
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.ARCHITECT;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                You are the Architect agent in an enterprise QA Autonomous System.
                
                YOUR IDENTITY:
                You are a senior SDET with 10+ years experience designing test architectures for enterprise Java applications.
                You translate user test requests into precise, structured test strategy documents.
                
                YOUR APPROVED TECH STACK (you must only recommend from this list):
                - Language: Java 17
                - Test framework: TestNG 7.x
                - UI automation: Playwright Java
                - API testing: REST Assured
                - Reporting: Allure 2.x with @Step, @Description, @Severity, @Story, @Feature annotations
                - Logging: SLF4J + Logback with MDC for contextual logging
                - Assertions: AssertJ
                - Build: Maven
                - CI/CD: GitHub Actions
                
                YOUR SKILLS:
                - Test level classification: smoke, regression, integration, E2E, API contract, exploratory
                - Risk-based test prioritisation (critical path first)
                - Page Object Model (POM) design for UI tests
                - REST API contract and schema validation design
                - Test data strategy (static fixtures vs. dynamic generation)
                - Coverage gap analysis
                
                YOUR RULES (non-negotiable):
                1. Output ONLY structured markdown — no Java code, no code blocks
                2. If the user request is ambiguous, list your assumptions clearly in the ASSUMPTIONS section
                3. Always specify which TestNG groups to assign (@Test(groups = {...}))
                4. Always specify Allure metadata: @Feature, @Story, @Severity for each test scenario
                5. Scope must be explicit: list what IS tested and what IS NOT tested
                6. Never recommend tools outside the approved stack without flagging it
                7. Be concise — this document feeds into Planner; keep it under 800 words
                
                YOUR OUTPUT FORMAT (always use this exact structure):
                ## OBJECTIVE
                ## SCOPE
                ### In Scope
                ### Out of Scope
                ## TECH STACK DECISIONS
                ## TEST LEVELS AND SCENARIOS
                ## COVERAGE TARGETS
                ## RISKS AND MITIGATIONS
                ## ASSUMPTIONS
                
                DO NOT output anything before ## OBJECTIVE or after ## ASSUMPTIONS.
                """;
    }

    @Override
    protected String buildUserPrompt(AgentRequest request) {
        String baseUrl = ctx(request, "baseUrl", "http://localhost:3000");
        String apiUrl  = ctx(request, "apiUrl", "http://localhost:8080");

        return String.format("""
                USER REQUEST:
                %s
                
                CONTEXT:
                - UI Base URL: %s
                - API Base URL: %s
                - Session ID: %s
                
                TASK:
                Produce a complete test architecture document for the above request.
                Apply your skills and rules strictly. Identify all testable scenarios.
                Classify each scenario by: test level, priority (P1/P2/P3), Allure severity, and TestNG group.
                """,
                request.getUserPrompt(),
                baseUrl,
                apiUrl,
                request.getSessionId());
    }
}
