package com.moonlight.buildlogic

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/** Verifies source, license, and content of dependencies maintained in-tree. */
@CacheableTask
abstract class VerifyVendoredDependencies extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getManifestFile()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getDependencyFiles()

    @Internal
    abstract DirectoryProperty getRepositoryDirectory()

    @TaskAction
    void verifyDependencies() {
        File repository = repositoryDirectory.get().asFile.canonicalFile
        Map manifest = new JsonSlurper().parse(
                manifestFile.get().asFile) as Map
        if (manifest.schemaVersion != 1) {
            throw new GradleException(
                    "Unsupported vendored dependency manifest schema: " +
                            manifest.schemaVersion)
        }

        List<Map> dependencies = manifest.dependencies as List<Map>
        if (dependencies == null || dependencies.isEmpty()) {
            throw new GradleException(
                    'Vendored dependency manifest must contain dependencies')
        }

        Set<String> ids = new LinkedHashSet<>()
        for (Map dependency : dependencies) {
            String id = requireString(dependency, 'id')
            if (!ids.add(id)) {
                throw new GradleException(
                        "Duplicate vendored dependency id: $id")
            }
            verifyDependency(repository, dependency)
        }
    }

    private static void verifyDependency(File repository, Map dependency) {
        String id = requireString(dependency, 'id')
        String name = requireString(dependency, 'name')
        String version = requireString(dependency, 'version')
        String source = requireString(dependency, 'source')
        String commit = requireString(dependency, 'commit')
                .toLowerCase(Locale.ROOT)
        String licenseId = requireString(dependency, 'licenseId')
        String expectedTreeSha256 = requireSha256(
                dependency,
                'treeSha256')

        if (!(source ==~ /^https:\/\/.+/)) {
            throw new GradleException(
                    "Vendored dependency $id must use an HTTPS source URL")
        }
        if (!(commit ==~ /^[0-9a-f]{40}$/)) {
            throw new GradleException(
                    "Vendored dependency $id must pin a full Git commit")
        }

        Path repositoryPath = repository.toPath()
        Path root = repositoryPath.resolve(
                requireString(dependency, 'root')).normalize()
        if (!root.startsWith(repositoryPath) || !Files.isDirectory(root)) {
            throw new GradleException(
                    "Vendored dependency $id has an invalid root: $root")
        }

        List<Path> files = Files.walk(root).withCloseable { stream ->
            stream.filter { Files.isRegularFile(it) }
                    .filter { !relativePath(root, it).startsWith('build/') }
                    .sorted { Path left, Path right ->
                        relativePath(root, left) <=> relativePath(root, right)
                    }
                    .toList()
        }
        if (files.isEmpty()) {
            throw new GradleException(
                    "Vendored dependency $id contains no tracked files")
        }

        String actualTreeSha256 = treeSha256(root, files)
        if (!actualTreeSha256.equalsIgnoreCase(expectedTreeSha256)) {
            throw new GradleException(
                    "Vendored dependency $id content changed. " +
                            "Expected tree SHA-256 $expectedTreeSha256, " +
                            "got $actualTreeSha256")
        }

        Path license = root.resolve(
                requireString(dependency, 'license')).normalize()
        Path packagedLicense = repositoryPath.resolve(
                requireString(dependency, 'packagedLicense')).normalize()
        if (!license.startsWith(root) || !Files.isRegularFile(license) ||
                Files.size(license) == 0 ||
                !packagedLicense.startsWith(repositoryPath) ||
                !Files.isRegularFile(packagedLicense) ||
                Files.mismatch(license, packagedLicense) != -1) {
            throw new GradleException(
                    "Vendored dependency $id license is not packaged verbatim")
        }

        Path metadata = root.resolve(
                requireString(dependency, 'metadata')).normalize()
        Map<String, String> actualMetadata = readMetadata(metadata, id)
        Map<String, String> expectedMetadata = [
                name   : name,
                version: version,
                source : source,
                commit : commit,
                license: licenseId
        ]
        if (actualMetadata != expectedMetadata) {
            throw new GradleException(
                    "Vendored dependency $id metadata mismatch. " +
                            "Expected $expectedMetadata, got $actualMetadata")
        }
    }

    private static Map<String, String> readMetadata(Path file, String id) {
        if (!Files.isRegularFile(file)) {
            throw new GradleException(
                    "Vendored dependency $id is missing upstream metadata")
        }
        Map<String, String> values = new LinkedHashMap<>()
        Files.readAllLines(file, StandardCharsets.UTF_8).each { String line ->
            if (line.isBlank()) {
                return
            }
            String[] parts = line.split('=', 2)
            if (parts.length != 2 || parts[0].isBlank() ||
                    values.put(parts[0], parts[1]) != null) {
                throw new GradleException(
                        "Invalid upstream metadata for vendored dependency $id")
            }
        }
        return values
    }

    private static String treeSha256(Path root, List<Path> files) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        files.each { Path file ->
            String line = "${canonicalTextSha256(file)}  ${relativePath(root, file)}\n"
            digest.update(line.getBytes(StandardCharsets.UTF_8))
        }
        return hex(digest.digest())
    }

    /** Hashes vendored source in the canonical LF form used by Git. */
    private static String canonicalTextSha256(Path file) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        String text = Files.readString(file, StandardCharsets.UTF_8)
                .replace('\r\n', '\n')
                .replace('\r', '\n')
        digest.update(text.getBytes(StandardCharsets.UTF_8))
        return hex(digest.digest())
    }

    private static String relativePath(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/')
    }

    private static String requireString(Map values, String key) {
        Object value = values[key]
        if (!(value instanceof String) || value.isBlank()) {
            throw new GradleException(
                    "Vendored dependency manifest field '$key' is required")
        }
        return value
    }

    private static String requireSha256(Map values, String key) {
        String value = requireString(values, key).toLowerCase(Locale.ROOT)
        if (!(value ==~ /^[0-9a-f]{64}$/)) {
            throw new GradleException(
                    "Vendored dependency manifest field '$key' must be SHA-256")
        }
        return value
    }

    private static String hex(byte[] value) {
        return value.collect { String.format('%02x', it & 0xff) }.join()
    }
}
