package com.qa.autonomous.util;

import com.qa.autonomous.config.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Writes LLM-generated Java source files to the configured output directory.
 * Handles package-to-path conversion and file overwrite safety.
 */
public final class GeneratedFileWriter {

    private static final Logger log = LoggerFactory.getLogger(GeneratedFileWriter.class);

    private GeneratedFileWriter() {}

    /**
     * Writes Java code to the correct file path derived from its package + class name.
     *
     * @param javaCode    The complete Java source code string.
     * @param sessionId   For audit logging.
     * @return            The path where the file was written, or null on failure.
     */
    public static Path writeGeneratedClass(String javaCode, String sessionId) {
        if (javaCode == null || javaCode.isBlank()) {
            log.warn("[{}] Skipping file write — code is blank", sessionId);
            return null;
        }

        String packageName = CodeExtractor.extractPackageName(javaCode);
        String className   = CodeExtractor.extractClassName(javaCode);

        if (className == null) {
            log.warn("[{}] Could not extract class name from generated code", sessionId);
            className = "GeneratedTest_" + System.currentTimeMillis();
        }

        String baseOutputDir = SystemConfig.getInstance().getCodeGenOutputDir();
        String packagePath   = packageName != null
                ? packageName.replace('.', '/')
                : "com/qa/autonomous/generated";

        Path outputDir  = Paths.get(baseOutputDir, packagePath.split("/"));
        Path outputFile = outputDir.resolve(className + ".java");

        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputFile, javaCode, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("[{}] Generated file written: {}", sessionId, outputFile.toAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            log.error("[{}] Failed to write generated file {}: {}", sessionId, outputFile, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Writes any text content to a specified output file path.
     */
    public static Path writeTextFile(String content, String relativePath, String sessionId) {
        Path outputFile = Paths.get(relativePath);
        try {
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("[{}] File written: {}", sessionId, outputFile.toAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            log.error("[{}] Failed to write file {}: {}", sessionId, relativePath, e.getMessage(), e);
            return null;
        }
    }
}
