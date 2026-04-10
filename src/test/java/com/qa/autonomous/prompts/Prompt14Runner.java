package com.qa.autonomous.prompts;

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


@Feature("Sample Prompts — Phase 3 Experimentation")
public class Prompt14Runner {

    private static final Logger log = LoggerFactory.getLogger(Prompt14Runner.class);
    private AgentOrchestrator orchestrator;

    @BeforeClass
    public void setUp() {
        orchestrator = new AgentOrchestrator();
        log.info("Prompt14Runner initialised — ready for experimentation");
    }
    // ============================================================
    // PROMPT 14— Review pipeline: Code quality review
    // Expected: Healer identifies testing issues
    // ============================================================
    @Test(groups = {"sample", "sample-review"},
            description = "PROMPT_14: Review a test class for epass/fail")
    @Story("Review Pipeline — Code Quality")
    @Severity(SeverityLevel.NORMAL)
    @Description("Healer fixes a flaky test ")
    public void prompt14_reviewToDoListApiTestClass() {
        String prompt = "Review this test code for issues";
        Map<String, String> context = new HashMap<>();
        context.put("codeToReview",
                """
                        package com.qa.autonomous.tests.api;
                        
                        import com.qa.autonomous.tests.BaseApiTest;
                        import io.qameta.allure.*;
                        import io.restassured.RestAssured;
                        import io.restassured.response.Response;
                        import org.junit.BeforeClass;
                        import org.junit.Test;
                        import org.slf4j.Logger;
                        import org.slf4j.LoggerFactory;
                        
                        import static io.restassured.RestAssured.given;
                        import static io.restassured.http.ContentType.JSON;
                        import static org.hamcrest.CoreMatchers.equalTo;
                        import static org.hamcrest.Matchers.hasJsonPath;
                        
                        public class TodoListApiTest extends BaseApiTest {
                        
                            private static final Logger log = LoggerFactory.getLogger(TodoListApiTest.class);
                        
                            @BeforeClass
                            public static void setup() {
                                RestAssured.baseURI = "\\n" +
                                        "https://api.freeapi.app/api/";
                            }
                        
                            @Feature("Todo List API")
                            @Story("Get All Todos")
                            @Severity(SeverityLevel.NORMAL)
                            @Description("Verifies the GET /v1/todos endpoint returns a list of todos")
                            @Test
                            public void shouldReturnAllTodos() {
                                log.info("Testing GET /v1/todos");
                                given()
                                    .when().get("/v1/todos")
                                    .then()
                                    .assertThat().statusCode(200)
                                    .body("todos.size()", equalTo(10));
                            }
                        
                            @Feature("Todo List API")
                            @Story("Get Todo by ID")
                            @Severity(SeverityLevel.NORMAL)
                            @Description("Verifies the GET /v1/todos/{id} endpoint returns a single todo item")
                            @Test
                            public void shouldReturnTodoById() {
                                log.info("Testing GET /v1/todos/1");
                                given()
                                    .pathParam("id", 1)
                                    .when().get("/v1/todos/{id}")
                                    .then()
                                    .assertThat().statusCode(200)
                                    .body("id", equalTo(1));
                            }
                        
                            @Feature("Todo List API")
                            @Story("Create Todo")
                            @Severity(SeverityLevel.NORMAL)
                            @Description("Verifies the POST /v1/todos endpoint creates a new todo item")
                            @Test
                            public void shouldCreateNewTodo() {
                                log.info("Testing POST /v1/todos");
                                given()
                                    .contentType(JSON)
                                    .body("{\\"title\\":\\"New Todo\\",\\"completed\\":false}")
                                    .when().post("/v1/todos")
                                    .then()
                                    .assertThat().statusCode(201);
                            }
                        
                            @Feature("Todo List API")
                            @Story("Update Todo")
                            @Severity(SeverityLevel.NORMAL)
                            @Description("Verifies the PUT /v1/todos/{id} endpoint updates a todo item")
                            @Test
                            public void shouldUpdateExistingTodo() {
                                log.info("Testing PUT /v1/todos/1");
                                given()
                                    .pathParam("id", 1)
                                    .contentType(JSON)
                                    .body("{\\"title\\":\\"Updated Todo\\",\\"completed\\":true}")
                                    .when().put("/v1/todos/{id}", 1)
                                    .then()
                                    .assertThat().statusCode(200);
                            }
                        
                            @Feature("Todo List API")
                            @Story("Delete Todo")
                            @Severity(SeverityLevel.NORMAL)
                            @Description("Verifies the DELETE /v1/todos/{id} endpoint deletes a todo item")
                            @Test
                            public void shouldDeleteExistingTodo() {
                                log.info("Testing DELETE /v1/todos/1");
                                given()
                                    .pathParam("id", 1)
                                    .when().delete("/v1/todos/{id}", 1)
                                    .then()
                                    .assertThat().statusCode(204);
                            }
                        }
                """);

        OrchestratorResult result = orchestrator.orchestrate(prompt, context);
        logAndAssert(result, "PROMPT_05");
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


