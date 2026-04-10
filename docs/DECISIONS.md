# Architecture Decision Records

This document captures the key architectural decisions made when building the QA Autonomous System, including the context, the options considered, and the reasoning behind each choice.
---

## ADR-001 — Why a multi-agent system instead of a single LLM prompt?

**Context**

The goal was to generate enterprise-quality Java test code from a natural language prompt. The simplest approach is one big prompt to an LLM: "here's what I want, give me the test code." This works for simple cases but breaks down immediately at enterprise scale.

**Options considered**

| Option | Pros | Cons |
|--------|------|------|
| Single monolithic prompt | Simple, fast, one LLM call | Hallucination compounds — no separation of concerns, output quality degrades with complexity, no recovery path |
| Multi-agent pipeline | Each agent has focused responsibility, failures are isolated, output of one feeds the next | More complex, more LLM calls, orchestration overhead |
| External agent framework (LangChain, AutoGen) | Pre-built tooling | Java ecosystem poorly supported, adds opaque dependency, hides the engineering |

**Decision**

Multi-agent pipeline with six specialised agents: Architect, Planner, Explorer, CodeGenerator, Healer, Reviewer.

**Reasoning**

A single prompt asking an LLM to simultaneously design a test strategy, plan implementation, explore a system, write production code, fix failures, and review quality will produce mediocre output for all six. The same principle that motivates separation of concerns in code applies here — each agent does one thing with a precise role definition, bounded scope, and explicit output contract.

The Reviewer catching `Thread.sleep()` in the CodeGenerator's output before it reaches production is exactly why this works. A single prompt cannot review its own output.

**Trade-off accepted**

More LLM calls = more latency and token cost. Mitigated by: the PromptInterpreter routing to the minimal required pipeline, and agents sharing context efficiently through truncated prior output.

---

## ADR-002 — Why Ollama + Llama3 instead of OpenAI GPT-4?

**Context**

The system needs an LLM. The obvious choice for quality is OpenAI's API. The obvious choice for cost is local inference.

**Options considered**

| Option | Quality | Cost | Privacy | Latency | Offline |
|--------|---------|------|---------|---------|---------|
| OpenAI GPT-4 | Excellent | $0.03–0.06/1K tokens | Data leaves your network | Low | No |
| OpenAI GPT-3.5 | Good | $0.002/1K tokens | Data leaves your network | Low | No |
| Ollama + Llama3 | Good | Free (compute only) | Runs entirely locally | Higher | Yes |
| Ollama + Mistral | Good | Free | Local | Higher | Yes |

**Decision**

Ollama with Llama3 as the default, with the `OllamaClient` behind an `LlmClient` interface to allow OpenAI substitution.

**Reasoning**

For a portfolio project that will be run by the developer and demonstrated to interviewers: the zero-cost, fully-local approach is the right default. No API keys to manage, no cost per demo run, no data leaving the machine, and it works on an airplane.

The architectural decision is more important than the model: abstracting behind `LlmClient` means the system is model-agnostic by design. Swapping from Llama3 to GPT-4 requires changing one property file line, not touching any agent code.

Llama3 (8B) produces adequate results for code generation when given precise system prompts with examples. The prompt engineering in each agent compensates for the quality gap versus GPT-4.

**Trade-off accepted**

Llama3 running on consumer hardware is slower (20–60s per agent) and less capable than GPT-4. For production use, the interface allows dropping in OpenAI. For portfolio use, the local-first approach is a feature — it demonstrates the system works without external dependencies.

---

## ADR-003 — Why TestNG instead of JUnit 5?

**Context**

Both TestNG and JUnit 5 are mature Java test frameworks. The choice matters because it affects test organisation, parallel execution, grouping, and lifecycle annotations.

**Options considered**

| Feature | TestNG | JUnit 5 |
|---------|--------|---------|
| Test grouping | Native `groups` attribute | Tags with filtering |
| Parallel execution | Built-in, configurable in XML | Requires configuration |
| Data providers | `@DataProvider` built-in | `@MethodSource`, `@CsvSource` |
| Suite configuration | `testng.xml` — powerful | `@Suite` — more limited |
| Allure integration | First-class `allure-testng` | First-class `allure-junit5` |
| Dependency between tests | `dependsOnMethods` | Not supported |
| Industry adoption in QA | Dominant in enterprise QA | Dominant in unit testing |

**Decision**

TestNG 7.x.

**Reasoning**

This is a QA automation framework, not a unit testing framework. The distinction matters. TestNG was designed for complex test suite management: grouping tests into smoke/regression/api/ui, controlling execution order, running the same test with multiple data sets via `@DataProvider`, and configuring suites declaratively in XML. These are everyday requirements in enterprise QA.

JUnit 5 has caught up significantly but its heritage is unit testing. When an interviewer asks why TestNG, the answer is clear: I chose the tool designed for the problem I'm solving.

The `testng.xml` suite file also makes the CI/CD configuration explicit — a Jenkins or GitHub Actions job can run `mvn test -Dgroups=smoke` and get exactly the right subset without any code changes.

**Trade-off accepted**

JUnit 5 has a larger ecosystem and is more familiar to backend developers. In a team that already uses JUnit 5, switching would reduce friction. This would be revisited if the framework were adopted beyond a single team.

---

## ADR-004 — Why REST Assured instead of the Java HTTP Client or OkHttp for API tests?

**Context**

The generated API tests need an HTTP client. The application layer (calling Ollama) already uses OkHttp. The question is whether to use the same library for test assertions.

**Options considered**

| Option | DSL quality | Allure integration | JSON assertions | Learning curve |
|--------|------------|-------------------|-----------------|----------------|
| REST Assured | Excellent given/when/then DSL | `allure-rest-assured` filter | Built-in JSONPath, Hamcrest | Low for QA engineers |
| OkHttp | Low-level, no DSL | Manual | Manual | Medium |
| Java 11 HttpClient | Standard library | Manual | Manual | Low |
| WireMock (for mocking) | N/A | N/A | N/A | Medium |

**Decision**

REST Assured 5.x for all test-layer API interactions.

**Reasoning**

REST Assured's `given().when().then()` DSL maps directly onto how QA engineers think about API tests: given these request conditions, when I call this endpoint, then I expect this response. This is not an aesthetic choice — it's a readability choice that matters when another QA engineer reads the generated code six months later.

The `allure-rest-assured` filter makes every HTTP request and response automatically visible in the Allure report with zero additional code. This is a significant advantage: every test automatically has request/response attached to its Allure report entry.

OkHttp is used at the infrastructure layer (calling Ollama) because it offers streaming support and connection pool management that REST Assured doesn't expose. Using REST Assured for test assertions and OkHttp for infrastructure calls is a deliberate separation of concerns — the right tool for each layer.

**Trade-off accepted**

REST Assured adds a dependency. For a project that already has 15+ dependencies, this is acceptable. If bundle size were a hard constraint, the Java HttpClient would be the alternative.

---

## ADR-005 — Why Allure instead of ExtentReports or a custom reporter?

**Context**

Test reporting is the primary way QA results are communicated to stakeholders. The choice of reporting framework affects how much information is surfaced automatically versus manually.

**Options considered**

| Option | Setup effort | Agent output attachment | Screenshot support | CI integration | Interactivity |
|--------|-------------|------------------------|-------------------|----------------|---------------|
| Allure 2.x | Medium | Via `@Attachment` | Native | GitHub Pages, Jenkins plugin | High — filterable, searchable |
| ExtentReports | Low | Manual | Manual | Embedded HTML | Medium |
| Custom HTML reporter | High | Full control | Full control | Custom | Full control |
| Surefire HTML | Zero | None | None | Maven site | Low |

**Decision**

Allure 2.x.

**Reasoning**

Allure's annotation-driven model (`@Step`, `@Feature`, `@Story`, `@Severity`, `@Description`, `@Attachment`) integrates test documentation directly into the test code. This means the test is both the executable specification and the report source — there is no separate document to maintain.

For this system specifically, every agent's output is attached to the Allure report automatically via `AllureUtil.attachAgentOutput()`. This means an interviewer running the system gets a complete audit trail of every LLM interaction, every generated code file, and every review verdict — all in a browseable report generated by `mvn allure:serve`.

The GitHub Actions pipeline publishes the Allure report to GitHub Pages on every push to main. This means the live report is always accessible at a public URL — a portfolio asset that requires no setup from the viewer.

**Trade-off accepted**

Allure requires the Allure CLI or Maven plugin to generate the HTML report from the raw results directory. This adds a step compared to ExtentReports which generates directly. This is a known and accepted trade-off given Allure's significant quality advantage.

---

## ADR-006 — Why OkHttp instead of Apache HttpClient for Ollama communication?

**Context**

The `OllamaClient` makes HTTP requests to the local Ollama server. This is infrastructure code, not test code, so the choice is made on engineering merit rather than QA tooling alignment.

**Options considered**

| Option | Connection pooling | Streaming support | API quality | Bundle size |
|--------|--------------------|-------------------|-------------|-------------|
| OkHttp 4.x | Excellent | Native | Modern, fluent | ~800KB |
| Apache HttpClient 5 | Good | Supported | Verbose | ~700KB |
| Java 11 HttpClient | Basic | Supported | Standard library | Zero |
| Spring WebClient | Excellent | Native | Reactive | Requires Spring |

**Decision**

OkHttp 4.x.

**Reasoning**

OkHttp is the de facto standard for JVM HTTP clients in production systems (it powers Retrofit, and is used internally by Square, Google, and others). Its connection pool, interceptor chain, and timeout model are well understood and battle-tested.

The logging interceptor (`okhttp3.logging.HttpLoggingInterceptor`) integrates cleanly with SLF4J, meaning all HTTP traffic to Ollama appears in the structured log files automatically. The interceptor chain also allows inserting authentication headers, retry logic, and request ID injection without modifying the core request code.

For future streaming support (if Ollama's streaming API is adopted to show real-time token generation), OkHttp's `ResponseBody` handles Server-Sent Events naturally.

**Trade-off accepted**

The Java 11 HttpClient would reduce dependencies by one. The trade-off is that OkHttp's API is significantly more productive for complex timeout and retry scenarios, which this system requires.

---

## ADR-007 — Why SLF4J + Logback instead of Log4j2 or java.util.logging?

**Context**

Enterprise systems require structured, configurable logging. The choice of logging framework affects observability, debugging speed, and integration with log aggregation platforms.

**Options considered**

| Option | MDC support | JSON output | Config format | Performance | Security history |
|--------|-------------|-------------|---------------|-------------|-----------------|
| SLF4J + Logback | Yes | Via encoder | XML | Excellent | Good |
| SLF4J + Log4j2 | Yes | Native | XML/JSON/YAML | Excellent | Log4Shell (CVE-2021-44228) |
| java.util.logging | Limited | Manual | properties | Poor | Good |
| System.out.println | None | None | None | Adequate | N/A |

**Decision**

SLF4J API + Logback implementation.

**Reasoning**

SLF4J as the API layer means the logging implementation can be swapped without changing any code — if a team's existing infrastructure uses Log4j2 or another backend, dropping in their `slf4j-log4j2` bridge works immediately.

Logback's MDC (Mapped Diagnostic Context) is the critical feature for this system. Every agent call sets `MDC.put("agentName", ...)` and `MDC.put("sessionId", ...)`, which means every log line automatically includes which agent produced it and which session it belongs to. Filtering `logs/agents.log` by session ID gives a complete per-run audit trail with zero additional code.

Log4j2 was explicitly avoided due to the Log4Shell vulnerability (CVE-2021-44228), which affected any system running Log4j2 2.0–2.14.1. While patched versions exist, the decision to avoid Log4j2 in new projects is defensible and demonstrates security awareness.

**Trade-off accepted**

Log4j2 has native async logging performance advantages for extremely high-throughput systems. For a test automation framework that processes at most hundreds of agent calls per day, this difference is irrelevant.

---

## ADR-008 — Why Maven instead of Gradle?

**Context**

Both Maven and Gradle are capable Java build tools. This is often a team preference decision, but there are technical considerations relevant to this project.

**Options considered**

| Factor | Maven | Gradle |
|--------|-------|--------|
| Allure plugin | Official `allure-maven` with full support | Community plugin, less maintained |
| Surefire / TestNG integration | First-class, 15+ years | Good but version-sensitive |
| Reproducibility | Deterministic by design | Build script code can cause non-determinism |
| CI/CD familiarity | Universal | Common but configuration varies |
| Learning curve | Low (declarative XML) | Higher (Groovy/Kotlin DSL) |

**Decision**

Maven 3.8+.

**Reasoning**

The `allure-maven` plugin is maintained by the Allure team and has direct integration with the Maven lifecycle (`mvn allure:report`, `mvn allure:serve`). The equivalent Gradle plugin is community-maintained and has historically lagged behind Allure releases.

For a portfolio project where the person running it may not be a Java expert, Maven's declarative `pom.xml` is more transparent than Gradle's executable build scripts. `mvn clean install -DskipTests` is a universal command that works predictably across environments.

Maven's convention-over-configuration approach also means the project structure (`src/main/java`, `src/test/java`, `src/main/resources`) is immediately recognisable to any Java engineer — the layout communicates intent without documentation.

**Trade-off accepted**

Gradle's incremental build support makes it faster for large multi-module projects. For a single-module framework of this size, the build time difference is negligible. If this grew into a multi-module project (e.g., separate modules for agents, generated tests, and reports), Gradle would be worth reconsidering.

---

## Summary

| ADR | Decision | The one-sentence answer for an interview |
|-----|----------|------------------------------------------|
| 001 | Multi-agent pipeline | One agent doing six jobs produces mediocre results at all six — separation of concerns applies to AI systems too. |
| 002 | Ollama + Llama3 | Free, local, private — and the model-agnostic interface means production can use GPT-4 without touching agent code. |
| 003 | TestNG | Designed for test suite management; JUnit is designed for unit testing — I chose the right tool for the problem. |
| 004 | REST Assured | Its given/when/then DSL maps directly to how QA engineers reason about API tests, and Allure integration is automatic. |
| 005 | Allure | Annotation-driven reporting means the test is the documentation, and the live report publishes to GitHub Pages on every push. |
| 006 | OkHttp | Battle-tested connection pooling and interceptor chain; streaming-ready for real-time token display if needed. |
| 007 | SLF4J + Logback | MDC context tagging gives per-agent, per-session log filtering with zero additional code. Log4Shell made Log4j2 a non-starter. |
| 008 | Maven | Allure's official plugin, deterministic builds, and a project layout every Java engineer recognises on sight. |