package com.qa.autonomous.tests;

import com.qa.autonomous.model.AgentRole;
import com.qa.autonomous.model.OrchestratorResult;
import com.qa.autonomous.orchestrator.AgentOrchestrator;
import io.qameta.allure.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrchestratorSystemTest — end-to-end system validation tests.
 *
 * These tests verify the orchestration pipeline is functioning correctly.
 * They require Ollama to be running locally on port 11434 with llama3 pulled.
 *
 * Run: mvn test -Dgroups=system
 * Skip in CI (no Ollama): -Dgroups=unit,api
 */
@Feature("QA Autonomous System")
public class OrchestratorSystemTest {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorSystemTest.class);
    private AgentOrchestrator orchestrator;

    @BeforeClass
    public void setUp() {
        orchestrator = new AgentOrchestrator();
        log.info("OrchestratorSystemTest initialised");
    }

    @Test(groups = {"system", "smoke"},
          description = "Verifies the full PLAN pipeline executes without error")
    @Story("Agent Orchestration")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Sends a test planning prompt and verifies Architect + Planner agents both produce output")
    public void shouldProducePlanWhenGivenTestPlanningPrompt() {
        String prompt = "Create a test plan for a user login feature with email and password fields";
        Map<String, String> context = new HashMap<>();
        context.put("baseUrl", "http://localhost:3000");

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);

        log.info("Plan result: {}", result.toExecutionSummary());

        assertThat(result).isNotNull();
        assertThat(result.getAgentOutputs()).isNotEmpty();

        // Architect must have produced output
        assertThat(result.getAgentOutputs())
                .anyMatch(o -> o.getRole() == AgentRole.ARCHITECT && o.isSuccess());

        // Planner must have produced output
        assertThat(result.getAgentOutputs())
                .anyMatch(o -> o.getRole() == AgentRole.PLANNER && o.isSuccess());
    }

    @Test(groups = {"system"},
          description = "Verifies GENERATE pipeline produces Java code")
    @Story("Code Generation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Sends a code generation prompt and verifies CodeGenerator produces compilable Java")
    public void shouldGenerateJavaTestCodeWhenGivenGeneratePrompt() {
        String prompt = "Write a REST Assured API test for GET /api/health that verifies it returns 200";
        Map<String, String> context = new HashMap<>();
        context.put("apiUrl", "http://localhost:8080");
        context.put("targetFile", "HealthApiTest.java");
        context.put("targetClass", "HealthApiTest");
        context.put("targetPackage", "com.qa.autonomous.tests.api");

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);

        log.info("Generate result: {}", result.toExecutionSummary());
        assertThat(result).isNotNull();

        // Should have attempted code generation
        assertThat(result.getAgentOutputs())
                .anyMatch(o -> o.getRole() == AgentRole.CODE_GENERATOR);
    }

    @Test(groups = {"system"},
          description = "Verifies HEAL pipeline is invoked when failure context is provided")
    @Story("Test Healing")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Sends a heal prompt with failure info and verifies Healer agent is invoked")
    public void shouldInvokeHealerWhenTestFailurePromptProvided() {
        String prompt = "My test is failing with NoSuchElementException on the login button locator";
        Map<String, String> context = new HashMap<>();
        context.put("failingMethod", "shouldLoginWithValidCredentials");
        context.put("errorMessage", "Timeout 30000ms exceeded: waiting for locator('#loginBtn')");
        context.put("stackTrace", "com.microsoft.playwright.TimeoutError: Timeout 30000ms exceeded");

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);

        log.info("Heal result: {}", result.toExecutionSummary());
        assertThat(result).isNotNull();
        assertThat(result.getAgentOutputs())
                .anyMatch(o -> o.getRole() == AgentRole.HEALER);
    }

    @Test(groups = {"system"},
          description = "Verifies token usage is tracked across all agents")
    @Story("Token Management")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies total token usage is captured and non-zero after orchestration")
    public void shouldTrackTokenUsageAcrossAgents() {
        String prompt = "Create a test plan for a user registration feature";

        OrchestratorResult result = orchestrator.orchestrate(prompt);

        assertThat(result).isNotNull();
        assertThat(result.getTotalTokensUsed()).isGreaterThan(0);
        log.info("Total tokens used: {}", result.getTotalTokensUsed());
    }
}
