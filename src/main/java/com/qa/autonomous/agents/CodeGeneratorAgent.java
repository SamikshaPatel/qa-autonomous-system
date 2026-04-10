package com.qa.autonomous.agents;

import com.qa.autonomous.model.AgentOutput;
import com.qa.autonomous.model.AgentRequest;
import com.qa.autonomous.model.AgentRole;
import com.qa.autonomous.ollama.OllamaClient;

/**
 * CODE GENERATOR AGENT
 *
 * Role: Produces production-quality, enterprise-grade Java test code
 *       using the exact specifications from Architect, Planner, and Explorer.
 *
 * Skills:
 *   - TestNG test class generation with full lifecycle annotations
 *   - Playwright Java: Page Object Model, stable locators, web-first assertions
 *   - REST Assured: request/response specs, JSON schema validation, filter chains
 *   - Allure: full annotation suite (@Step, @Description, @Feature, @Story, @Severity, @Attachment)
 *   - SLF4J: structured logging with MDC in every generated class
 *   - AssertJ: fluent assertion chains
 *   - Java 17: records, text blocks, var, switch expressions where appropriate
 *
 * Rules (MUST follow):
 *   - NEVER use Thread.sleep() — always use Playwright waitFor* or REST Assured polling
 *   - NEVER use fragile XPath positional selectors
 *   - ALWAYS add @Step to every meaningful action
 *   - ALWAYS add SLF4J logger to every class
 *   - ALWAYS use explicit waits with meaningful timeout messages
 *   - Test methods MUST be independent — no shared mutable state between tests
 *   - Page Object methods return 'this' for fluent chaining where appropriate
 *   - Every class must have a package declaration and all required imports
 *   - Output ONLY valid, compilable Java code in a single ```java block
 *
 * Output contract:
 *   One complete Java file per invocation, wrapped in ```java ... ``` fences.
 */
public class CodeGeneratorAgent extends BaseAgent {

    public CodeGeneratorAgent(OllamaClient ollamaClient) {
        super(ollamaClient);
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.CODE_GENERATOR;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                You are the CodeGenerator agent in an enterprise QA Autonomous System.
                
                YOUR IDENTITY:
                You are a senior SDET who writes flawless, enterprise-grade Java test automation code.
                Your code is used in production CI/CD pipelines and must meet the highest standards.
                
                YOUR APPROVED PATTERNS:
                
                [PLAYWRIGHT PAGE OBJECT PATTERN]
                public class LoginPage extends BasePage {
                    private static final Logger log = LoggerFactory.getLogger(LoginPage.class);
                    private final Locator emailInput;
                    private final Locator passwordInput;
                    private final Locator loginButton;
                    private final Locator errorMessage;
                
                    public LoginPage(Page page) {
                        super(page);
                        this.emailInput   = page.locator("[data-testid='email-input']");
                        this.passwordInput = page.locator("[data-testid='password-input']");
                        this.loginButton  = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"));
                        this.errorMessage = page.locator("[data-testid='error-message']");
                    }
                
                    @Step("Enter email: {email}")
                    public LoginPage enterEmail(String email) {
                        log.debug("Entering email: {}", email);
                        emailInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
                        emailInput.fill(email);
                        return this;
                    }
                
                    @Step("Click Login button")
                    public void clickLogin() {
                        log.debug("Clicking login button");
                        loginButton.click();
                    }
                }
                
                [PLAYWRIGHT TEST CLASS PATTERN]
                @Feature("Authentication")
                public class LoginTest extends BaseTest {
                    private static final Logger log = LoggerFactory.getLogger(LoginTest.class);
                
                    @Test(groups = {"smoke", "auth"}, description = "Verify valid login succeeds")
                    @Story("User Login")
                    @Severity(SeverityLevel.BLOCKER)
                    @Description("Validates that a user with valid credentials can log in and reach the dashboard")
                    public void shouldNavigateToDashboardWhenValidCredentials() {
                        log.info("Test: valid login flow");
                        LoginPage loginPage = new LoginPage(page);
                        loginPage
                            .navigate()
                            .enterEmail("user@example.com")
                            .enterPassword("password123")
                            .clickLogin();
                        DashboardPage dashboard = new DashboardPage(page);
                        dashboard.assertIsLoaded();
                    }
                }
                
                [REST ASSURED API TEST PATTERN]
                @Feature("User API")
                public class UserApiTest extends BaseApiTest {
                    private static final Logger log = LoggerFactory.getLogger(UserApiTest.class);
                
                    @Test(groups = {"api", "smoke"})
                    @Story("Get User")
                    @Severity(SeverityLevel.CRITICAL)
                    @Step("GET /api/users/{id} returns 200 with valid schema")
                    public void shouldReturn200WithUserSchemaWhenValidId() {
                        log.info("Test: GET /api/users/1");
                        given(requestSpec)
                            .pathParam("id", 1)
                        .when()
                            .get("/api/users/{id}")
                        .then()
                            .spec(responseSpec)
                            .statusCode(200)
                            .body("id", equalTo(1))
                            .body("email", matchesPattern("^[\\\\w.-]+@[\\\\w.-]+\\\\.[a-z]{2,}$"));
                    }
                }
                
                YOUR MANDATORY CODING RULES:
                1. NO Thread.sleep() — use Playwright's waitFor* methods or REST Assured await
                2. NO brittle XPath — use data-testid, aria-label, role, text, label, or stable CSS
                3. ALWAYS declare Locators as private final fields in the constructor (not inline)
                4. ALWAYS add @Step to every public Page Object method
                5. ALWAYS add SLF4J logger: private static final Logger log = LoggerFactory.getLogger(X.class)
                6. ALWAYS include package and ALL imports — code must compile without edits
                7. EVERY test method must be fully independent
                8. USE AssertJ for non-Playwright assertions: assertThat(value).isEqualTo(expected)
                9. REST ASSURED: always use RequestSpecification and ResponseSpecification
                10. ALLURE: always add @Feature (class level), @Story, @Severity, @Description (method level)
                
                OUTPUT RULE:
                Output EXACTLY ONE complete Java file.
                Wrap it in ```java ... ``` fences.
                Do not output any text before the opening fence or after the closing fence.
                The code must be 100% compilable as-is.
                """;
    }

    @Override
    protected String buildUserPrompt(AgentRequest request) {
        AgentOutput explorerOutput  = request.getPriorOutput(AgentRole.EXPLORER);
        AgentOutput plannerOutput   = request.getPriorOutput(AgentRole.PLANNER);
        AgentOutput architectOutput = request.getPriorOutput(AgentRole.ARCHITECT);

        String explorerContent  = explorerOutput  != null ? explorerOutput.getContent()  : "No explorer data.";
        String plannerContent   = plannerOutput   != null ? plannerOutput.getContent()   : "No planner data.";
        String architectContent = architectOutput != null ? architectOutput.getContent() : "No architect data.";

        String targetFile    = ctx(request, "targetFile",    "LoginTest.java");
        String targetClass   = ctx(request, "targetClass",   "LoginTest");
        String targetPackage = ctx(request, "targetPackage", "com.qa.autonomous.tests");
        String baseUrl       = ctx(request, "baseUrl",       "http://localhost:3000");
        String apiUrl        = ctx(request, "apiUrl",        "http://localhost:8080");

        return String.format("""
                USER ORIGINAL REQUEST:
                %s
                
                ARCHITECTURE CONTEXT (summarised):
                %s
                
                IMPLEMENTATION PLAN CONTEXT (summarised):
                %s
                
                EXPLORER ELEMENT/ENDPOINT CATALOG:
                %s
                
                CODE GENERATION TARGET:
                - File to generate: %s
                - Class name: %s
                - Package: %s
                - Base UI URL: %s
                - Base API URL: %s
                - Additional task context: %s
                
                TASK:
                Generate the complete Java file for: %s
                
                REQUIREMENTS:
                1. Use the Explorer catalog for ALL locators and endpoint paths — do not guess
                2. Follow every coding rule in your system prompt exactly
                3. Include every import — assume no wildcard imports
                4. Add @Step to every meaningful action method
                5. Add Allure annotations: @Feature (class), @Story + @Severity + @Description (method)
                6. Extend BaseTest (for Playwright tests) or BaseApiTest (for REST Assured tests)
                7. Output ONLY the Java code in ```java ... ``` fences
                """,
                request.getUserPrompt(),
                truncate(architectContent, 400),
                truncate(plannerContent, 400),
                truncate(explorerContent, 800),
                targetFile, targetClass, targetPackage,
                baseUrl, apiUrl,
                request.getAgentTask() != null ? request.getAgentTask() : "Generate as planned",
                targetFile);
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "\n...[truncated]";
    }
}
