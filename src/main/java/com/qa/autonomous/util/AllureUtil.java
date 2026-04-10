package com.qa.autonomous.util;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility for attaching agent outputs and metadata to Allure reports.
 * Makes agent orchestration fully visible in the test report.
 */
public final class AllureUtil {

    private static final Logger log = LoggerFactory.getLogger(AllureUtil.class);

    private AllureUtil() {}

    /**
     * Attaches plain text to the current Allure step.
     */
    public static void attachText(String name, String content) {
        try {
            Allure.addAttachment(name, "text/plain",
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                    ".txt");
        } catch (Exception e) {
            log.warn("Failed to attach '{}' to Allure: {}", name, e.getMessage());
        }
    }

    /**
     * Attaches Java source code to the current Allure step with syntax highlighting.
     */
    public static void attachJavaCode(String name, String javaCode) {
        try {
            if (javaCode == null || javaCode.isBlank()) return;
            Allure.addAttachment(name, "text/plain",
                    new ByteArrayInputStream(javaCode.getBytes(StandardCharsets.UTF_8)),
                    ".java");
        } catch (Exception e) {
            log.warn("Failed to attach code '{}' to Allure: {}", name, e.getMessage());
        }
    }

    /**
     * Attaches JSON content to the current Allure step.
     */
    public static void attachJson(String name, String json) {
        try {
            if (json == null || json.isBlank()) return;
            Allure.addAttachment(name, "application/json",
                    new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                    ".json");
        } catch (Exception e) {
            log.warn("Failed to attach JSON '{}' to Allure: {}", name, e.getMessage());
        }
    }

    /**
     * Attaches the orchestration execution summary to Allure.
     */
    public static void attachOrchestratorSummary(String summary) {
        attachText("Orchestration Summary", summary);
    }

    /**
     * Attaches agent output to Allure with the agent name as label.
     */
    public static void attachAgentOutput(String agentName, String output) {
        attachText(agentName + " Agent Output", output);
    }

    /**
     * Adds a parameter to the current Allure test (visible in report).
     */
    public static void addParameter(String name, String value) {
        try {
            Allure.getLifecycle().updateTestCase(tc ->
                    tc.getParameters().add(new Parameter().setName(name).setValue(value)));
        } catch (Exception e) {
            log.debug("Could not add Allure parameter '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Executes a named Allure step with a runnable body.
     */
    public static void step(String stepName, Runnable action) {
        Allure.step(stepName, action::run);
    }
}
