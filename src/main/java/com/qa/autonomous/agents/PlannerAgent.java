package com.qa.autonomous.agents;

import com.qa.autonomous.model.AgentRequest;
import com.qa.autonomous.model.AgentOutput;
import com.qa.autonomous.model.AgentRole;
import com.qa.autonomous.ollama.OllamaClient;

/**
 * PLANNER AGENT
 *
 * Role: Converts the Architect's strategy document into a concrete, ordered
 *       implementation plan that the CodeGenerator can execute without ambiguity.
 *
 * Skills:
 *   - Task decomposition and sequencing
 *   - File and class naming according to enterprise conventions
 *   - TestNG XML suite planning
 *   - Dependency mapping between test classes
 *   - Estimating implementation complexity
 *
 * Rules (MUST follow):
 *   - Read the Architect output carefully before planning
 *   - Produce a numbered, ordered task list — order matters for code generation
 *   - Every task must have: Task ID, Description, File name, Class name, Method names, Dependencies
 *   - Never invent new test scenarios not present in the Architect document
 *   - Never generate Java code — only file plans and specifications
 *   - Naming conventions: PascalCase for classes, camelCase for methods, kebab-case for files
 *
 * Output contract:
 *   A structured markdown plan with a TASK LIST table and FILE MANIFEST section.
 */
public class PlannerAgent extends BaseAgent {

    public PlannerAgent(OllamaClient ollamaClient) {
        super(ollamaClient);
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.PLANNER;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                You are the Planner agent in an enterprise QA Autonomous System.
                
                YOUR IDENTITY:
                You are a meticulous senior QA lead who transforms high-level test strategies into
                implementation-ready task plans. You are obsessive about completeness and naming conventions.
                
                YOUR NAMING CONVENTIONS:
                - Test classes: [Feature]Test.java (e.g., LoginTest.java, CheckoutApiTest.java)
                - Page Objects: [PageName]Page.java (e.g., LoginPage.java, DashboardPage.java)
                - Base classes: Base[Type].java (e.g., BaseTest.java, BasePage.java)
                - Utility classes: [Name]Util.java or [Name]Helper.java
                - Test methods: should[Condition]When[Action] (e.g., shouldDisplayErrorWhenInvalidEmail)
                - Packages: com.qa.autonomous.[feature] (e.g., com.qa.autonomous.login)
                
                YOUR TASK SPECIFICATION FORMAT:
                Each task must include:
                - TASK_ID: T-001, T-002, etc.
                - TYPE: [PAGE_OBJECT | BASE_CLASS | TEST_CLASS | UTILITY | CONFIG]
                - FILE: exact filename with path
                - CLASS: exact class name
                - PACKAGE: exact package
                - METHODS: comma-separated list of method signatures
                - DEPENDS_ON: TASK_IDs this task depends on (or NONE)
                - ALLURE: @Feature and @Story values
                - TESTNG_GROUPS: groups to assign
                - PRIORITY: P1 / P2 / P3
                
                YOUR RULES (non-negotiable):
                1. Tasks must be in dependency order — base classes before tests, pages before tests
                2. Every test class needs a corresponding TestNG XML entry
                3. Do not add scenarios that the Architect did not specify
                4. Output only the structured plan — no Java code
                5. Always include: BaseTest, BasePage (or BaseApi), at least one test class
                6. Include a FILE MANIFEST section listing every file to be created
                7. Include a TESTNG XML PLAN section showing the testng.xml structure
                
                YOUR OUTPUT FORMAT:
                ## IMPLEMENTATION PLAN
                ## TASK LIST
                [task table]
                ## FILE MANIFEST
                [file list with paths]
                ## TESTNG XML PLAN
                [testng.xml structure in plain text]
                ## EXECUTION ORDER
                [numbered sequence]
                """;
    }

    @Override
    protected String buildUserPrompt(AgentRequest request) {
        AgentOutput architectOutput = request.getPriorOutput(AgentRole.ARCHITECT);
        String architectContent = architectOutput != null
                ? architectOutput.getContent()
                : "No architect output available. Plan based on user request only.";

        return String.format("""
                USER ORIGINAL REQUEST:
                %s
                
                ARCHITECT STRATEGY DOCUMENT:
                %s
                
                TASK:
                Create a complete, ordered implementation plan based on the Architect's strategy.
                
                REQUIREMENTS:
                1. Every task must follow the exact specification format from your rules
                2. Order tasks in dependency sequence (infrastructure first, tests last)
                3. Include BaseTest.java with: TestNG @BeforeMethod/@AfterMethod, Playwright setup, Allure setup
                4. Include BasePage.java or BaseApiTest.java as appropriate
                5. Every test method must have explicit Allure annotations planned
                6. Output the FILE MANIFEST with exact file paths
                7. Output the TESTNG XML PLAN structure
                
                Do not generate Java code. Generate plans only.
                """,
                request.getUserPrompt(),
                architectContent);
    }
}
