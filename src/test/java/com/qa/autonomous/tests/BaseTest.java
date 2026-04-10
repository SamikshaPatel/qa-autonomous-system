package com.qa.autonomous.tests;

import com.microsoft.playwright.*;
import com.qa.autonomous.config.SystemConfig;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.nio.file.Paths;
import java.util.UUID;

/**
 * BaseTest — TestNG base class for all Playwright UI test classes.
 *
 * Provides:
 *   - Thread-local Playwright, Browser, BrowserContext, and Page instances
 *   - Automatic screenshot on failure (attached to Allure)
 *   - Allure environment metadata
 *   - SLF4J MDC setup per test method
 *   - Clean teardown guarantees
 *
 * Usage: extend this class in every Playwright test class.
 *        Do NOT create Playwright instances yourself.
 */
public abstract class BaseTest {

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    protected static final SystemConfig config = SystemConfig.getInstance();

    // Thread-local: safe for parallel test execution
    private static final ThreadLocal<Playwright>       playwrightTL = new ThreadLocal<>();
    private static final ThreadLocal<Browser>          browserTL    = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext>   contextTL    = new ThreadLocal<>();
    private static final ThreadLocal<Page>             pageTL       = new ThreadLocal<>();

    /** Exposed to subclasses — the active Playwright Page for this test. */
    protected Page page;

    // ---- Lifecycle ----

    @BeforeMethod(alwaysRun = true)
    public void setUpTest(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("sessionId", sessionId);
        MDC.put("testName", testName);
        log.info("=== TEST START: {} | session={} ===", testName, sessionId);

        Playwright playwright = Playwright.create();
        playwrightTL.set(playwright);

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(config.isPlaywrightHeadless())
                .setSlowMo(0);

        Browser browser;
        String browserName = config.get("playwright.browser", "chromium").toLowerCase();
        browser = switch (browserName) {
            case "firefox" -> playwright.firefox().launch(launchOptions);
            case "webkit"  -> playwright.webkit().launch(launchOptions);
            default        -> playwright.chromium().launch(launchOptions);
        };
        browserTL.set(browser);

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setIgnoreHTTPSErrors(true)
                .setRecordVideoDir(config.getBoolean("playwright.record.video", false)
                        ? Paths.get("target/videos")
                        : null));
        contextTL.set(context);

        Page newPage = context.newPage();
        pageTL.set(newPage);
        this.page = newPage;

        log.info("Browser={} headless={} | session={}", browserName, config.isPlaywrightHeadless(), sessionId);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownTest(ITestResult result) {
        String testName  = result.getMethod().getMethodName();
        boolean passed   = result.getStatus() == ITestResult.SUCCESS;
        String status    = passed ? "PASSED" : (result.getStatus() == ITestResult.FAILURE ? "FAILED" : "SKIPPED");

        log.info("=== TEST END: {} | {} ===", testName, status);

        // Capture screenshot on failure
        if (!passed && pageTL.get() != null) {
            try {
                captureScreenshot(testName);
            } catch (Exception e) {
                log.warn("Failed to capture screenshot for {}: {}", testName, e.getMessage());
            }
        }

        // Teardown in reverse order
        safeClose(pageTL.get(),      "Page",           testName);
        safeClose(contextTL.get(),   "BrowserContext", testName);
        safeClose(browserTL.get(),   "Browser",        testName);
        safeClose(playwrightTL.get(),"Playwright",     testName);

        pageTL.remove();
        contextTL.remove();
        browserTL.remove();
        playwrightTL.remove();

        MDC.remove("sessionId");
        MDC.remove("testName");
    }

    // ---- Helpers ----

    @Attachment(value = "Screenshot on Failure: {testName}", type = "image/png")
    @Step("Capture failure screenshot")
    private byte[] captureScreenshot(String testName) {
        try {
            Page currentPage = pageTL.get();
            if (currentPage == null) return new byte[0];
            byte[] screenshot = currentPage.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            log.info("Screenshot captured for failed test: {}", testName);
            return screenshot;
        } catch (Exception e) {
            log.error("Screenshot capture failed: {}", e.getMessage());
            return new byte[0];
        }
    }

    private void safeClose(AutoCloseable resource, String name, String testName) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                log.warn("Error closing {} for test {}: {}", name, testName, e.getMessage());
            }
        }
    }

    /** Utility to navigate to a URL and wait for DOM content loaded. */
    @Step("Navigate to: {url}")
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        page.navigate(url);
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
    }

    private static boolean getBoolean(SystemConfig cfg, String key, boolean def) {
        return cfg.getBoolean(key, def);
    }
}
