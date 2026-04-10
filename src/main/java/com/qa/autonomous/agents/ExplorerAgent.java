package com.qa.autonomous.agents;

import com.qa.autonomous.model.AgentOutput;
import com.qa.autonomous.model.AgentRequest;
import com.qa.autonomous.model.AgentRole;
import com.qa.autonomous.ollama.OllamaClient;

/**
 * EXPLORER AGENT
 *
 * Role: Gathers intelligence about the target system to provide concrete
 *       locators, endpoints, and element metadata to the CodeGenerator.
 *
 * Skills:
 *   - UI exploration: identifying stable locators (data-testid, aria-label, role)
 *   - API exploration: cataloging endpoints, methods, schemas, auth patterns
 *   - Locator priority strategy: data-testid > aria-label > role > text > CSS > XPath
 *   - Response schema analysis for API tests
 *   - Auth flow identification (JWT, session, API key)
 *
 * Rules (MUST follow):
 *   - Output locator intelligence in structured format CodeGenerator can consume directly
 *   - Never recommend fragile locators (positional XPath, auto-generated IDs)
 *   - Flag if a locator is potentially unstable
 *   - For APIs: always specify content-type, auth headers, expected status codes
 *   - Document all assumptions when real system access is not available
 *
 * Output contract:
 *   Two sections: UI ELEMENT CATALOG (if UI test) and API ENDPOINT CATALOG (if API test).
 *   Each entry has exact values the CodeGenerator will use verbatim.
 */
public class ExplorerAgent extends BaseAgent {

    public ExplorerAgent(OllamaClient ollamaClient) {
        super(ollamaClient);
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.EXPLORER;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                You are the Explorer agent in an enterprise QA Autonomous System.
                
                YOUR IDENTITY:
                You are a specialist in test intelligence gathering. You produce precise, ready-to-use
                locator and endpoint catalogs that eliminate guesswork for the code generator.
                
                LOCATOR PRIORITY LADDER (use in this order, stop at first viable option):
                1. data-testid attribute   → page.locator("[data-testid='login-button']")
                2. aria-label attribute    → page.locator("[aria-label='Submit form']")
                3. ARIA role + name        → page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("Login"))
                4. Visible text (exact)    → page.getByText("Sign in", new GetByTextOptions().setExact(true))
                5. Label text             → page.getByLabel("Email address")
                6. CSS selector (stable)  → page.locator("input[name='email']")
                7. XPath (last resort)    → flag as UNSTABLE if used
                
                API CATALOG FORMAT (for each endpoint):
                - METHOD: GET/POST/PUT/DELETE/PATCH
                - PATH: exact path with path params in {braces}
                - AUTH: Bearer/ApiKey/None/Session
                - REQUEST_BODY: JSON schema (key: type, required/optional)
                - HAPPY_PATH_STATUS: 200/201/etc.
                - ERROR_STATUSES: 400 (validation), 401 (auth), 403 (forbidden), 404 (not found), 422 (unprocessable)
                - RESPONSE_SCHEMA: key fields to assert
                
                UI CATALOG FORMAT (for each element):
                - ELEMENT_ID: unique ID you assign (e.g., LOGIN_EMAIL_INPUT)
                - DESCRIPTION: human-readable description
                - LOCATOR_TYPE: TESTID / ARIA_LABEL / ROLE / TEXT / LABEL / CSS / XPATH
                - LOCATOR_VALUE: exact Playwright Java expression
                - STABILITY: STABLE / POTENTIALLY_UNSTABLE / UNSTABLE
                - ACTIONS: CLICK / FILL / ASSERT_VISIBLE / ASSERT_TEXT / SELECT
                
                YOUR RULES (non-negotiable):
                1. Never invent locators you cannot justify — flag assumptions clearly
                2. Always provide the Playwright Java expression, not pseudocode
                3. For REST Assured: provide the exact Java path expression for JSON assertions
                4. Mark any element as UNSTABLE if it uses positional selectors
                5. Include both positive and negative test elements (error messages, validation states)
                6. For auth flows: document the complete token acquisition sequence
                
                YOUR OUTPUT FORMAT:
                ## EXPLORATION SUMMARY
                ## UI ELEMENT CATALOG
                [element entries]
                ## API ENDPOINT CATALOG
                [endpoint entries]
                ## AUTH FLOW
                ## ASSUMPTIONS AND UNKNOWNS
                """;
    }

    @Override
    protected String buildUserPrompt(AgentRequest request) {
        AgentOutput plannerOutput = request.getPriorOutput(AgentRole.PLANNER);
        AgentOutput architectOutput = request.getPriorOutput(AgentRole.ARCHITECT);

        String planContext = plannerOutput != null ? plannerOutput.getContent() : "No plan available.";
        String archContext = architectOutput != null ? architectOutput.getContent() : "No architecture available.";

        String baseUrl = ctx(request, "baseUrl", "http://localhost:3000");
        String apiUrl  = ctx(request, "apiUrl", "http://localhost:8080");

        return String.format("""
                USER ORIGINAL REQUEST:
                %s
                
                ARCHITECT DOCUMENT:
                %s
                
                PLANNER DOCUMENT:
                %s
                
                TARGET SYSTEM:
                - UI URL: %s
                - API URL: %s
                
                TASK:
                Produce a complete element and endpoint catalog for the test scenarios identified
                by the Architect and Planner.
                
                For EVERY test scenario in the plan:
                1. Identify all UI elements needed (if UI test) using the locator priority ladder
                2. Identify all API endpoints needed (if API test) with full schema documentation
                3. Document the auth flow if authentication is required
                4. Flag any element/endpoint where you are making assumptions
                
                Output must be ready for direct consumption by the CodeGenerator — no vagueness.
                Every locator must be a valid Playwright Java expression.
                Every API entry must include all status codes and schema fields the test will assert.
                """,
                request.getUserPrompt(),
                truncate(archContext, 600),
                truncate(planContext, 600),
                baseUrl,
                apiUrl);
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "\n...[truncated for token efficiency]";
    }
}
