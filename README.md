# QA Autonomous System

An enterprise-grade AI-powered QA automation system using **Ollama Llama3** and a **multi-agent orchestration** pipeline. Users submit natural language prompts; the system routes them through specialised AI agents that design, plan, explore, generate, heal, and review test code automatically.

> **New in v2:** CLI entry point (`java -jar`), Architecture Decision Records (`docs/DECISIONS.md`), and fixed system property forwarding so `-Dollama.timeout.seconds=120` works correctly from the command line.

---

## Architecture

```
User Prompt  ──►  QaAutonomousCli  (terminal / java -jar)
                        │
                        ▼         or call directly in Java:
              AgentOrchestrator.orchestrate(prompt, context)
                        │
                        ▼
              PromptInterpreter  ──►  classifies intent → selects pipeline mode
                        │
                        ▼
              AgentOrchestrator  ──►  routes to agent pipeline
                        │
                        ├── ArchitectAgent      →  test strategy & architecture document
                        ├── PlannerAgent        →  ordered implementation task list
                        ├── ExplorerAgent       →  UI element catalog + API endpoint catalog
                        ├── CodeGeneratorAgent  →  production-quality Java test code
                        ├── HealerAgent         →  root-cause analysis + targeted fix
                        └── ReviewerAgent       →  enterprise standards enforcement
                        │
                        ▼
              OrchestratorResult  ──►  generated code on disk + Allure report
```

### Pipeline Modes

| Mode       | Agents Invoked                                              | Triggered by                          |
|------------|-------------------------------------------------------------|---------------------------------------|
| `FULL`     | Architect → Planner → Explorer → CodeGen → Healer → Reviewer | "build a complete test suite for..." |
| `GENERATE` | Architect → Planner → Explorer → CodeGen → Reviewer         | "write tests for...", "create tests"  |
| `PLAN`     | Architect → Planner                                         | "test plan for...", "strategy for..." |
| `EXPLORE`  | Explorer                                                    | "what endpoints does..."              |
| `HEAL`     | Healer → Reviewer                                           | "my test is failing..."               |
| `REVIEW`   | Reviewer                                                    | "review this code..."                 |

---

## Tech Stack

| Layer         | Technology              | Why this choice                                    |
|---------------|-------------------------|----------------------------------------------------|
| Language      | Java 17                 | LTS, records, text blocks, modern switch           |
| Test runner   | TestNG 7.x              | Suite management, grouping, parallel — built for QA |
| UI automation | Playwright Java 1.44    | Auto-wait, cross-browser, stable locator API       |
| API testing   | REST Assured 5.x        | given/when/then DSL, native Allure filter          |
| Reporting     | Allure 2.27             | Annotation-driven, GitHub Pages publishable        |
| Logging       | SLF4J + Logback         | MDC per-agent tracing, structured log files        |
| LLM           | Ollama + Llama3 (local) | Free, private, offline — model-agnostic interface  |
| HTTP client   | OkHttp 4.x              | Connection pooling, interceptors, streaming-ready  |
| Assertions    | AssertJ                 | Fluent chains, descriptive failure messages        |
| Build         | Maven 3.x               | Official Allure plugin, deterministic, universal   |
| CI/CD         | GitHub Actions          | Installs Ollama, publishes Allure to GitHub Pages  |

> See [`docs/DECISIONS.md`](docs/DECISIONS.md) for the full reasoning behind every technology choice — including what was rejected and why. This is the document to read before any interview.

---

## Prerequisites

### 1. Java 17
```bash
java -version   # must be 17+
```

### 2. Maven 3.8+
```bash
mvn -version
```

### 3. IntelliJ IDEA
- Install the **Lombok plugin**: Settings → Plugins → search "Lombok" → Install → Restart
- Enable annotation processing: Settings → Build → Compiler → Annotation Processors → ✅ Enable

### 4. Ollama + Llama3
```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh   # macOS / Linux
# Windows: https://ollama.com/download

# Pull the Llama3 model (~4.7 GB, one-time download)
ollama pull llama3

# Start the Ollama server — keep this terminal open during all runs
ollama serve

# Verify Ollama is ready before running any prompt
curl http://localhost:11434/api/tags
```

---

## Quick Start — Two Ways to Run

### Option A: `java -jar` (recommended for demos and interviews)

Build once, run anywhere — no Maven, no TestNG, no IntelliJ needed.

```bash
# Step 1 — build the fat jar (includes all dependencies)
mvn clean package -DskipTests

# Step 2 — run with a prompt
java -jar target/qa-autonomous-system-1.0.0.jar "Write API tests for GET /api/health"

# Interactive mode — type your prompt at the prompt
java -jar target/qa-autonomous-system-1.0.0.jar

# See all options
java -jar target/qa-autonomous-system-1.0.0.jar --help
```

### Option B: Maven + TestNG (for CI/CD and full Allure reporting)

```bash
# Step 1 — load dependencies
mvn clean install -DskipTests

# Step 2 — run a single sample prompt
mvn test -Dtest=SamplePromptsRunner#prompt02_healthCheckApiTest

# Step 3 — view the Allure report in your browser
mvn allure:serve
```

---

## CLI Reference

```
USAGE
  java -jar qa-autonomous-system-1.0.0.jar [OPTIONS] [PROMPT]

OPTIONS
  --prompt,       -p  <text>   Prompt to send to the agent pipeline
  --base-url,     -u  <url>    UI base URL  (default: http://localhost:3000)
  --api-url,      -a  <url>    API base URL (default: http://localhost:8080)
  --output-file,  -o  <file>   Target filename for generated code
  --class-name,   -c  <name>   Target class name for generated code
  --package,      -k  <pkg>    Target Java package for generated code
  --verbose,      -v           Print generated code preview to terminal
  --version                    Print version and exit
  --help,         -h           Show this help

ENVIRONMENT VARIABLES (highest priority — override everything else)
  OLLAMA_BASE_URL          Ollama server URL (default: http://localhost:11434)
  OLLAMA_MODEL             Model name       (default: llama3)
  OLLAMA_TIMEOUT_SECONDS   Timeout seconds  (default: 120)
```

### CLI examples

```bash
# Generate REST Assured tests with full context
java -jar target/qa-autonomous-system-1.0.0.jar \
  --prompt "Write API tests for POST /api/auth/login returning a JWT token" \
  --api-url http://localhost:8080 \
  --output-file AuthApiTest.java \
  --verbose

# Fix a failing test (triggers HEAL pipeline automatically)
java -jar target/qa-autonomous-system-1.0.0.jar \
  "My login test fails with NoSuchElementException on the email input"

# Override Ollama timeout for slow machines
OLLAMA_TIMEOUT_SECONDS=240 java -jar target/qa-autonomous-system-1.0.0.jar \
  "Build a complete Playwright test suite for the checkout feature"

# Use a faster, smaller model
OLLAMA_MODEL=llama3.2:3b java -jar target/qa-autonomous-system-1.0.0.jar \
  "Create a test plan for user registration"
```

---

## Running Sample Prompts (TestNG)

All 12 sample prompts are in `SamplePromptsRunner.java`. Each exercises a different pipeline mode and agent combination.

| # | Method | Pipeline | What It Does |
|---|--------|----------|--------------|
| 01 | `prompt01_fullLoginTestSuite` | FULL | Full UI test suite for login |
| 02 | `prompt02_healthCheckApiTest` | GENERATE | REST Assured test for health check |
| 03 | `prompt03_checkoutTestPlan` | PLAN | Test plan only, no code |
| 04 | `prompt04_healFailingLocator` | HEAL | Fix NoSuchElementException |
| 05 | `prompt05_reviewTestClass` | REVIEW | Detect Thread.sleep + hardcoded creds |
| 06 | `prompt06_exploreUserApi` | EXPLORE | Catalog a User Management API |
| 07 | `prompt07_checkoutE2eFlow` | FULL | E2E checkout Playwright test |
| 08 | `prompt08_authApiTests` | GENERATE | Auth API: all status codes |
| 09 | `prompt09_dashboardPageTest` | GENERATE | Dashboard page Playwright test |
| 10 | `prompt10_healFlakyDropdown` | HEAL | Fix animation timing flakiness |
| 11 | `prompt11_userCrudApiTests` | GENERATE | Full CRUD API tests for /api/users |
| 12 | `prompt12_searchFeatureSuite` | FULL | Search feature: UI + API + E2E |

```bash
# Run one prompt
mvn test -Dtest=SamplePromptsRunner#prompt02_healthCheckApiTest

# Run by group
mvn test -Dgroups=sample-api
mvn test -Dgroups=sample-ui
mvn test -Dgroups=sample-heal
mvn test -Dgroups=sample

# Override timeout from the command line (works in v2)
mvn test -Dollama.timeout.seconds=180 -Dtest=SamplePromptsRunner#prompt01_fullLoginTestSuite
```

---

## Writing Your Own Prompts

### In Java

```java
AgentOrchestrator orchestrator = new AgentOrchestrator();

// Minimal — pipeline mode is inferred from your prompt
OrchestratorResult result = orchestrator.orchestrate(
    "Write API tests for GET /api/products that verify the response schema"
);

// With explicit context
Map<String, String> context = new HashMap<>();
context.put("apiUrl",        "http://localhost:8080");
context.put("targetFile",    "ProductApiTest.java");
context.put("targetClass",   "ProductApiTest");
context.put("targetPackage", "com.qa.autonomous.tests.api");

OrchestratorResult result = orchestrator.orchestrate(yourPrompt, context);

// Inspect results
System.out.println(result.toExecutionSummary());
System.out.println(result.getGeneratedTestCode());
System.out.println("Tokens used: " + result.getTotalTokensUsed());
```

### Available context keys

| Key              | Used by          | Description                                       |
|------------------|------------------|---------------------------------------------------|
| `baseUrl`        | Playwright agents | UI base URL (default: `http://localhost:3000`)   |
| `apiUrl`         | REST Assured agents | API base URL (default: `http://localhost:8080`) |
| `targetFile`     | CodeGenerator    | Output filename, e.g. `LoginTest.java`            |
| `targetClass`    | CodeGenerator    | Class name, e.g. `LoginTest`                      |
| `targetPackage`  | CodeGenerator    | Package, e.g. `com.qa.autonomous.tests.ui`        |
| `failingMethod`  | Healer           | Name of the failing test method                   |
| `errorMessage`   | Healer           | Error message from the failure                    |
| `stackTrace`     | Healer           | Full stack trace from the failure                 |
| `codeToReview`   | Reviewer         | Java source code string to review                 |

---

## Configuration

### `src/main/resources/application.properties`

```properties
# Ollama settings
ollama.base.url=http://localhost:11434
ollama.model=llama3
ollama.timeout.seconds=120        # increase to 240 on slow machines
ollama.max.tokens=4096
ollama.temperature=0.1

# Browser
playwright.base.url=http://localhost:3000
playwright.headless=true          # set false to watch the browser

# API
restassured.base.uri=http://localhost:8080

# Orchestration
orchestrator.max.retries=3
orchestrator.retry.delay.ms=2000
```

### Three ways to override config

**1. Environment variables** — highest priority, works for both `java -jar` and `mvn test`:
```bash
OLLAMA_TIMEOUT_SECONDS=240 mvn test -Dgroups=sample
OLLAMA_MODEL=llama3.2:3b java -jar target/qa-autonomous-system-1.0.0.jar "..."
```

**2. Maven `-D` flags** — forwarded into the Surefire JVM in v2 via `<systemPropertyVariables>` in `pom.xml`:
```bash
mvn test -Dollama.timeout.seconds=180 -Dtest=SamplePromptsRunner#prompt01_fullLoginTestSuite
```

**3. Edit `application.properties`** — permanent change:
```bash
# Always verify the compiled copy matches after editing
grep "timeout" target/classes/application.properties
# Expected: ollama.timeout.seconds=120
```

> **Why `-D` flags didn't work before v2:** Maven Surefire forks a separate JVM to run tests. Without `<systemPropertyVariables>` in `pom.xml`, any `-Dfoo=bar` flags passed to Maven are visible in Maven's own JVM but silently absent in the forked test JVM. The fix adds explicit forwarding of all `ollama.*`, `playwright.*`, and `restassured.*` properties into the fork.

---

## CI/CD (GitHub Actions)

The pipeline at `.github/workflows/qa-autonomous-ci.yml` runs automatically on push to `main`:

| Job | What it does | Requires Ollama |
|-----|-------------|-----------------|
| Build & Validate | Compiles sources and tests | No |
| Smoke Tests | Installs Ollama, pulls Llama3, runs `smoke` group | Yes |
| Sample API Prompts | Runs `sample-api` group (main branch only) | Yes |
| Publish Report | Deploys Allure report to GitHub Pages | No |

Trigger a specific group manually:
```
GitHub → Actions → QA Autonomous System → Run workflow → test_groups: sample-api
```

The live Allure report publishes to:
```
https://<your-github-username>.github.io/<repo-name>/allure-report/
```

---

## Project Structure

```
qa-autonomous-system/
├── src/
│   ├── main/
│   │   ├── java/com/qa/autonomous/
│   │   │   ├── cli/
│   │   │   │   └── QaAutonomousCli.java        ← java -jar entry point        ★ v2
│   │   │   ├── agents/
│   │   │   │   ├── BaseAgent.java              ← abstract base for all agents
│   │   │   │   ├── ArchitectAgent.java         ← test strategy designer
│   │   │   │   ├── PlannerAgent.java           ← implementation planner
│   │   │   │   ├── ExplorerAgent.java          ← system explorer
│   │   │   │   ├── CodeGeneratorAgent.java     ← Java test code generator
│   │   │   │   ├── HealerAgent.java            ← test failure healer
│   │   │   │   └── ReviewerAgent.java          ← code quality reviewer
│   │   │   ├── orchestrator/
│   │   │   │   ├── AgentOrchestrator.java      ← central pipeline controller
│   │   │   │   └── PromptInterpreter.java      ← LLM-based intent classifier
│   │   │   ├── ollama/
│   │   │   │   ├── OllamaClient.java           ← Llama3 HTTP client (OkHttp)
│   │   │   │   └── OllamaResponse.java         ← typed LLM response model
│   │   │   ├── model/
│   │   │   │   ├── AgentRole.java              ← agent role enum
│   │   │   │   ├── AgentRequest.java           ← context passed between agents
│   │   │   │   ├── AgentOutput.java            ← single agent result
│   │   │   │   └── OrchestratorResult.java     ← full pipeline result
│   │   │   ├── config/
│   │   │   │   └── SystemConfig.java           ← env → system prop → properties
│   │   │   └── util/
│   │   │       ├── CodeExtractor.java          ← extract Java from LLM markdown
│   │   │       ├── GeneratedFileWriter.java    ← write generated code to disk
│   │   │       └── AllureUtil.java             ← Allure attachment helpers
│   │   └── resources/
│   │       ├── application.properties          ← all system configuration
│   │       └── logback.xml                     ← SLF4J + MDC structured logging
│   └── test/
│       ├── java/com/qa/autonomous/
│       │   ├── tests/
│       │   │   ├── BaseTest.java               ← Playwright TestNG base
│       │   │   ├── BasePage.java               ← Page Object base class
│       │   │   ├── BaseApiTest.java            ← REST Assured TestNG base
│       │   │   └── OrchestratorSystemTest.java ← system validation tests
│       │   └── prompts/
│       │       └── SamplePromptsRunner.java    ← 12 ready-to-run sample prompts
│       └── resources/
│           ├── testng.xml                      ← suite config (groups, parallel)
│           └── allure.properties               ← Allure environment metadata
├── .github/workflows/
│   └── qa-autonomous-ci.yml                    ← full CI/CD pipeline
├── docs/
│   └── DECISIONS.md                            ← Architecture Decision Records  ★ v2
├── logs/                                       ← runtime logs (gitignored)
├── pom.xml                                     ← Maven build + shade + surefire  ★ v2
└── README.md
```

---

## Troubleshooting

| Symptom | Root Cause | Fix |
|---------|-----------|-----|
| Log shows `timeout=5s` | Property file value or Surefire not forwarding it | Use `OLLAMA_TIMEOUT_SECONDS=120` env var (always works) |
| `-Dollama.timeout.seconds` ignored | Old `pom.xml` missing `<systemPropertyVariables>` | Update `pom.xml` from v2 release — this is now fixed |
| All agents fail with `Read timed out` | Timeout too short — Llama3 needs 20–60s per call | Set `OLLAMA_TIMEOUT_SECONDS=120` or higher |
| `Connection refused :11434` | Ollama server not running | Open a terminal and run `ollama serve` |
| `model not found: llama3` | Model not pulled yet | Run `ollama pull llama3` (~4.7 GB) |
| Fat jar fails to build | Missing `maven-shade-plugin` in old `pom.xml` | Update `pom.xml` from v2; then `mvn clean package -DskipTests` |
| Lombok errors in IntelliJ | Plugin not installed | Settings → Plugins → Lombok → Install; enable annotation processing |
| Generated code has no class body | LLM output truncated mid-response | Increase `ollama.max.tokens=8192` in properties |
| `SizeAndTimeBasedFNATP is deprecated` | Logback warning (harmless) | Safe to ignore — not an error |

---

## Architecture Decisions

Every technology choice in this system is documented with full context and reasoning in [`docs/DECISIONS.md`](docs/DECISIONS.md). Eight decisions are covered, each with: what was considered, what was chosen, and the trade-off accepted.

| # | Decision | One-line answer |
|---|----------|----------------|
| ADR-001 | Multi-agent vs. single prompt | One agent doing six jobs produces mediocre output at all six — separation of concerns applies to AI too |
| ADR-002 | Ollama + Llama3 vs. OpenAI | Free, local, private — the model-agnostic interface means GPT-4 is one config line away |
| ADR-003 | TestNG vs. JUnit 5 | TestNG was designed for test suite management; JUnit was designed for unit testing |
| ADR-004 | REST Assured vs. OkHttp for tests | given/when/then maps to how QA engineers reason; Allure integration is automatic |
| ADR-005 | Allure vs. ExtentReports | Annotation-driven — the test is the documentation; reports publish to GitHub Pages on every push |
| ADR-006 | OkHttp for Ollama client | Interceptor chain, connection pooling, streaming-ready |
| ADR-007 | SLF4J + Logback vs. Log4j2 | MDC per-agent context tagging; Log4Shell ruled Log4j2 out |
| ADR-008 | Maven vs. Gradle | Official Allure plugin; deterministic builds; universal project structure |

---

## Agent Reference

### ArchitectAgent
- **Input**: user prompt + context (baseUrl, apiUrl)
- **Output**: markdown strategy document (Objective, Scope, Tech Stack, Test Levels, Risks, Assumptions)
- **Prompt triggers**: "test plan", "strategy", "architecture", "what should I test"

### PlannerAgent
- **Input**: Architect output
- **Output**: ordered task list with file names, class names, method signatures, TestNG groups, Allure metadata
- **Always produces**: BaseTest plan, Page Object plans, TestNG XML structure

### ExplorerAgent
- **Input**: Architect + Planner output
- **Output**: UI element catalog (Playwright locators) + API endpoint catalog (schemas, status codes)
- **Locator priority**: `data-testid` → `aria-label` → role → text → label → CSS → XPath (last resort)

### CodeGeneratorAgent
- **Input**: all prior agent outputs
- **Output**: one complete, compilable Java file wrapped in ` ```java ``` ` fences
- **Hard rules**: no `Thread.sleep()`, AssertJ assertions, full Allure annotations, SLF4J logger in every class

### HealerAgent
- **Input**: failing code + error message + stack trace
- **Output**: failure classification → root cause analysis → minimal fixed code
- **Never does**: removes assertions, adds `Thread.sleep()`, changes what the test is testing

### ReviewerAgent
- **Input**: generated or healed code
- **Output**: `PASS` / `CONDITIONAL_PASS` / `FAIL` verdict + findings table + corrected code if needed
- **Checks**: 10 anti-patterns + naming conventions + import hygiene + Allure completeness + no hardcoded credentials