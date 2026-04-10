package com.qa.autonomous.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for extracting code blocks from LLM markdown output.
 * LLMs wrap code in ``` fences — this class extracts them reliably.
 */
public final class CodeExtractor {

    private static final Logger log = LoggerFactory.getLogger(CodeExtractor.class);

    // Matches ```java ... ``` or ``` ... ``` blocks
    private static final Pattern JAVA_FENCE   = Pattern.compile("```java\\s*\\n([\\s\\S]*?)\\n```", Pattern.MULTILINE);
    private static final Pattern GENERIC_FENCE = Pattern.compile("```\\s*\\n([\\s\\S]*?)\\n```", Pattern.MULTILINE);
    private static final Pattern XML_FENCE    = Pattern.compile("```xml\\s*\\n([\\s\\S]*?)\\n```", Pattern.MULTILINE);
    private static final Pattern YAML_FENCE   = Pattern.compile("```ya?ml\\s*\\n([\\s\\S]*?)\\n```", Pattern.MULTILINE);

    private CodeExtractor() {}

    /**
     * Extracts the first Java code block from LLM output.
     * Returns null if no Java block is found.
     */
    public static String extractFirstJavaBlock(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) return null;

        Matcher m = JAVA_FENCE.matcher(llmOutput);
        if (m.find()) {
            String code = m.group(1).trim();
            log.debug("Extracted Java block: {} chars", code.length());
            return code;
        }

        // Fallback: try generic fence if it looks like Java
        Matcher gm = GENERIC_FENCE.matcher(llmOutput);
        while (gm.find()) {
            String block = gm.group(1).trim();
            if (block.contains("package ") || block.contains("import ") || block.contains("public class ")) {
                log.debug("Extracted Java block from generic fence: {} chars", block.length());
                return block;
            }
        }

        log.warn("No Java code block found in LLM output (length={})", llmOutput.length());
        return null;
    }

    /**
     * Extracts all Java code blocks from LLM output.
     */
    public static List<String> extractAllJavaBlocks(String llmOutput) {
        List<String> blocks = new ArrayList<>();
        if (llmOutput == null || llmOutput.isBlank()) return blocks;

        Matcher m = JAVA_FENCE.matcher(llmOutput);
        while (m.find()) {
            blocks.add(m.group(1).trim());
        }
        log.debug("Extracted {} Java blocks", blocks.size());
        return blocks;
    }

    /**
     * Extracts the first XML block from LLM output.
     */
    public static String extractFirstXmlBlock(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) return null;
        Matcher m = XML_FENCE.matcher(llmOutput);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * Extracts the first YAML block from LLM output.
     */
    public static String extractFirstYamlBlock(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) return null;
        Matcher m = YAML_FENCE.matcher(llmOutput);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * Checks if extracted code contains a compilable Java class declaration.
     */
    public static boolean looksLikeValidJava(String code) {
        if (code == null || code.isBlank()) return false;
        return code.contains("public class ") || code.contains("public abstract class ") ||
               code.contains("public interface ") || code.contains("public enum ");
    }

    /**
     * Extracts the class name from a Java source string.
     */
    public static String extractClassName(String javaCode) {
        if (javaCode == null) return null;
        Pattern classPattern = Pattern.compile("public(?:\\s+(?:abstract|final))?\\s+class\\s+(\\w+)");
        Matcher m = classPattern.matcher(javaCode);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Extracts the package name from a Java source string.
     */
    public static String extractPackageName(String javaCode) {
        if (javaCode == null) return null;
        Pattern pkgPattern = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
        Matcher m = pkgPattern.matcher(javaCode);
        return m.find() ? m.group(1) : null;
    }
}
