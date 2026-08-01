package com.moonlight.buildlogic

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Adds checked-in native and source dependencies to the Gradle CycloneDX BOM. */
@CacheableTask
abstract class GenerateApplicationSbom extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getGradleSbom()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getNativeManifest()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getVendoredManifest()

    @OutputFile
    abstract RegularFileProperty getOutputFile()

    @TaskAction
    void generate() {
        JsonSlurper slurper = new JsonSlurper()
        Map bom = slurper.parse(gradleSbom.get().asFile) as Map
        Map nativeDependencies = slurper.parse(
                nativeManifest.get().asFile) as Map
        Map vendoredDependencies = slurper.parse(
                vendoredManifest.get().asFile) as Map

        if (bom.bomFormat != 'CycloneDX' || bom.specVersion != '1.6') {
            throw new GradleException(
                    'Expected a CycloneDX 1.6 Gradle component BOM')
        }

        List<Map> components = bom.components as List<Map>
        List<Map> dependencyGraph = bom.dependencies as List<Map>
        if (components == null || dependencyGraph == null) {
            throw new GradleException(
                    'Gradle component BOM has no component graph')
        }

        Map shieldModule = components.find { Map component ->
            component.name == 'shield-controller-extensions' &&
                    String.valueOf(component.purl)
                            .contains('project_path=%3Acore%3A' +
                                    'shield-controller-extensions')
        }
        if (shieldModule == null) {
            throw new GradleException(
                    'Gradle component BOM is missing application modules')
        }
        String applicationRef = (bom.metadata as Map)
                .component['bom-ref'] as String

        List<String> nativeRefs = []
        for (Map dependency :
                nativeDependencies.dependencies as List<Map>) {
            Map component = nativeComponent(dependency)
            addUniqueComponent(components, component)
            nativeRefs.add(component['bom-ref'] as String)
            ensureDependencyNode(dependencyGraph,
                    component['bom-ref'] as String)
        }
        appendDependencies(
                dependencyGraph,
                applicationRef,
                nativeRefs)

        for (Map dependency :
                vendoredDependencies.dependencies as List<Map>) {
            Map component = vendoredComponent(dependency)
            addUniqueComponent(components, component)
            String componentRef = component['bom-ref'] as String
            ensureDependencyNode(dependencyGraph, componentRef)
            appendDependencies(
                    dependencyGraph,
                    shieldModule['bom-ref'] as String,
                    [componentRef])
        }

        components.sort { Map left, Map right ->
            String.valueOf(left['bom-ref']) <=>
                    String.valueOf(right['bom-ref'])
        }
        dependencyGraph.sort { Map left, Map right ->
            String.valueOf(left.ref) <=> String.valueOf(right.ref)
        }

        verifyReferences(bom)
        File output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.setText(
                JsonOutput.prettyPrint(JsonOutput.toJson(bom)) + '\n',
                'UTF-8')
    }

    private static Map nativeComponent(Map dependency) {
        String id = required(dependency, 'id')
        String name = required(dependency, 'metadataName')
        String version = required(dependency, 'version')
        String purl = "pkg:generic/$id@$version"
        return [
                type              : 'library',
                'bom-ref'         : purl,
                name              : name,
                version           : version,
                hashes            : [[
                        alg    : 'SHA-256',
                        content: required(dependency, 'sourceSha256')
                ]],
                licenses          : [[
                        license: [id: nativeLicenseId(id)]
                ]],
                purl              : purl,
                modified          : false,
                externalReferences: [[
                        type: 'distribution',
                        url : required(dependency, 'source')
                ]],
                properties        : provenanceProperties(dependency)
        ]
    }

    private static Map vendoredComponent(Map dependency) {
        String id = required(dependency, 'id')
        String version = required(dependency, 'version')
        String purl = "pkg:github/cgutman/$id@$version"
        return [
                type              : 'library',
                'bom-ref'         : purl,
                name              : required(dependency, 'name'),
                version           : version,
                licenses          : [[
                        license: [id: required(dependency, 'licenseId')]
                ]],
                purl              : purl,
                modified          : true,
                externalReferences: [[
                        type: 'vcs',
                        url : required(dependency, 'source') + '#' +
                                required(dependency, 'commit')
                ]],
                properties        : provenanceProperties(dependency) + [[
                        name : 'moonlight:upstream-commit',
                        value: required(dependency, 'commit')
                ]]
        ]
    }

    private static List<Map> provenanceProperties(Map dependency) {
        return [[
                name : 'moonlight:checked-in-tree-sha256',
                value: required(dependency, 'treeSha256')
        ]]
    }

    private static String nativeLicenseId(String id) {
        switch (id) {
            case 'openssl':
                return 'Apache-2.0'
            case 'libopus':
                return 'BSD-3-Clause'
            default:
                throw new GradleException(
                        "Native dependency $id has no SBOM license mapping")
        }
    }

    private static void addUniqueComponent(
            List<Map> components,
            Map component) {
        String ref = component['bom-ref'] as String
        if (components.any { it['bom-ref'] == ref }) {
            throw new GradleException("Duplicate SBOM component: $ref")
        }
        components.add(component)
    }

    private static void ensureDependencyNode(
            List<Map> graph,
            String ref) {
        if (!graph.any { it.ref == ref }) {
            graph.add([ref: ref, dependsOn: []])
        }
    }

    private static void appendDependencies(
            List<Map> graph,
            String ownerRef,
            List<String> addedRefs) {
        Map owner = graph.find { it.ref == ownerRef }
        if (owner == null) {
            owner = [ref: ownerRef, dependsOn: []]
            graph.add(owner)
        }
        Set<String> refs = new TreeSet<>((owner.dependsOn ?: []) as List)
        refs.addAll(addedRefs)
        owner.dependsOn = refs as List
    }

    private static void verifyReferences(Map bom) {
        Set<String> components = (bom.components as List<Map>).collect {
            it['bom-ref'] as String
        } as Set<String>
        String root = (bom.metadata as Map).component['bom-ref'] as String
        Set<String> validRefs = new LinkedHashSet<>(components)
        validRefs.add(root)

        (bom.dependencies as List<Map>).each { Map node ->
            if (!validRefs.contains(node.ref)) {
                throw new GradleException(
                        "SBOM dependency node has no component: ${node.ref}")
            }
            (node.dependsOn ?: []).each { String ref ->
                if (!validRefs.contains(ref)) {
                    throw new GradleException(
                            "SBOM dependency edge has no component: $ref")
                }
            }
        }
    }

    private static String required(Map values, String key) {
        Object value = values[key]
        if (!(value instanceof String) || value.isBlank()) {
            throw new GradleException("SBOM manifest field '$key' is required")
        }
        return value
    }
}
