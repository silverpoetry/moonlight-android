package com.moonlight.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.regex.Pattern

/** Rejects reintroduction of removed fork branding and resource prefixes. */
@CacheableTask
abstract class VerifyNoLegacyBranding extends DefaultTask {
    private static final Pattern LEGACY_BRAND = Pattern.compile(
            '(?i)(?:\\baxixi\\b|\\bgamesbs\\b|game[_ -]?sbs|' +
                    '\\bfidelityfx\\b|\\bfsr\\b)')
    private static final Pattern LEGACY_RESOURCE = Pattern.compile(
            '(?i)(?:ic_axi_|icon_axi_|bg_gradient_axi_|bg_ax_|' +
                    'layout_axixi_|item_layout_axixi_|axi_keyboard_|' +
                    'ax_gamepad_|ic_app_axi|atv_banner_axi|' +
                    'keyboard_axi_|gamepad_axi_|' +
                    'mouse_model_(?:names|values)_axi|' +
                    'axi_add_computer)')
    private static final Pattern LEGACY_SHORTCUT_MARKER = Pattern.compile(
            '(?i)(?:\\baxix\\b|axi->)')
    private static final String LEGACY_ACTION_MIGRATION =
            '/LegacyVirtualControlActionMigration.java'
    private static final Set<String> TEXT_EXTENSIONS = [
            'java', 'kt', 'xml', 'gradle', 'groovy', 'json',
            'properties', 'txt', 'md', 'ps1', 'c', 'h', 'cpp',
            'mk', 'pro'
    ] as Set

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSourceFiles()

    @TaskAction
    void verifySource() {
        List<String> violations = []
        sourceFiles.files
                .findAll { it.isFile() }
                .sort { first, second -> first.path <=> second.path }
                .each { file ->
                    String relativePath = file.path
                    VerifyNoLegacyBranding.addViolation(
                            violations,
                            relativePath,
                            0,
                            relativePath)

                    String extension = file.name.contains('.')
                            ? file.name.substring(
                                    file.name.lastIndexOf('.') + 1)
                                    .toLowerCase(Locale.ROOT)
                            : ''
                    if (!TEXT_EXTENSIONS.contains(extension)) {
                        return
                    }
                    List<String> lines = Files.readAllLines(
                            file.toPath(), StandardCharsets.UTF_8)
                    for (int index = 0; index < lines.size(); index++) {
                        VerifyNoLegacyBranding.addViolation(
                                violations,
                                relativePath,
                                index + 1,
                                lines[index])
                    }
                }

        if (!violations.isEmpty()) {
            throw new GradleException(
                    'Removed fork branding is forbidden in production ' +
                            'sources and resources:\n' +
                            violations.join('\n'))
        }
    }

    private static void addViolation(
            List<String> violations,
            String path,
            int line,
            String value) {
        boolean hasLegacyBrand = LEGACY_BRAND.matcher(value).find()
        boolean hasLegacyResource = LEGACY_RESOURCE.matcher(value).find()
        boolean hasLegacyShortcutMarker =
                LEGACY_SHORTCUT_MARKER.matcher(value).find()
        if (!hasLegacyBrand &&
                !hasLegacyResource &&
                !hasLegacyShortcutMarker) {
            return
        }
        if (!hasLegacyBrand &&
                !hasLegacyResource &&
                hasLegacyShortcutMarker &&
                path.replace('\\', '/').endsWith(
                        LEGACY_ACTION_MIGRATION)) {
            return
        }
        violations.add(line == 0
                ? path
                : path + ':' + line)
    }
}
