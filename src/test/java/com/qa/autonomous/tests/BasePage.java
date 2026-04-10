package com.qa.autonomous.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.qa.autonomous.config.SystemConfig;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BasePage — parent for all Playwright Page Objects.
 *
 * Provides:
 *   - Common navigation, wait, and element interaction utilities
 *   - Allure @Step annotations on all reusable actions
 *   - Centralised timeout configuration
 *
 * Rules for subclasses:
 *   - Declare ALL locators as private final fields, initialised in the constructor
 *   - Use assertThat(locator) in TEST classes, NOT in Page Objects
 *   - Page Object methods should return 'this' for fluency where the page stays the same
 *   - Page Object methods should return a new Page Object when navigation occurs
 */
public abstract class BasePage {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final Page page;
    protected final SystemConfig config;
    protected final String baseUrl;
    protected static final int DEFAULT_TIMEOUT_MS = 30_000;

    protected BasePage(Page page) {
        this.page    = page;
        this.config  = SystemConfig.getInstance();
        this.baseUrl = config.getPlaywrightBaseUrl();
    }

    /**
     * Navigates to the page's URL. Subclasses define getPath().
     */
    @Step("Navigate to page: {this.getClass().getSimpleName()}")
    public BasePage navigate() {
        String fullUrl = baseUrl + getPath();
        log.info("Navigating to: {}", fullUrl);
        page.navigate(fullUrl);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        waitForPageReady();
        return this;
    }

    /**
     * Override in subclasses to provide the page-specific URL path (e.g., "/login").
     */
    protected abstract String getPath();

    /**
     * Wait for the page to be in a stable state.
     * Override for pages with complex loading patterns.
     */
    protected void waitForPageReady() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    // ---- Reusable element interaction utilities ----

    @Step("Fill '{locatorDescription}' with value")
    protected void fill(Locator locator, String value, String locatorDescription) {
        log.debug("Filling '{}' with value (masked)", locatorDescription);
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT_MS));
        locator.clear();
        locator.fill(value);
    }

    @Step("Click '{locatorDescription}'")
    protected void click(Locator locator, String locatorDescription) {
        log.debug("Clicking '{}'", locatorDescription);
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT_MS));
        locator.click();
    }

    @Step("Select option '{option}' from '{locatorDescription}'")
    protected void selectOption(Locator locator, String option, String locatorDescription) {
        log.debug("Selecting '{}' from '{}'", option, locatorDescription);
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT_MS));
        locator.selectOption(option);
    }

    @Step("Wait for URL to contain: {urlFragment}")
    protected void waitForUrlContaining(String urlFragment) {
        log.debug("Waiting for URL containing: {}", urlFragment);
        page.waitForURL("**" + urlFragment + "**",
                new Page.WaitForURLOptions().setTimeout(DEFAULT_TIMEOUT_MS));
    }

    @Step("Get text of '{locatorDescription}'")
    protected String getText(Locator locator, String locatorDescription) {
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT_MS));
        String text = locator.textContent();
        log.debug("Text of '{}': {}", locatorDescription, text);
        return text != null ? text.trim() : "";
    }

    @Step("Check visibility of '{locatorDescription}'")
    protected boolean isVisible(Locator locator, String locatorDescription) {
        boolean visible = locator.isVisible();
        log.debug("'{}' visible: {}", locatorDescription, visible);
        return visible;
    }

    /** Current page title. */
    public String getTitle() {
        return page.title();
    }

    /** Current URL. */
    public String getCurrentUrl() {
        return page.url();
    }
}
