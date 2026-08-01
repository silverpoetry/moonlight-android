package com.moonlight.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Rejects repository-local credential and signing artifacts. */
@CacheableTask
abstract class VerifyForbiddenFiles extends DefaultTask {
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getForbiddenFiles()

    @TaskAction
    void verifyFiles() {
        List<String> violations = forbiddenFiles.files
                .findAll { it.isFile() }
                .collect { project.relativePath(it) }
                .sort()
        if (!violations.isEmpty()) {
            throw new GradleException(
                    'Credential or signing files must not live in the repository:\n' +
                            violations.join('\n'))
        }
    }
}
