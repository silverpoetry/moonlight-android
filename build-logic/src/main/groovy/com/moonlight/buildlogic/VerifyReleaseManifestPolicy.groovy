package com.moonlight.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

import javax.xml.parsers.DocumentBuilderFactory

/** Verifies the security surface after all Release manifest overlays merge. */
@CacheableTask
abstract class VerifyReleaseManifestPolicy extends DefaultTask {
    private static final String ANDROID_NAMESPACE =
            'http://schemas.android.com/apk/res/android'
    private static final String PROFILE_INSTALLER_RECEIVER =
            'androidx.profileinstaller.ProfileInstallReceiver'
    private static final Map<String, String> EXPORTED_COMPONENTS = [
            'com.limelight.PosterContentProvider': '',
            'com.limelight.PcView': '',
            'com.limelight.ShortcutTrampoline': '',
            'com.limelight.FilePushActivity': '',
            (PROFILE_INSTALLER_RECEIVER): 'android.permission.DUMP'
    ] as Map<String, String>

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getManifestFile()

    @Input
    abstract Property<String> getVariantName()

    @TaskAction
    void verifyPolicy() {
        Document document = parse(manifestFile.get().asFile)
        Element application = firstElement(
                document.documentElement,
                'application')
        require(application != null, 'Merged Release manifest has no application')
        require(androidAttribute(application, 'debuggable') != 'true',
                'Release application is debuggable')
        require(androidAttribute(application, 'testOnly') != 'true',
                'Release application is marked testOnly')

        document.getElementsByTagName('profileable').each { Node node ->
            VerifyReleaseManifestPolicy.require(
                    VerifyReleaseManifestPolicy.androidAttribute(
                            (Element) node,
                            'shell') != 'true',
                    'Release application is profileable by shell')
        }

        Map<String, String> exported = [:]
        ['activity', 'service', 'provider', 'receiver'].each { tagName ->
            application.getElementsByTagName(tagName).each { Node node ->
                Element component = (Element) node
                String name = VerifyReleaseManifestPolicy.androidAttribute(
                        component,
                        'name')
                String exportedState =
                        VerifyReleaseManifestPolicy.androidAttribute(
                                component,
                                'exported')
                VerifyReleaseManifestPolicy.require(!name.isEmpty(),
                        "Unnamed ${tagName} in merged Release manifest")
                VerifyReleaseManifestPolicy.require(
                        exportedState == 'true' || exportedState == 'false',
                        "${name} has no explicit exported state after merge")
                if (exportedState == 'true') {
                    exported.put(
                            name,
                            VerifyReleaseManifestPolicy.androidAttribute(
                                    component,
                                    'permission'))
                }
            }
        }

        require(exported == EXPORTED_COMPONENTS,
                "${variantName.get()} exported surface changed: ${exported}")

        Element game = findComponent(application, 'activity',
                'com.limelight.Game')
        require(game != null && androidAttribute(game, 'exported') == 'false',
                'Debug stream entry point leaked into Release')
        require(findComponent(
                application,
                'provider',
                'com.limelight.nvstream.filetransfer.' +
                        'GenericContentProvider') == null,
                'Debug file-transfer provider leaked into Release')
    }

    private static Element findComponent(
            Element application,
            String tagName,
            String className) {
        return application.getElementsByTagName(tagName)
                .find { Node node ->
                    VerifyReleaseManifestPolicy.androidAttribute(
                            (Element) node,
                            'name') == className
                } as Element
    }

    private static Element firstElement(Element parent, String tagName) {
        for (Node node = parent.firstChild;
             node != null;
             node = node.nextSibling) {
            if (node.nodeType == Node.ELEMENT_NODE &&
                    node.nodeName == tagName) {
                return (Element) node
            }
        }
        return null
    }

    private static String androidAttribute(Element element, String name) {
        return element.getAttributeNS(ANDROID_NAMESPACE, name)
    }

    private static Document parse(File file) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        factory.namespaceAware = true
        factory.setFeature(
                'http://apache.org/xml/features/disallow-doctype-decl',
                true)
        factory.setFeature(
                'http://xml.org/sax/features/external-general-entities',
                false)
        factory.setFeature(
                'http://xml.org/sax/features/external-parameter-entities',
                false)
        return factory.newDocumentBuilder().parse(file)
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GradleException(message)
        }
    }
}
