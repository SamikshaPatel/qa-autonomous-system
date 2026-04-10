package com.qa.autonomous.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.autonomous.config.SystemConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for Ollama's /api/generate endpoint.
 * Handles prompt construction, response parsing, retry logic,
 * and token usage logging.
 *
 * Thread-safe: OkHttpClient is internally thread-safe.
 */
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final SystemConfig config;
    private final String baseUrl;
    private final String model;

    public OllamaClient() {
        this.config = SystemConfig.getInstance();
        this.baseUrl = config.getOllamaBaseUrl();
        this.model = config.getOllamaModel();

        int timeoutSec = config.getOllamaTimeoutSeconds();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(timeoutSec, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        this.mapper = new ObjectMapper();
        log.info("OllamaClient initialized | url={} model={} timeout={}s", baseUrl, model, timeoutSec);
    }

    /**
     * Sends a prompt to Ollama and returns the full response text.
     *
     * @param systemPrompt  The agent's role definition and rules (system context).
     * @param userPrompt    The specific task/question for this invocation.
     * @param sessionId     For MDC tracing across log files.
     * @return              Raw text response from Llama3.
     */
    public OllamaResponse generate(String systemPrompt, String userPrompt, String sessionId) {
        MDC.put("sessionId", sessionId);
        Instant start = Instant.now();

        if (config.isLogAgentPrompts()) {
            log.debug("OLLAMA REQUEST | session={}\n--- SYSTEM ---\n{}\n--- USER ---\n{}",
                    sessionId,
                    truncate(systemPrompt, 500),
                    truncate(userPrompt, 500));
        }

        // Build the combined prompt (Ollama uses a single "prompt" field for non-chat API)
        String fullPrompt = buildPrompt(systemPrompt, userPrompt);

        OllamaRequestBody requestBody = new OllamaRequestBody(
                model,
                fullPrompt,
                false, // stream=false for synchronous response
                new OllamaOptions(config.getOllamaTemperature(), config.getOllamaMaxTokens())
        );

        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            log.error("Failed to serialize Ollama request: {}", e.getMessage(), e);
            return OllamaResponse.failure("Serialization error: " + e.getMessage());
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/api/generate")
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("Accept", "application/json")
                .build();

        int maxRetries = config.getOrchestratorMaxRetries();
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Response response = httpClient.newCall(request).execute()) {
                Duration elapsed = Duration.between(start, Instant.now());

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "empty";
                    log.warn("Ollama returned HTTP {} on attempt {}/{}: {}", response.code(), attempt, maxRetries, errorBody);
                    if (attempt == maxRetries) {
                        return OllamaResponse.failure("HTTP " + response.code() + ": " + errorBody);
                    }
                    sleep(config.getOrchestratorRetryDelayMs() * attempt);
                    continue;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                OllamaApiResponse apiResponse = mapper.readValue(responseBody, OllamaApiResponse.class);

                if (config.isLogTokenUsage()) {
                    log.info("OLLAMA TOKENS | prompt={} completion={} total={} duration={}ms",
                            apiResponse.getPromptEvalCount(),
                            apiResponse.getEvalCount(),
                            apiResponse.getPromptEvalCount() + apiResponse.getEvalCount(),
                            elapsed.toMillis());
                }

                log.debug("OLLAMA RESPONSE | session={} length={} elapsed={}ms",
                        sessionId, apiResponse.getResponse().length(), elapsed.toMillis());

                return OllamaResponse.builder()
                        .content(apiResponse.getResponse())
                        .promptTokens(apiResponse.getPromptEvalCount())
                        .completionTokens(apiResponse.getEvalCount())
                        .executionTime(elapsed)
                        .success(true)
                        .build();

            } catch (IOException e) {
                log.warn("Ollama IO error on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) {
                    return OllamaResponse.failure("IO error after " + maxRetries + " retries: " + e.getMessage());
                }
                sleep(config.getOrchestratorRetryDelayMs() * attempt);
            }
        }

        MDC.remove("sessionId");
        return OllamaResponse.failure("Exhausted all retries");
    }

    /**
     * Constructs the Ollama prompt using standard instruction-following format.
     * This structure works reliably with Llama3 instruction-tuned models.
     */
    private String buildPrompt(String systemPrompt, String userPrompt) {
        return "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
                + systemPrompt.trim()
                + "<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n"
                + userPrompt.trim()
                + "<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n";
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "(null)";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...[truncated]";
    }

    // ---- Inner model classes for JSON serialization ----

    static class OllamaRequestBody {
        @JsonProperty("model")     public final String model;
        @JsonProperty("prompt")    public final String prompt;
        @JsonProperty("stream")    public final boolean stream;
        @JsonProperty("options")   public final OllamaOptions options;

        OllamaRequestBody(String model, String prompt, boolean stream, OllamaOptions options) {
            this.model = model;
            this.prompt = prompt;
            this.stream = stream;
            this.options = options;
        }
    }

    static class OllamaOptions {
        @JsonProperty("temperature")    public final double temperature;
        @JsonProperty("num_predict")    public final int numPredict;

        OllamaOptions(double temperature, int numPredict) {
            this.temperature = temperature;
            this.numPredict = numPredict;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OllamaApiResponse {
        @JsonProperty("response")           private String response = "";
        @JsonProperty("prompt_eval_count")  private int promptEvalCount;
        @JsonProperty("eval_count")         private int evalCount;
        @JsonProperty("done")               private boolean done;

        public String getResponse()        { return response; }
        public int getPromptEvalCount()    { return promptEvalCount; }
        public int getEvalCount()          { return evalCount; }
        public boolean isDone()            { return done; }
    }
}
