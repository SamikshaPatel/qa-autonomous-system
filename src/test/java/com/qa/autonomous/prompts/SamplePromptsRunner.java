package com.qa.autonomous.prompts;

import com.qa.autonomous.model.OrchestratorResult;
import com.qa.autonomous.orchestrator.AgentOrchestrator;
import io.qameta.allure.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================
 * PHASE 3 — SAMPLE PROMPTS FOR EXPERIMENTATION
 * ============================================================
 *
 * This class contains 12 ready-to-run sample prompts that exercise
 * every agent and pipeline mode in the system.
 *
 * HOW TO RUN (from IntelliJ):
 *   Right-click this class → Run
 *   OR: right-click any individual @Test method → Run
 *
 * HOW TO RUN (from terminal):
 *   mvn test -Dtest=SamplePromptsRunner -Dgroups=sample
 *
 * PREREQUISITES:
 *   1. Ollama installed: https://ollama.com
 *   2. Llama3 pulled: ollama pull llama3
 *   3. Ollama running: ollama serve
 *
 * ============================================================
 * PROMPT CATALOGUE (what each prompt tests):
 * ============================================================
 *
 * PROMPT_01: Full UI test suite for login    → FULL pipeline (all 6 agents)
 * PROMPT_02: REST API test for health check  → GENERATE pipeline (API)
 * PROMPT_03: Test plan only, no code         → PLAN pipeline
 * PROMPT_04: Fix failing test                → HEAL pipeline
 * PROMPT_05: Review test code quality        → REVIEW pipeline
 * PROMPT_06: Explore REST API endpoints      → EXPLORE pipeline
 * PROMPT_07: E2E checkout flow               → FULL pipeline (complex UI)
 * PROMPT_08: Auth API - login endpoint       → GENERATE pipeline (API + auth)
 * PROMPT_09: Dashboard page test             → GENERATE pipeline (UI)
 * PROMPT_10: Fix flaky dropdown test         → HEAL pipeline (timing issue)
 * PROMPT_11: User registration API tests     → GENERATE pipeline (CRUD)
 * PROMPT_12: Full suite for search feature   → FULL pipeline (search UI+API)
 */
@Feature("Sample Prompts — Phase 3 Experimentation")
public class


SamplePromptsRunner {

    private static final Logger log = LoggerFactory.getLogger(SamplePromptsRunner.class);
    private AgentOrchestrator orchestrator;

    @BeforeClass
    public void setUp() {
        orchestrator = new AgentOrchestrator();
        log.info("SamplePromptsRunner initialised — ready for experimentation");
    }

    // ============================================================
    // PROMPT 01 — Full pipeline: Login UI test suite
    // Expected: 6 agents execute, Java test code generated
    // ============================================================
    @Test(groups = {"sample", "sample-full"},
          description = "PROMPT_01: Full UI test suite for a login page")
    @Story("Full Pipeline — Login UI")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Exercises all 6 agents. Generates a full Playwright test suite for login.")
    public void prompt01_fullLoginTestSuite() {
        String prompt = """
                Create a complete automated test suite for a login page.
                The login page has an email field, a password field, and a Login button.
                It should test: successful login, invalid email format, wrong password,
                empty fields validation, and the "Forgot Password" link.
                Use Playwright Java with Page Object Model and full Allure reporting.
                """;

        Map<String, String> context = buildUiContext(
                "http://localhost:3000",
                "LoginTest.java",
                "LoginTest",
                "com.qa.autonomous.tests.ui"
        );

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_01");
    }

    // ============================================================
    // PROMPT 02 — Generate pipeline: Health check API test
    // Expected: CodeGenerator produces REST Assured test
    // ============================================================
    @Test(groups = {"sample", "sample-api"},
          description = "PROMPT_02: REST Assured test for GET /api/health")
    @Story("Generate Pipeline — Health API")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Generates a REST Assured test verifying the health endpoint returns 200 with correct schema.")
    public void prompt02_healthCheckApiTest() {
        String prompt = """
                Write a REST Assured API test for the health check endpoint GET /api/health.
                The endpoint returns: {"status": "UP", "version": "1.0.0", "timestamp": "<ISO datetime>"}
                Test: 200 status code, status field equals "UP", version field present,
                timestamp is a valid ISO date format.
                """;

        Map<String, String> context = buildApiContext(
                "http://localhost:8080",
                "HealthApiTest.java",
                "HealthApiTest",
                "com.qa.autonomous.tests.api"
        );

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_02");
    }

    // ============================================================
    // PROMPT 03 — Plan pipeline: Test plan only, no code
    // Expected: Architect + Planner outputs only
    // ============================================================
    @Test(groups = {"sample", "sample-plan"},
          description = "PROMPT_03: Test plan for a checkout feature")
    @Story("Plan Pipeline — Checkout Feature")
    @Severity(SeverityLevel.NORMAL)
    @Description("Produces a test strategy and implementation plan for checkout. No code generated.")
    public void prompt03_checkoutTestPlan() {
        String prompt = """
                Create a test plan for an e-commerce checkout feature.
                The checkout flow has: cart summary, shipping address form,
                payment details (card number, expiry, CVV), order review, and order confirmation.
                Include: risk analysis, test scenarios at each step, TestNG groups,
                and Allure metadata for each scenario.
                """;

        OrchestratorResult result = orchestrator.orchestrate(prompt, new HashMap<>());
        logAndAssert(result, "PROMPT_03");
    }

    // ============================================================
    // PROMPT 04 — Heal pipeline: Fix a failing locator
    // Expected: Healer diagnoses locator drift, provides fix
    // ============================================================
    @Test(groups = {"sample", "sample-heal"},
          description = "PROMPT_04: Fix a failing login test with locator issue")
    @Story("Heal Pipeline — Locator Drift")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Healer diagnoses and fixes a NoSuchElementException caused by a brittle CSS locator.")
    public void prompt04_healFailingLocator() {
        String prompt = "My login test is failing and I need it fixed.";

        Map<String, String> context = new HashMap<>();
        context.put("failingMethod", "shouldLoginWithValidCredentials");
        context.put("errorMessage", "Timeout 30000ms exceeded waiting for locator('#email-input-v2')");
        context.put("stackTrace",
                "com.microsoft.playwright.TimeoutError: Timeout 30000ms exceeded\n" +
                "    at LoginPage.enterEmail(LoginPage.java:34)\n" +
                "    at LoginTest.shouldLoginWithValidCredentials(LoginTest.java:52)");
        context.put("codeToReview",
                """
                public class LoginPage extends BasePage {
                    private final Locator emailInput = page.locator("#email-input-v2");
                    private final Locator passwordInput = page.locator("#password-input-v2");
                    private final Locator loginButton = page.locator("#btn-login-submit");

                    public LoginPage(Page page) { super(page); }

                    public LoginPage enterEmail(String email) {
                        emailInput.fill(email);  // No wait
                        return this;
                    }
                    protected String getPath() { return "/login"; }
                }
                """);

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_04");
    }

    // ============================================================
    // PROMPT 05 — Review pipeline: Code quality review
    // Expected: Reviewer produces verdict with findings
    // ============================================================
    @Test(groups = {"sample", "sample-review"},
          description = "PROMPT_05: Review a test class for enterprise standards")
    @Story("Review Pipeline — Code Quality")
    @Severity(SeverityLevel.NORMAL)
    @Description("Reviewer checks a submitted test class and outputs PASS/FAIL with findings.")
    public void prompt05_reviewTestClass() {
        String prompt = "Review this test code for enterprise quality standards";

        Map<String, String> context = new HashMap<>();
        context.put("codeToReview",
                """
                package com.qa.tests;
                import com.microsoft.playwright.*;
                import org.testng.annotations.*;
                import static org.testng.Assert.*;

                public class UserTest {
                    Page page;

                    @BeforeMethod
                    public void setup() {
                        Playwright pw = Playwright.create();
                        page = pw.chromium().launch().newPage();
                    }

                    @Test
                    public void testLogin() {
                        page.navigate("http://localhost:3000/login");
                        page.locator("#email").fill("admin");
                        page.locator("#pass").fill("admin123");    // hardcoded credential
                        page.locator("#submit").click();
                        Thread.sleep(3000);                         // Thread.sleep!
                        assertTrue(page.url().contains("dashboard"));
                    }
                }
                """);

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_05");
    }

    // ============================================================
    // PROMPT 06 — Explore pipeline: Map a REST API
    // Expected: Explorer produces endpoint and schema catalog
    // ============================================================
    @Test(groups = {"sample", "sample-explore"},
          description = "PROMPT_06: Explore and catalog a User Management REST API")
    @Story("Explore Pipeline — API Discovery")
    @Severity(SeverityLevel.NORMAL)
    @Description("Explorer catalogs all endpoints, schemas, and auth patterns for a User API.")
    public void prompt06_exploreUserApi() {
        String prompt = """
                Explore and document the User Management REST API at /api/users.
                The API supports: list all users (GET), get by ID (GET), create user (POST),
                update user (PUT), delete user (DELETE).
                Authentication: Bearer token via Authorization header.
                Document all endpoints, request/response schemas, auth flow,
                and all status codes for each endpoint.
                """;

        Map<String, String> context = new HashMap<>();
        context.put("apiUrl", "http://localhost:8080");

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_06");
    }

    // ============================================================
    // PROMPT 07 — Full pipeline: E2E checkout flow
    // Expected: All 6 agents, complex Playwright POM generated
    // ============================================================
    @Test(groups = {"sample", "sample-full"},
          description = "PROMPT_07: Full E2E test for checkout flow")
    @Story("Full Pipeline — Checkout E2E")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Generates a complete Playwright E2E test covering the multi-step checkout flow.")
    public void prompt07_checkoutE2eFlow() {
        String prompt = """
                Build a complete end-to-end Playwright test for the checkout flow.
                Flow: Add item to cart → View cart → Enter shipping address →
                Enter payment details → Review order → Confirm order → Verify confirmation page.
                Use Page Object Model. Test the happy path and one negative path
                (invalid credit card format).
                """;

        Map<String, String> context = buildUiContext(
                "http://localhost:3000",
                "CheckoutE2ETest.java",
                "CheckoutE2ETest",
                "com.qa.autonomous.tests.e2e"
        );

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_07");
    }

    // ============================================================
    // PROMPT 08 — Generate pipeline: Auth API tests
    // Expected: REST Assured tests with Bearer token auth
    // ============================================================
    @Test(groups = {"sample", "sample-api"},
          description = "PROMPT_08: REST Assured tests for POST /api/auth/login")
    @Story("Generate Pipeline — Auth API")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Generates API tests for the login endpoint: valid creds, invalid creds, missing body, lockout.")
    public void prompt08_authApiTests() {
        String prompt = """
                Write REST Assured tests for the authentication API endpoint POST /api/auth/login.
                Request body: {"email": "string", "password": "string"}
                Happy path (200): returns {"token": "JWT_STRING", "expiresIn": 3600, "userId": "UUID"}
                Test scenarios:
                - Valid credentials → 200 with token
                - Wrong password → 401 with {"error": "INVALID_CREDENTIALS"}
                - Non-existent email → 401 with {"error": "INVALID_CREDENTIALS"}
                - Missing email field → 400 with {"error": "VALIDATION_ERROR"}
                - Missing password field → 400 with {"error": "VALIDATION_ERROR"}
                - Empty request body → 400
                """;

        Map<String, String> context = buildApiContext(
                "http://localhost:8080",
                "AuthApiTest.java",
                "AuthApiTest",
                "com.qa.autonomous.tests.api"
        );

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_08");
    }

    // ============================================================
    // PROMPT 09 — Generate pipeline: Dashboard page UI test
    // Expected: Playwright test with POM for dashboard
    // ============================================================
    @Test(groups = {"sample", "sample-ui"},
          description = "PROMPT_09: Playwright tests for the main dashboard page")
    @Story("Generate Pipeline — Dashboard UI")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Generates Playwright tests for dashboard: widgets visible, data loads, navigation works.")
    public void prompt09_dashboardPageTest() {
        String prompt = """
                Create Playwright tests for the main dashboard page (requires login first).
                The dashboard has: a welcome message with user's first name, a stats widget showing
                total orders, revenue, and active users, a recent activity table with 10 rows,
                and a navigation sidebar with links to: Orders, Products, Users, Reports.
                Test: dashboard loads after login, all widgets are visible, stats are numeric,
                activity table has correct columns (Date, Action, User, Status), sidebar links work.
                """;

        Map<String, String> context = buildUiContext(
                "http://localhost:3000",
                "DashboardTest.java",
                "DashboardTest",
                "com.qa.autonomous.tests.ui"
        );

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_09");
    }

    // ============================================================
    // PROMPT 10 — Heal pipeline: Fix flaky dropdown
    // Expected: Healer identifies timing issue, applies waitFor fix
    // ============================================================
    @Test(groups = {"sample", "sample-heal"},
          description = "PROMPT_10: Fix a flaky dropdown test with timing issues")
    @Story("Heal Pipeline — Flakiness Fix")
    @Severity(SeverityLevel.NORMAL)
    @Description("Healer fixes a flaky test caused by an animation not completing before interaction.")
    public void prompt10_healFlakyDropdown() {
        String prompt = "My dropdown test fails intermittently — sometimes it works, sometimes it doesn't";

        Map<String, String> context = new HashMap<>();
        context.put("failingMethod", "shouldSelectShippingCountry");
        context.put("errorMessage", "Element is not stable: it is animating");
        context.put("stackTrace",
                "com.microsoft.playwright.PlaywrightException: Element is not stable\n" +
                "    at CheckoutPage.selectCountry(CheckoutPage.java:67)\n" +
                "    at CheckoutTest.shouldSelectShippingCountry(CheckoutTest.java:89)");
        context.put("codeToReview",
                """
                @Step("Select country: {country}")
                public CheckoutPage selectCountry(String country) {
                    countryDropdown.click();
                    countryDropdown.selectOption(country);   // Fails because animation still running
                    return this;
                }
                """);

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_10");
    }

    // ============================================================
    // PROMPT 11 — Generate pipeline: User CRUD API
    // Expected: Complete REST Assured CRUD test class
    // ============================================================
    @Test(groups = {"sample", "sample-api"},
          description = "PROMPT_11: Full CRUD tests for the /api/users resource")
    @Story("Generate Pipeline — User CRUD API")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Generates REST Assured tests for all CRUD operations on /api/users.")
    public void prompt11_userCrudApiTests() {
        String prompt = """
                Write complete REST Assured CRUD tests for the /api/users resource.
                All endpoints require Bearer token authentication.
                
                GET /api/users → 200 list of users [{"id","email","firstName","lastName","role","createdAt"}]
                GET /api/users/{id} → 200 single user or 404 if not found
                POST /api/users → 201 created user (body: {email,firstName,lastName,password,role})
                PUT /api/users/{id} → 200 updated user or 404
                DELETE /api/users/{id} → 204 no content or 404
                
                Tests: happy path for each, 401 without token, 404 for non-existent ID,
                400 for invalid email format on POST, 409 for duplicate email on POST.
                Use test data cleanup in @AfterMethod.
                """;

        Map<String, String> context = buildApiContext(
                "http://localhost:8080",
                "UserCrudApiTest.java",
                "UserCrudApiTest",
                "com.qa.autonomous.tests.api"
        );

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_11");
    }

    // ============================================================
    // PROMPT 12 — Full pipeline: Search feature (UI + API)
    // Expected: Full 6-agent run for a complex feature
    // ============================================================
    @Test(groups = {"sample", "sample-full"},
          description = "PROMPT_12: Full suite for a product search feature")
    @Story("Full Pipeline — Search Feature")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Full 6-agent orchestration for search: UI tests, API tests, E2E flow.")
    public void prompt12_searchFeatureSuite() {
        String prompt = """
                Build a complete test suite for the product search feature.
                
                UI: Search bar in the header, results page with product cards (image, name, price, rating).
                Filters: category dropdown, price range slider, rating filter (1-5 stars).
                Pagination: 20 results per page with previous/next buttons.
                
                API: GET /api/products/search?q={query}&category={cat}&minPrice={n}&maxPrice={n}&page={n}
                Returns: {"products": [...], "total": n, "page": n, "pageSize": 20}
                
                Required tests:
                - UI: search returns results, empty search shows all, filters work, pagination works
                - API: search with query, search with filters, pagination, 400 on invalid price range
                - E2E: search from homepage → apply filter → click product → verify product detail page
                """;

        Map<String, String> context = new HashMap<>();
        context.put("baseUrl", "http://localhost:3000");
        context.put("apiUrl", "http://localhost:8080");
        context.put("targetFile", "SearchTest.java");
        context.put("targetClass", "SearchTest");
        context.put("targetPackage", "com.qa.autonomous.tests");

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_12");
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Map<String, String> buildUiContext(String baseUrl, String file, String clazz, String pkg) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put("baseUrl", baseUrl);
        ctx.put("targetFile", file);
        ctx.put("targetClass", clazz);
        ctx.put("targetPackage", pkg);
        return ctx;
    }

    private Map<String, String> buildApiContext(String apiUrl, String file, String clazz, String pkg) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put("apiUrl", apiUrl);
        ctx.put("targetFile", file);
        ctx.put("targetClass", clazz);
        ctx.put("targetPackage", pkg);
        return ctx;
    }

    private void logAndAssert(OrchestratorResult result, String promptId) {
        log.info("[{}] Orchestration result:\n{}", promptId, result.toExecutionSummary());

        assertThat(result)
                .withFailMessage("[%s] OrchestratorResult must not be null", promptId)
                .isNotNull();

        assertThat(result.getAgentOutputs())
                .withFailMessage("[%s] At least one agent must have executed", promptId)
                .isNotEmpty();

        // Log any failures without hard-failing the test (LLM output may vary)
        if (!result.isSuccess()) {
            log.warn("[{}] Orchestration reported non-success: {}", promptId, result.getFailureReason());
        }

        // Token usage summary
        log.info("[{}] Total tokens consumed: {}", promptId, result.getTotalTokensUsed());
    }
}
