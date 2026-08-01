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

/** Verifies provenance, content, and layout of checked-in native dependencies. */
@CacheableTask
abstract class VerifyNativeDependencies extends DefaultTask {
    private static final List<String> REQUIRED_ABIS = [
            'armeabi-v7a',
            'arm64-v8a',
            'x86',
            'x86_64'
    ].asImmutable()

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
                    "Unsupported native dependency manifest schema: ${manifest.schemaVersion}")
        }

        List<Map> dependencies = manifest.dependencies as List<Map>
        if (dependencies == null || dependencies.isEmpty()) {
            throw new GradleException(
                    'Native dependency manifest must contain dependencies')
        }

        Set<Path> registeredRoots = new LinkedHashSet<>()
        for (Map dependency : dependencies) {
            verifyDependency(repository, dependency, registeredRoots)
        }
        verifyNoUnregisteredArchives(repository, registeredRoots)
        verifyNoOpaqueJavaArchives(repository)
    }

    private static void verifyDependency(
            File repository,
            Map dependency,
            Set<Path> registeredRoots) {
        String id = requireString(dependency, 'id')
        String version = requireString(dependency, 'version')
        String source = requireString(dependency, 'source')
        String sourceSha256 = requireSha256(dependency, 'sourceSha256')
        String metadataName = requireString(dependency, 'metadataName')
        String rootValue = requireString(dependency, 'root')
        String expectedTreeSha256 = requireSha256(
                dependency,
                'treeSha256')
        List<String> libraries = dependency.libraries as List<String>
        if (libraries == null || libraries.isEmpty()) {
            throw new GradleException(
                    "Native dependency $id has no library list")
        }

        Path repositoryPath = repository.toPath()
        Path rootPath = repositoryPath.resolve(rootValue).normalize()
        if (!rootPath.startsWith(repositoryPath) ||
                !Files.isDirectory(rootPath)) {
            throw new GradleException(
                    "Native dependency $id has an invalid root: $rootValue")
        }
        registeredRoots.add(rootPath)

        List<String> excluded = (dependency.excluded ?: []) as List<String>
        Set<String> excludedPaths = excluded.collect {
            normalizeRelativePath(it)
        } as Set<String>
        List<Path> files = Files.walk(rootPath).withCloseable { stream ->
            stream.filter { Files.isRegularFile(it) }
                    .filter {
                        !excludedPaths.contains(relativePath(rootPath, it))
                    }
                    .sorted { left, right ->
                        relativePath(rootPath, left) <=>
                                relativePath(rootPath, right)
                    }
                    .toList()
        }
        if (files.isEmpty()) {
            throw new GradleException(
                    "Native dependency $id contains no tracked files")
        }

        String actualTreeSha256 = treeSha256(rootPath, files)
        if (!actualTreeSha256.equalsIgnoreCase(expectedTreeSha256)) {
            throw new GradleException(
                    "Native dependency $id content changed. " +
                            "Expected tree SHA-256 $expectedTreeSha256, " +
                            "got $actualTreeSha256")
        }

        File licenseFile = rootPath.resolve(
                requireString(dependency, 'license')).toFile()
        if (!licenseFile.isFile() || licenseFile.length() == 0) {
            throw new GradleException(
                    "Native dependency $id is missing its license")
        }
        Path packagedLicense = repository.toPath().resolve(
                requireString(dependency, 'packagedLicense')).normalize()
        if (!packagedLicense.startsWith(repository.toPath()) ||
                !Files.isRegularFile(packagedLicense) ||
                Files.mismatch(licenseFile.toPath(), packagedLicense) != -1) {
            throw new GradleException(
                    "Native dependency $id license is not packaged verbatim")
        }

        File metadataFile = rootPath.resolve(
                requireString(dependency, 'metadata')).toFile()
        Map<String, String> metadata = readMetadata(metadataFile, id)
        Map<String, String> expectedMetadata = [
                name         : metadataName,
                version      : version,
                source       : source,
                source_sha256: sourceSha256,
                ndk          : requireString(dependency, 'ndk'),
                min_api      : String.valueOf(dependency.minApi),
                abis         : REQUIRED_ABIS.join(',')
        ]
        if (metadata != expectedMetadata) {
            throw new GradleException(
                    "Native dependency $id metadata mismatch. " +
                            "Expected $expectedMetadata, got $metadata")
        }

        if (!(source ==~ /^https:\/\/.+/)) {
            throw new GradleException(
                    "Native dependency $id must use an HTTPS source URL")
        }

        Set<String> expectedArchives = new LinkedHashSet<>()
        REQUIRED_ABIS.each { String abi ->
            libraries.each { String library ->
                expectedArchives.add("$abi/$library")
            }
        }
        Set<String> actualArchives = files.findAll {
            it.fileName.toString().endsWith('.a')
        }.collect {
            relativePath(rootPath, it)
        } as Set<String>
        if (actualArchives != expectedArchives) {
            throw new GradleException(
                    "Native dependency $id archive layout mismatch. " +
                            "Expected $expectedArchives, got $actualArchives")
        }
        actualArchives.each { String relative ->
            verifyArchiveMagic(rootPath.resolve(relative), id)
        }
    }

    private static void verifyNoUnregisteredArchives(
            File repository,
            Set<Path> registeredRoots) {
        Path nativeRoot = repository.toPath().resolve(
                'app/src/main/jni').normalize()
        List<String> unregistered = Files.walk(nativeRoot).withCloseable { stream ->
            stream.filter { Files.isRegularFile(it) }
                    .filter { it.fileName.toString().endsWith('.a') }
                    .filter { Path file ->
                        !registeredRoots.any { file.startsWith(it) }
                    }
                    .map { repository.toPath().relativize(it).toString() }
                    .sorted()
                    .toList()
        }
        if (!unregistered.isEmpty()) {
            throw new GradleException(
                    'Unregistered native archives are forbidden:\n' +
                            unregistered.join('\n'))
        }
    }

    private static void verifyNoOpaqueJavaArchives(File repository) {
        Path libs = repository.toPath().resolve('app/libs')
        if (!Files.isDirectory(libs)) {
            return
        }
        List<String> archives = Files.walk(libs).withCloseable { stream ->
            stream.filter { Files.isRegularFile(it) }
                    .filter {
                        String name = it.fileName.toString()
                        name.endsWith('.aar') || name.endsWith('.jar')
                    }
                    .map { repository.toPath().relativize(it).toString() }
                    .sorted()
                    .toList()
        }
        if (!archives.isEmpty()) {
            throw new GradleException(
                    'Opaque local AAR/JAR dependencies are forbidden:\n' +
                            archives.join('\n'))
        }
    }

    private static Map<String, String> readMetadata(
            File file,
            String id) {
        if (!file.isFile()) {
            throw new GradleException(
                    "Native dependency $id is missing build metadata")
        }
        Map<String, String> values = new LinkedHashMap<>()
        file.eachLine(StandardCharsets.UTF_8.name()) { String line ->
            if (line.isBlank()) {
                return
            }
            String[] parts = line.split('=', 2)
            if (parts.length != 2 || parts[0].isBlank() ||
                    values.put(parts[0], parts[1]) != null) {
                throw new GradleException(
                        "Invalid build metadata for native dependency $id")
            }
        }
        return values
    }

    private static void verifyArchiveMagic(Path file, String id) {
        byte[] expected = '!<arch>\n'.getBytes(StandardCharsets.US_ASCII)
        byte[] actual = new byte[expected.length]
        file.withInputStream { stream ->
            if (stream.read(actual) != actual.length) {
                throw new GradleException(
                        "Native dependency $id contains a truncated archive: $file")
            }
        }
        if (!Arrays.equals(actual, expected)) {
            throw new GradleException(
                    "Native dependency $id contains an invalid archive: $file")
        }
    }

    private static String treeSha256(Path root, List<Path> files) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        files.each { Path file ->
            String line = "${fileSha256(file)}  ${relativePath(root, file)}\n"
            digest.update(line.getBytes(StandardCharsets.UTF_8))
        }
        return hex(digest.digest())
    }

    private static String fileSha256(Path file) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        file.withInputStream { stream ->
            byte[] buffer = new byte[64 * 1024]
            int count
            while ((count = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, count)
            }
        }
        return hex(digest.digest())
    }

    private static String relativePath(Path root, Path file) {
        return normalizeRelativePath(root.relativize(file).toString())
    }

    private static String normalizeRelativePath(String path) {
        return path.replace('\\', '/')
    }

    private static String requireString(Map values, String key) {
        Object value = values[key]
        if (!(value instanceof String) || value.isBlank()) {
            throw new GradleException(
                    "Native dependency manifest field '$key' is required")
        }
        return value
    }

    private static String requireSha256(Map values, String key) {
        String value = requireString(values, key).toLowerCase(Locale.ROOT)
        if (!(value ==~ /^[0-9a-f]{64}$/)) {
            throw new GradleException(
                    "Native dependency manifest field '$key' must be SHA-256")
        }
        return value
    }

    private static String hex(byte[] value) {
        return value.collect { String.format('%02x', it & 0xff) }.join()
    }
}
