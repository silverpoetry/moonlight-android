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

import java.nio.file.Path

/** Rejects repository-local credential and signing artifacts. */
@CacheableTask
abstract class VerifyForbiddenFiles extends DefaultTask {
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getForbiddenFiles()

    @Internal
    abstract DirectoryProperty getRepositoryDirectory()

    @TaskAction
    void verifyFiles() {
        Path repositoryRoot = repositoryDirectory.get().asFile.toPath()
        List<String> violations = forbiddenFiles.files
                .findAll { it.isFile() }
                .collect { repositoryRoot.relativize(it.toPath())
                        .toString()
                        .replace('\\', '/') }
                .sort()
        if (!violations.isEmpty()) {
            throw new GradleException(
                    'Credential or signing files must not live in the repository:\n' +
                            violations.join('\n'))
        }
    }
}
