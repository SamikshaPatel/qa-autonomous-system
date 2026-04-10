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
 * PROMPT_13: ToDoList API Tests generation
 */
@Feature("Sample Prompts — Phase 3 Experimentation")
public class MyPromptRunner {

    private static final Logger log = LoggerFactory.getLogger(MyPromptRunner.class);
    private AgentOrchestrator orchestrator;

    @BeforeClass
    public void setUp() {
        orchestrator = new AgentOrchestrator();
        log.info("MyPromptRunner initialised — ready for experimentation");
    }

    // ============================================================
    // PROMPT 13 — Generate pipeline: User CRUD API -https://freeapi.hashnode.space/api-guide/apireference/getUsers
    // Expected: Complete REST Assured CRUD test class
    // ============================================================
    @Test(groups = {"sample", "sample-api"},
            description = "PROMPT_13: Full CRUD tests for the /ToDo List API")
    @Story("Generate Pipeline — ToDo List API")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Generates REST Assured tests for all CRUD operations on /api/users.")
    public void prompt13_ToDoListApiTests() {
        String prompt = """
               Generate comprehensive, production-grade API test cases for a TO-DO application based on the following API documentation:
                
                 Context:
                 This API belongs to a TO-DO system that typically includes the following endpoints:
                 - GET v1//todos (get all todos)
                 - GET v1/todos/{id}
                 - POST v1/todos
                 - PUT v1/todos/{id}
                 - PATCH v1/todos/{id}
                 - DELETE v1/todos/{id}
                
                 (Assume standard REST behavior even if not fully documented.)
                
                 ---
                
                 ### 🎯 Your Responsibilities
                
                 Generate **detailed test cases** covering:
                
                 ### 1. Functional Testing
                 - Validate successful responses for all endpoints
                 - Validate CRUD operations
                 - Verify response schema and data correctness
                 - Validate filtering, sorting, pagination (if applicable)
                
                 ### 2. Negative Testing
                 - Invalid IDs
                 - Missing required fields
                 - Invalid payload formats
                 - Unauthorized / forbidden access
                 - Invalid query parameters
                
                 ### 3. Edge Cases
                 - Empty todo list
                 - Large dataset handling
                 - Duplicate entries
                 - Boundary values (string length, nulls)
                
                 ### 4. Contract Testing
                 - Validate response structure (JSON schema)
                 - Field types (id, title, completed, timestamps, etc.)
                
                 ### 5. Status Code Validation
                 - 200, 201, 204 (success cases)
                 - 400, 404, 422 (client errors)
                 - 500 (server errors)
                
                 ### 6. Performance Considerations
                 - Response time expectations
                 - Bulk requests behavior
                
                 ### 7. Security Testing
                 - Auth validation (if applicable)
                 - Injection attempts (SQL, script)
                 - Data exposure validation

                """;

        Map<String, String> context = buildApiContext(
                "https://freeapi.hashnode.space/api-guide/apireference/getAllTodos",
                "TodoList_Api_Test.java",
                "TodoListApiTest",
                "com.qa.autonomous.tests.api"
        );

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_13");
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
