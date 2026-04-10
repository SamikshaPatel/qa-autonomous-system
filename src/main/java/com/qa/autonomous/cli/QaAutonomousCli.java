package com.qa.autonomous.cli;

import com.qa.autonomous.model.AgentOutput;
import com.qa.autonomous.model.OrchestratorResult;
import com.qa.autonomous.orchestrator.AgentOrchestrator;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * QaAutonomousCli — command-line entry point for the QA Autonomous System.
 *
 * Usage modes:
 *
 *   1. INLINE prompt (single run):
 *      java -jar qa-autonomous.jar "Write API tests for GET /api/health"
 *
 *   2. INTERACTIVE mode (no args):
 *      java -jar qa-autonomous.jar
 *      > (type your prompt and press Enter)
 *
 *   3. WITH CONTEXT flags:
 *      java -jar qa-autonomous.jar \
 *        --prompt "Write login UI tests" \
 *        --base-url http://localhost:3000 \
 *        --api-url http://localhost:8080 \
 *        --output-file LoginTest.java
 *
 *   4. HELP:
 *      java -jar qa-autonomous.jar --help
 *
 * Build the fat jar first:
 *      mvn clean package -DskipTests
 * Then run:
 *      java -jar target/qa-autonomous-system-1.0.0.jar "your prompt here"
 */
public class QaAutonomousCli {

    private static final String VERSION = "1.0.0";
    private static final String ANSI_RESET  = "\u001B[0m";
    private static final String ANSI_BOLD   = "\u001B[1m";
    private static final String ANSI_GREEN  = "\u001B[32m";
    private static final String ANSI_CYAN   = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED    = "\u001B[31m";
    private static final String ANSI_GRAY   = "\u001B[90m";

    private static final PrintStream out;

    static {
        // Force UTF-8 output so banner renders on all terminals
        out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        printBanner();

        // Parse arguments
        CliArgs parsed = CliArgs.parse(args);

        if (parsed.help) {
            printHelp();
            return;
        }

        if (parsed.version) {
            out.println("qa-autonomous-system v" + VERSION);
            return;
        }

        String prompt = parsed.prompt;

        // Interactive mode if no prompt given
        if (prompt == null || prompt.isBlank()) {
            prompt = runInteractiveMode();
            if (prompt == null) return;
        }

        // Build context map from flags
        Map<String, String> context = new HashMap<>();
        if (parsed.baseUrl    != null) context.put("baseUrl",      parsed.baseUrl);
        if (parsed.apiUrl     != null) context.put("apiUrl",       parsed.apiUrl);
        if (parsed.outputFile != null) context.put("targetFile",   parsed.outputFile);
        if (parsed.className  != null) context.put("targetClass",  parsed.className);
        if (parsed.pkg        != null) context.put("targetPackage", parsed.pkg);

        // Run
        out.println();
        out.println(ANSI_CYAN + "Prompt  : " + ANSI_RESET + prompt);
        if (!context.isEmpty()) {
            out.println(ANSI_CYAN + "Context : " + ANSI_RESET + context);
        }
        out.println();

        AgentOrchestrator orchestrator = new AgentOrchestrator();
        OrchestratorResult result = orchestrator.orchestrate(prompt, context);

        printResult(result, parsed.verbose);
    }

    // ---- Interactive mode ----

    private static String runInteractiveMode() {
        out.println(ANSI_BOLD + "Interactive mode" + ANSI_RESET
                + ANSI_GRAY + " — type your prompt and press Enter. Ctrl+C to exit." + ANSI_RESET);
        out.println();
        out.print(ANSI_GREEN + "> " + ANSI_RESET);
        out.flush();

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        if (!scanner.hasNextLine()) return null;
        String line = scanner.nextLine().trim();
        if (line.isBlank()) {
            out.println(ANSI_RED + "No prompt entered. Exiting." + ANSI_RESET);
            return null;
        }
        return line;
    }

    // ---- Output rendering ----

    private static void printResult(OrchestratorResult result, boolean verbose) {
        out.println(line('─', 60));

        // Status header
        if (result.isSuccess()) {
            out.println(ANSI_GREEN + ANSI_BOLD + "  ORCHESTRATION COMPLETE" + ANSI_RESET);
        } else {
            out.println(ANSI_RED + ANSI_BOLD + "  ORCHESTRATION FAILED" + ANSI_RESET);
        }

        out.println(line('─', 60));

        // Metrics
        out.printf("  %-18s %s%n", "Session ID:", result.getSessionId());
        out.printf("  %-18s %ds%n", "Duration:",
                result.getTotalExecutionTime() != null ? result.getTotalExecutionTime().toSeconds() : 0);
        out.printf("  %-18s %d%n", "Tokens used:", result.getTotalTokensUsed());
        out.println();

        // Per-agent results
        out.println(ANSI_BOLD + "  Agent Pipeline:" + ANSI_RESET);
        for (AgentOutput agent : result.getAgentOutputs()) {
            String icon   = agent.isSuccess() ? ANSI_GREEN + "  ✓" : ANSI_RED + "  ✗";
            String timing = agent.getExecutionTime() != null
                    ? ANSI_GRAY + " (" + agent.getExecutionTime().toSeconds() + "s, "
                      + agent.getTotalTokens() + " tokens)" + ANSI_RESET
                    : "";
            out.println(icon + "  " + ANSI_BOLD + padRight(agent.getRole().getDisplayName(), 16)
                    + ANSI_RESET + timing);

            if (!agent.isSuccess() && agent.getErrorMessage() != null) {
                out.println(ANSI_RED + "       " + agent.getErrorMessage() + ANSI_RESET);
            }
        }

        out.println();

        // Generated code
        if (result.getGeneratedTestCode() != null && !result.getGeneratedTestCode().isBlank()) {
            out.println(ANSI_BOLD + "  Generated Code:" + ANSI_RESET);
            out.println(ANSI_GREEN + "  Written to: "
                    + System.getProperty("user.dir") + "/src/test/java/.../generated/"
                    + ANSI_RESET);
            out.println();
            if (verbose) {
                out.println(ANSI_GRAY + "  " + line('─', 56) + ANSI_RESET);
                // Print first 40 lines of generated code
                String[] lines = result.getGeneratedTestCode().split("\n");
                int limit = Math.min(lines.length, 40);
                for (int i = 0; i < limit; i++) {
                    out.println(ANSI_GRAY + "  " + lines[i] + ANSI_RESET);
                }
                if (lines.length > 40) {
                    out.println(ANSI_GRAY + "  ... (" + (lines.length - 40) + " more lines)" + ANSI_RESET);
                }
                out.println(ANSI_GRAY + "  " + line('─', 56) + ANSI_RESET);
            }
        }

        // Review notes summary
        if (result.getReviewNotes() != null && !result.getReviewNotes().isBlank()) {
            String verdict = extractVerdict(result.getReviewNotes());
            out.println(ANSI_BOLD + "  Code Review: " + ANSI_RESET + verdict);
            out.println();
        }

        // Failure reason
        if (!result.isSuccess() && result.getFailureReason() != null) {
            out.println(ANSI_RED + "  Failure: " + result.getFailureReason() + ANSI_RESET);
            out.println();
            printTroubleshootingTips(result.getFailureReason());
        }

        out.println(line('─', 60));
        out.println(ANSI_GRAY + "  Run 'mvn allure:serve' to view full report in browser." + ANSI_RESET);
        out.println(line('─', 60));
    }

    private static void printTroubleshootingTips(String reason) {
        out.println(ANSI_YELLOW + "  Troubleshooting:" + ANSI_RESET);
        if (reason.contains("timed out") || reason.contains("timeout") || reason.contains("IO error")) {
            out.println(ANSI_YELLOW + "  → Ollama may be slow or not running." + ANSI_RESET);
            out.println(ANSI_YELLOW + "    Check: curl http://localhost:11434/api/tags" + ANSI_RESET);
            out.println(ANSI_YELLOW + "    Fix:   OLLAMA_TIMEOUT_SECONDS=120 java -jar ..." + ANSI_RESET);
        }
        if (reason.contains("Connection refused")) {
            out.println(ANSI_YELLOW + "  → Ollama is not running." + ANSI_RESET);
            out.println(ANSI_YELLOW + "    Fix:   ollama serve" + ANSI_RESET);
        }
        if (reason.contains("model not found")) {
            out.println(ANSI_YELLOW + "  → Model not pulled yet." + ANSI_RESET);
            out.println(ANSI_YELLOW + "    Fix:   ollama pull llama3" + ANSI_RESET);
        }
        out.println();
    }

    private static String extractVerdict(String reviewNotes) {
        if (reviewNotes.contains("REVIEW VERDICT: PASS"))             return ANSI_GREEN + "PASS" + ANSI_RESET;
        if (reviewNotes.contains("REVIEW VERDICT: CONDITIONAL_PASS")) return ANSI_YELLOW + "CONDITIONAL PASS" + ANSI_RESET;
        if (reviewNotes.contains("REVIEW VERDICT: FAIL"))             return ANSI_RED + "FAIL" + ANSI_RESET;
        return ANSI_GRAY + "see allure report" + ANSI_RESET;
    }

    // ---- Banner ----

    private static void printBanner() {
        out.println();
        out.println(ANSI_CYAN + ANSI_BOLD);
        out.println("  ╔═══════════════════════════════════════════════════════╗");
        out.println("  ║        QA Autonomous System  v" + VERSION + "                 ║");
        out.println("  ║  Architect · Planner · Explorer · CodeGen · Healer   ║");
        out.println("  ║  Reviewer  ·  Powered by Ollama Llama3               ║");
        out.println("  ╚═══════════════════════════════════════════════════════╝");
        out.println(ANSI_RESET);
    }

    // ---- Help ----

    private static void printHelp() {
        out.println(ANSI_BOLD + "USAGE" + ANSI_RESET);
        out.println("  java -jar qa-autonomous.jar [OPTIONS] [PROMPT]");
        out.println();
        out.println(ANSI_BOLD + "QUICK START" + ANSI_RESET);
        out.println("  java -jar qa-autonomous.jar \"Write API tests for GET /api/health\"");
        out.println("  java -jar qa-autonomous.jar   (interactive mode)");
        out.println();
        out.println(ANSI_BOLD + "OPTIONS" + ANSI_RESET);
        out.println("  --prompt,       -p   <text>   Prompt to send to the agent pipeline");
        out.println("  --base-url,     -u   <url>    UI base URL  (default: http://localhost:3000)");
        out.println("  --api-url,      -a   <url>    API base URL (default: http://localhost:8080)");
        out.println("  --output-file,  -o   <file>   Target filename for generated code");
        out.println("  --class-name,   -c   <name>   Target class name for generated code");
        out.println("  --package,      -k   <pkg>    Target Java package for generated code");
        out.println("  --verbose,      -v            Print generated code to terminal");
        out.println("  --version                     Print version and exit");
        out.println("  --help,         -h            Show this help");
        out.println();
        out.println(ANSI_BOLD + "ENVIRONMENT VARIABLES" + ANSI_RESET);
        out.println("  OLLAMA_BASE_URL          Ollama server URL (default: http://localhost:11434)");
        out.println("  OLLAMA_MODEL             Model name       (default: llama3)");
        out.println("  OLLAMA_TIMEOUT_SECONDS   Timeout in secs  (default: 120)");
        out.println();
        out.println(ANSI_BOLD + "PIPELINE MODES" + ANSI_RESET);
        out.println("  FULL     → all 6 agents  (\"build a complete test suite for...\")");
        out.println("  GENERATE → 5 agents      (\"write tests for...\", \"create API tests for...\")");
        out.println("  PLAN     → 2 agents      (\"create a test plan for...\")");
        out.println("  EXPLORE  → 1 agent       (\"what endpoints does...\")");
        out.println("  HEAL     → 2 agents      (\"my test is failing...\")");
        out.println("  REVIEW   → 1 agent       (\"review this code...\")");
        out.println();
        out.println(ANSI_BOLD + "EXAMPLES" + ANSI_RESET);
        out.println("  # Generate REST Assured tests for a health endpoint");
        out.println("  java -jar qa-autonomous.jar \\");
        out.println("    --prompt \"Write API tests for GET /api/health returning {status: UP}\" \\");
        out.println("    --api-url http://localhost:8080 \\");
        out.println("    --output-file HealthApiTest.java");
        out.println();
        out.println("  # Fix a failing test");
        out.println("  java -jar qa-autonomous.jar \"My login test fails with NoSuchElementException\"");
        out.println();
        out.println("  # Generate with verbose code output");
        out.println("  java -jar qa-autonomous.jar -v \"Write tests for the user registration API\"");
        out.println();
        out.println(ANSI_BOLD + "PREREQUISITES" + ANSI_RESET);
        out.println("  1. Ollama running:   ollama serve");
        out.println("  2. Model pulled:     ollama pull llama3");
        out.println("  3. Check Ollama:     curl http://localhost:11434/api/tags");
        out.println();
        out.println(ANSI_BOLD + "REPORT" + ANSI_RESET);
        out.println("  After a run: mvn allure:serve   (opens browser report)");
        out.println();
    }

    // ---- Argument parser ----

    static class CliArgs {
        String  prompt;
        String  baseUrl;
        String  apiUrl;
        String  outputFile;
        String  className;
        String  pkg;
        boolean verbose;
        boolean help;
        boolean version;

        static CliArgs parse(String[] args) {
            CliArgs a = new CliArgs();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--help",    "-h"  -> a.help    = true;
                    case "--version"        -> a.version = true;
                    case "--verbose", "-v"  -> a.verbose = true;
                    case "--prompt",  "-p"  -> a.prompt     = next(args, i++);
                    case "--base-url", "-u" -> a.baseUrl    = next(args, i++);
                    case "--api-url",  "-a" -> a.apiUrl     = next(args, i++);
                    case "--output-file","-o"->a.outputFile = next(args, i++);
                    case "--class-name","-c"-> a.className  = next(args, i++);
                    case "--package", "-k"  -> a.pkg        = next(args, i++);
                    default -> {
                        // Treat bare non-flag arg as the prompt
                        if (!args[i].startsWith("-") && a.prompt == null) {
                            a.prompt = args[i];
                        }
                    }
                }
            }
            return a;
        }

        private static String next(String[] args, int i) {
            return (i + 1 < args.length) ? args[i + 1] : null;
        }
    }

    // ---- Utilities ----

    private static String line(char ch, int len) {
        return String.valueOf(ch).repeat(len);
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}