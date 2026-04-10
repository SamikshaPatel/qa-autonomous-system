package com.qa.autonomous.orchestrator;

import com.qa.autonomous.agents.*;
import com.qa.autonomous.config.SystemConfig;
import com.qa.autonomous.model.*;
import com.qa.autonomous.ollama.OllamaClient;
import com.qa.autonomous.util.AllureUtil;
import com.qa.autonomous.util.CodeExtractor;
import com.qa.autonomous.util.GeneratedFileWriter;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * AgentOrchestrator — the central controller of the QA Autonomous System.
 *
 * Responsibilities:
 *   1. Interpret user prompts to determine which agent pipeline to execute
 *   2. Invoke agents in the correct order, passing accumulated context
 *   3. Attach all agent outputs to Allure for full traceability
 *   4. Handle partial failures gracefully (continue or abort based on severity)
 *   5. Write generated code to disk when produced
 *
 * Pipeline modes:
 *   - FULL:      Architect → Planner → Explorer → CodeGenerator → Healer (if needed) → Reviewer
 *   - GENERATE:  Architect → Planner → Explorer → CodeGenerator → Reviewer
 *   - HEAL:      Healer → Reviewer
 *   - REVIEW:    Reviewer only
 *   - EXPLORE:   Explorer only
 *   - PLAN:      Architect → Planner
 */
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final OllamaClient ollamaClient;
    private final ArchitectAgent     architectAgent;
    private final PlannerAgent       plannerAgent;
    private final ExplorerAgent      explorerAgent;
    private final CodeGeneratorAgent codeGeneratorAgent;
    private final HealerAgent        healerAgent;
    private final ReviewerAgent      reviewerAgent;
    private final SystemConfig       config;
    private final PromptInterpreter  promptInterpreter;

    public AgentOrchestrator() {
        this.config             = SystemConfig.getInstance();
        this.ollamaClient       = new OllamaClient();
        this.architectAgent     = new ArchitectAgent(ollamaClient);
        this.plannerAgent       = new PlannerAgent(ollamaClient);
        this.explorerAgent      = new ExplorerAgent(ollamaClient);
        this.codeGeneratorAgent = new CodeGeneratorAgent(ollamaClient);
        this.healerAgent        = new HealerAgent(ollamaClient);
        this.reviewerAgent      = new ReviewerAgent(ollamaClient);
        this.promptInterpreter  = new PromptInterpreter(ollamaClient);
        log.info("AgentOrchestrator initialised — all 6 agents loaded");
    }

    /**
     * Main entry point. Accepts a raw user prompt and orchestrates the full agent pipeline.
     *
     * @param userPrompt  The natural language prompt from the user.
     * @param context     Optional key-value metadata (baseUrl, apiUrl, targetFile, etc.)
     * @return            Complete orchestration result with all agent outputs.
     */
    @Step("Orchestrate: {userPrompt}")
    public OrchestratorResult orchestrate(String userPrompt, Map<String, String> context) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("sessionId", sessionId);
        Instant start = Instant.now();

        log.info("=== ORCHESTRATION START | session={} ===", sessionId);
        log.info("User prompt: {}", userPrompt);

        AllureUtil.addParameter("Session ID", sessionId);
        AllureUtil.addParameter("User Prompt", userPrompt);

        try {
            // 1. Interpret the prompt to choose pipeline mode
            PipelineMode mode = promptInterpreter.interpret(userPrompt, sessionId);
            log.info("Pipeline mode selected: {} | session={}", mode, sessionId);
            AllureUtil.addParameter("Pipeline Mode", mode.name());

            // 2. Build the initial request
            AgentRequest baseRequest = AgentRequest.builder()
                    .sessionId(sessionId)
                    .userPrompt(userPrompt)
                    .context(context != null ? context : new HashMap<>())
                    .build();

            // 3. Execute the selected pipeline
            List<AgentOutput> outputs = executePipeline(mode, baseRequest, sessionId);

            // 4. Determine final success state
            boolean success = outputs.stream().allMatch(AgentOutput::isSuccess);
            String failureReason = outputs.stream()
                    .filter(o -> !o.isSuccess())
                    .map(o -> o.getRole().getDisplayName() + ": " + o.getErrorMessage())
                    .findFirst().orElse(null);

            // 5. Extract generated code if present
            String generatedCode = outputs.stream()
                    .filter(o -> o.getRole() == AgentRole.CODE_GENERATOR && o.isSuccess())
                    .map(AgentOutput::getExtractedCode)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);

            String reviewNotes = outputs.stream()
                    .filter(o -> o.getRole() == AgentRole.REVIEWER && o.isSuccess())
                    .map(AgentOutput::getContent)
                    .findFirst().orElse(null);

            // 6. Write generated code to disk
            if (generatedCode != null && CodeExtractor.looksLikeValidJava(generatedCode)) {
                GeneratedFileWriter.writeGeneratedClass(generatedCode, sessionId);
            }

            Duration totalTime = Duration.between(start, Instant.now());
            OrchestratorResult result = OrchestratorResult.builder()
                    .sessionId(sessionId)
                    .userPrompt(userPrompt)
                    .agentOutputs(outputs)
                    .generatedTestCode(generatedCode)
                    .reviewNotes(reviewNotes)
                    .success(success)
                    .failureReason(failureReason)
                    .totalExecutionTime(totalTime)
                    .startedAt(start)
                    .build();

            String summary = result.toExecutionSummary();
            log.info("\n{}", summary);
            AllureUtil.attachOrchestratorSummary(summary);

            log.info("=== ORCHESTRATION END | session={} success={} duration={}s tokens={} ===",
                    sessionId, success, totalTime.toSeconds(), result.getTotalTokensUsed());

            return result;

        } catch (Exception e) {
            Duration elapsed = Duration.between(start, Instant.now());
            log.error("Orchestration failed with unexpected exception | session={}: {}", sessionId, e.getMessage(), e);
            return OrchestratorResult.builder()
                    .sessionId(sessionId)
                    .userPrompt(userPrompt)
                    .agentOutputs(Collections.emptyList())
                    .success(false)
                    .failureReason("Orchestrator exception: " + e.getMessage())
                    .totalExecutionTime(elapsed)
                    .startedAt(start)
                    .build();
        } finally {
            MDC.remove("sessionId");
        }
    }

    /**
     * Convenience overload with no extra context.
     */
    public OrchestratorResult orchestrate(String userPrompt) {
        return orchestrate(userPrompt, new HashMap<>());
    }

    // ---- Pipeline execution ----

    private List<AgentOutput> executePipeline(PipelineMode mode, AgentRequest baseRequest, String sessionId) {
        List<AgentOutput> outputs = new ArrayList<>();

        switch (mode) {
            case FULL, GENERATE -> {
                runAgent(architectAgent,     baseRequest, outputs, sessionId, architectAgent.getRole().getDisplayName());
                runAgent(plannerAgent,       baseRequest, outputs, sessionId, plannerAgent.getRole().getDisplayName());
                runAgent(explorerAgent,      baseRequest, outputs, sessionId, explorerAgent.getRole().getDisplayName());
                runAgent(codeGeneratorAgent, baseRequest, outputs, sessionId, codeGeneratorAgent.getRole().getDisplayName());
                if (mode == PipelineMode.FULL) {
                    runAgent(healerAgent, baseRequest, outputs, sessionId, healerAgent.getRole().getDisplayName());
                }
                runAgent(reviewerAgent, baseRequest, outputs, sessionId, reviewerAgent.getRole().getDisplayName());
            }
            case HEAL -> {
                runAgent(healerAgent,   baseRequest, outputs, sessionId, healerAgent.getRole().getDisplayName());
                runAgent(reviewerAgent, baseRequest, outputs, sessionId, reviewerAgent.getRole().getDisplayName());
            }
            case REVIEW  -> runAgent(reviewerAgent,  baseRequest, outputs, sessionId, reviewerAgent.getRole().getDisplayName());
            case EXPLORE -> runAgent(explorerAgent,  baseRequest, outputs, sessionId, explorerAgent.getRole().getDisplayName());
            case PLAN    -> {
                runAgent(architectAgent, baseRequest, outputs, sessionId, architectAgent.getRole().getDisplayName());
                runAgent(plannerAgent,   baseRequest, outputs, sessionId, plannerAgent.getRole().getDisplayName());
            }
        }
        return outputs;
    }

    @Step("Run agent: {agentName}")
    private void runAgent(BaseAgent agent, AgentRequest baseRequest,
                          List<AgentOutput> outputs, String sessionId, String agentName) {
        // Build an updated request with all prior outputs injected
        AgentRequest request = AgentRequest.builder()
                .sessionId(baseRequest.getSessionId())
                .userPrompt(baseRequest.getUserPrompt())
                .agentTask(baseRequest.getAgentTask())
                .context(baseRequest.getContext())
                .priorOutputs(new ArrayList<>(outputs))
                .build();

        log.info("--- Invoking {} | session={} ---", agentName, sessionId);
        AgentOutput output = agent.execute(request);
        outputs.add(output);

        // Attach to Allure
        AllureUtil.attachAgentOutput(agent.getRole().getDisplayName(), output.getContent());
        if (output.getExtractedCode() != null) {
            AllureUtil.attachJavaCode(agent.getRole().getDisplayName() + " — Generated Code",
                    output.getExtractedCode());
        }

        log.info("--- {} completed | success={} tokens={} time={}ms ---",
                agentName,
                output.isSuccess(),
                output.getTotalTokens(),
                output.getExecutionTime() != null ? output.getExecutionTime().toMillis() : -1);
    }

    /**
     * Pipeline execution modes.
     */
    public enum PipelineMode {
        FULL,       // Full pipeline: all 6 agents
        GENERATE,   // Arch → Plan → Explore → CodeGen → Review (no Healer)
        HEAL,       // Healer + Reviewer only
        REVIEW,     // Reviewer only
        EXPLORE,    // Explorer only
        PLAN        // Architect + Planner only
    }
}
