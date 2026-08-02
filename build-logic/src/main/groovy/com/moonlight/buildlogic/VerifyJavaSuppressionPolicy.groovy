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

/** Keeps unsafe Java warning suppressions narrow and reviewable. */
@CacheableTask
abstract class VerifyJavaSuppressionPolicy extends DefaultTask {
    private static final Set<String> ALLOWED_UNCHECKED_CAST_PATHS = [
            '/core/settings/src/main/java/com/limelight/settings/SettingKey.java',
            '/app/src/main/java/com/limelight/preferences/SettingsItem.java'
    ] as Set<String>
    private static final String APP_LOCALE_PATH =
            '/app/src/main/java/com/limelight/settings/android/' +
                    'AndroidAppLocale.java'
    private static final Pattern UNCHECKED_SUPPRESSION = Pattern.compile(
            '@SuppressWarnings\\s*\\([^)]*\\bunchecked\\b[^)]*\\)')
    private static final Pattern STALE_INFLATED_ID_SUPPRESSION =
            Pattern.compile(
                    '@SuppressLint\\s*\\(\\s*"MissingInflatedId"\\s*\\)')
    private static final Pattern UNSAFE_RECEIVER_SUPPRESSION =
            Pattern.compile(
                    '@SuppressLint\\s*\\(\\s*"' +
                            'UnspecifiedRegisterReceiverFlag"\\s*\\)')
    private static final Pattern APP_BUNDLE_LOCALE_SUPPRESSION =
            Pattern.compile(
                    '@SuppressLint\\s*\\(\\s*"' +
                            'AppBundleLocaleChanges"\\s*\\)')

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
                    String source = Files.readString(
                            file.toPath(),
                            StandardCharsets.UTF_8)
                    if (STALE_INFLATED_ID_SUPPRESSION
                            .matcher(source).find()) {
                        violations.add(
                                VerifyJavaSuppressionPolicy.relativePath(
                                        repositoryRoot, file) +
                                        ': stale MissingInflatedId suppression')
                    }
                    if (UNSAFE_RECEIVER_SUPPRESSION
                            .matcher(source).find()) {
                        violations.add(
                                VerifyJavaSuppressionPolicy.relativePath(
                                        repositoryRoot, file) +
                                        ': dynamic receivers must declare ' +
                                        'an exported state through the ' +
                                        'compatibility API')
                    }
                    if (APP_BUNDLE_LOCALE_SUPPRESSION
                            .matcher(source).find() &&
                            !path.endsWith(APP_LOCALE_PATH)) {
                        violations.add(
                                VerifyJavaSuppressionPolicy.relativePath(
                                        repositoryRoot, file) +
                                        ': AppBundleLocaleChanges is allowed ' +
                                        'only at the reviewed locale adapter')
                    }
                    if (UNCHECKED_SUPPRESSION.matcher(source).find() &&
                            !ALLOWED_UNCHECKED_CAST_PATHS.any {
                                path.endsWith(it)
                            }) {
                        violations.add(
                                VerifyJavaSuppressionPolicy.relativePath(
                                        repositoryRoot, file) +
                                        ': unchecked suppression outside ' +
                                        'the reviewed erasure bridges')
                    }
                }

        if (!violations.isEmpty()) {
            throw new GradleException(
                    'Java suppressions must remain narrow and reviewed:\n' +
                            violations.join('\n'))
        }
    }

    private static String relativePath(Path repositoryRoot, File file) {
        return repositoryRoot.relativize(file.toPath())
                .toString()
                .replace('\\', '/')
    }
}
