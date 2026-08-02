package com.moonlight.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

/** Enforces the debug-gated logging boundary for production application code. */
@CacheableTask
abstract class VerifyProductionLoggingPolicy extends DefaultTask {
    private static final String DEBUG_LOG_PATH =
            '/com/limelight/DebugLog.java'
    private static final String LIME_LOG_PATH =
            '/com/limelight/LimeLog.java'
    private static final String SHIELD_LOG_PATH =
            '/ShieldControllerLog.java'
    private static final String NATIVE_LOG_PATH =
            '/app/src/main/jni/moonlight_native_log.h'
    private static final Pattern STACK_TRACE = Pattern.compile(
            '\\.printStackTrace\\s*\\(')
    private static final Pattern STANDARD_CONSOLE = Pattern.compile(
            '\\bSystem\\s*\\.\\s*(?:out|err)\\s*\\.')
    private static final Pattern ANDROID_LOG = Pattern.compile(
            '\\b(?:import\\s+android\\.util\\.Log\\s*;|' +
                    'android\\.util\\.Log\\s*\\.)')
    private static final Pattern JAVA_LOGGING = Pattern.compile(
            '\\b(?:import\\s+java\\.util\\.logging\\.|' +
                    'java\\.util\\.logging\\.)')
    private static final Pattern NATIVE_ANDROID_LOG = Pattern.compile(
            '(?:#\\s*include\\s*<android/log\\.h>|' +
                    '__android_log_(?:print|vprint|write)\\s*\\()')

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSourceFiles()

    @Internal
    abstract DirectoryProperty getRepositoryDirectory()

    @TaskAction
    void verifyPolicy() {
        List<String> violations = []
        Path repositoryRoot = repositoryDirectory.get().asFile.toPath()
        sourceFiles.files
                .findAll { it.isFile() }
                .sort { first, second -> first.path <=> second.path }
                .each { file ->
                    String path = file.path.replace('\\', '/')
                    List<String> lines = Files.readAllLines(
                            file.toPath(),
                            StandardCharsets.UTF_8)
                    for (int index = 0; index < lines.size(); index++) {
                        String line = lines[index]
                        boolean forbidden =
                                STACK_TRACE.matcher(line).find() ||
                                STANDARD_CONSOLE.matcher(line).find() ||
                                (!VerifyProductionLoggingPolicy
                                        .isAndroidLogBoundary(path) &&
                                        ANDROID_LOG.matcher(line).find()) ||
                                (!path.endsWith(LIME_LOG_PATH) &&
                                        JAVA_LOGGING.matcher(line).find()) ||
                                (!path.endsWith(NATIVE_LOG_PATH) &&
                                        NATIVE_ANDROID_LOG
                                                .matcher(line).find())
                        if (forbidden) {
                            violations.add(
                                    VerifyProductionLoggingPolicy.relativePath(
                                            repositoryRoot, file) + ':' +
                                            (index + 1))
                        }
                    }
                }

        if (!violations.isEmpty()) {
            throw new GradleException(
                    'Production logging must use an approved debug-gated ' +
                            'Java or native boundary:\n' +
                            violations.join('\n'))
        }
    }

    private static String relativePath(Path repositoryRoot, File file) {
        return repositoryRoot.relativize(file.toPath())
                .toString()
                .replace('\\', '/')
    }

    private static boolean isAndroidLogBoundary(String path) {
        return path.endsWith(DEBUG_LOG_PATH) ||
                path.endsWith(SHIELD_LOG_PATH)
    }
}
