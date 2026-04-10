package com.qa.autonomous.tests;

import com.qa.autonomous.config.SystemConfig;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.util.UUID;

/**
 * BaseApiTest — TestNG base class for all REST Assured API test classes.
 *
 * Provides:
 *   - Pre-configured RequestSpecification (base URI, content-type, Allure filter)
 *   - Pre-configured ResponseSpecification (logging on failure)
 *   - SLF4J MDC per-test tracing
 *   - Token/auth helpers for authenticated endpoints
 *
 * Usage: extend this class in every REST Assured test class.
 *        Use 'given(requestSpec)' as your starting point.
 */
public abstract class BaseApiTest {

    private static final Logger log = LoggerFactory.getLogger(BaseApiTest.class);
    protected static final SystemConfig config = SystemConfig.getInstance();

    protected RequestSpecification requestSpec;
    protected ResponseSpecification responseSpec;

    @BeforeSuite(alwaysRun = true)
    public void configureSuite() {
        RestAssured.baseURI = config.getRestAssuredBaseUri();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        log.info("REST Assured configured | baseURI={}", RestAssured.baseURI);
    }

    @BeforeMethod(alwaysRun = true)
    public void setUpApiTest(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("sessionId", sessionId);
        MDC.put("testName", testName);
        log.info("=== API TEST START: {} | session={} ===", testName, sessionId);

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(config.getRestAssuredBaseUri())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .addFilter(new RequestLoggingFilter(LogDetail.ALL))
                .addFilter(new ResponseLoggingFilter(LogDetail.ALL))
                .build();

        responseSpec = new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .build();

        customiseRequestSpec(requestSpec);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownApiTest(ITestResult result) {
        String status = result.getStatus() == ITestResult.SUCCESS ? "PASSED" : "FAILED";
        log.info("=== API TEST END: {} | {} ===", result.getMethod().getMethodName(), status);
        MDC.remove("sessionId");
        MDC.remove("testName");
    }

    /**
     * Override in subclasses to add auth tokens, custom headers, etc.
     * Called automatically in @BeforeMethod after the base spec is built.
     */
    protected void customiseRequestSpec(RequestSpecification spec) {
        // Default: no auth. Subclasses add: spec.header("Authorization", "Bearer " + getToken())
    }

    /**
     * Helper: creates a Bearer token header value.
     */
    protected RequestSpecification withBearer(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(requestSpec)
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    /**
     * Helper: creates an API key header.
     */
    protected RequestSpecification withApiKey(String headerName, String apiKey) {
        return new RequestSpecBuilder()
                .addRequestSpecification(requestSpec)
                .addHeader(headerName, apiKey)
                .build();
    }
}
