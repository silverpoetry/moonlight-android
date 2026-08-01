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

/** Rejects invisible Unicode controls that can make source review deceptive. */
@CacheableTask
abstract class VerifyNoBidiControls extends DefaultTask {
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSourceFiles()

    @TaskAction
    void verifySourceText() {
        List<String> violations = []
        sourceFiles.files
                .findAll { it.isFile() }
                .sort { first, second -> first.path <=> second.path }
                .each { file ->
                    String source = Files.readString(
                            file.toPath(), StandardCharsets.UTF_8)
                    int line = 1
                    for (int index = 0; index < source.length(); index++) {
                        int character = source.charAt(index)
                        if (VerifyNoBidiControls
                                .isBidirectionalControl(character)) {
                            violations.add(String.format(
                                    '%s:%d contains U+%04X',
                                    project.relativePath(file),
                                    line,
                                    character))
                        }
                        if (character == '\n') {
                            line++
                        }
                    }
                }

        if (!violations.isEmpty()) {
            throw new GradleException(
                    'Bidirectional Unicode controls are forbidden in source:\n' +
                            violations.join('\n'))
        }
    }

    static boolean isBidirectionalControl(int character) {
        return character == 0x061C ||
                character == 0x200E ||
                character == 0x200F ||
                (character >= 0x202A && character <= 0x202E) ||
                (character >= 0x2066 && character <= 0x2069)
    }
}
