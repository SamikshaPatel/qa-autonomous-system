package com.qa.autonomous.model;

/**
 * Defines all agent roles in the QA Autonomous System.
 * Each role maps to a specialized agent with distinct responsibilities,
 * skills, and constraints.
 */
public enum AgentRole {

    /**
     * Translates user intent into a structured test architecture plan.
     * Owns the high-level design of the test strategy.
     */
    ARCHITECT("Architect", "Designs the overall test strategy and architecture from user intent"),

    /**
     * Breaks architecture into a sequenced, actionable execution plan.
     * Produces step-by-step agent delegation instructions.
     */
    PLANNER("Planner", "Decomposes architecture into ordered, executable agent tasks"),

    /**
     * Explores the target system (UI or API) to gather intelligence.
     * Produces structured metadata about endpoints, pages, and elements.
     */
    EXPLORER("Explorer", "Discovers and catalogs the target system's testable surfaces"),

    /**
     * Generates production-quality Java test code.
     * Writes TestNG + Playwright/RestAssured tests with Allure annotations.
     */
    CODE_GENERATOR("CodeGenerator", "Produces enterprise-grade Java test code from specifications"),

    /**
     * Detects, diagnoses, and repairs failing or flaky tests.
     * Applies root-cause analysis before suggesting fixes.
     */
    HEALER("Healer", "Diagnoses test failures and applies targeted, minimal fixes"),

    /**
     * Reviews generated code for quality, correctness, and standards compliance.
     * Enforces enterprise coding standards and best practices.
     */
    REVIEWER("Reviewer", "Reviews test code for correctness, quality, and enterprise standards");

    private final String displayName;
    private final String description;

    AgentRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
