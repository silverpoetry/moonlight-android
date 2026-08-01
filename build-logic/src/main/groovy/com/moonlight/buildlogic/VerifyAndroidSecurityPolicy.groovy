package com.moonlight.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

import javax.xml.parsers.DocumentBuilderFactory

/** Locks the reviewed Android component, sharing, backup, and trust surface. */
@CacheableTask
abstract class VerifyAndroidSecurityPolicy extends DefaultTask {
    private static final String ANDROID_NAMESPACE =
            'http://schemas.android.com/apk/res/android'

    private static final Set<String> EXPORTED_COMPONENTS = [
            '.PosterContentProvider',
            '.PcView',
            '.ShortcutTrampoline',
            '.FilePushActivity'
    ] as Set

    private static final Set<String> SENSITIVE_BACKUP_EXCLUSIONS = [
            'sharedpref:.',
            'database:.',
            'file:client.key',
            'file:client.crt',
            'file:uniqueid'
    ] as Set

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getManifestFile()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getFileProviderPathsFile()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getLegacyBackupRulesFile()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getModernBackupRulesFile()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getNetworkSecurityConfigFile()

    @TaskAction
    void verifyPolicy() {
        verifyManifest(parse(manifestFile.get().asFile))
        verifyFileProvider(parse(fileProviderPathsFile.get().asFile))
        verifyLegacyBackup(parse(legacyBackupRulesFile.get().asFile))
        verifyModernBackup(parse(modernBackupRulesFile.get().asFile))
        verifyNetworkSecurity(parse(networkSecurityConfigFile.get().asFile))
    }

    private static void verifyManifest(Document document) {
        Element application = firstElement(document.documentElement, 'application')
        require(application != null, 'Android manifest has no application element')
        require(androidAttribute(application, 'allowBackup') == 'true',
                'Automatic backup policy must remain explicit')
        require(androidAttribute(application, 'fullBackupContent') == '@xml/backup_rules',
                'Legacy backup rules are not wired to the application')
        require(androidAttribute(application, 'dataExtractionRules') == '@xml/backup_rules_s',
                'Modern backup rules are not wired to the application')
        require(androidAttribute(application, 'networkSecurityConfig') ==
                '@xml/network_security_config',
                'Network security policy is not wired to the application')

        Set<String> actuallyExported = [] as Set
        ['activity', 'service', 'provider', 'receiver'].each { tagName ->
            application.getElementsByTagName(tagName).each { Node node ->
                Element component = (Element) node
                String name = androidAttribute(component, 'name')
                String exported = androidAttribute(component, 'exported')
                require(!name.isEmpty(), "Unnamed ${tagName} in Android manifest")
                require(exported == 'true' || exported == 'false',
                        "${name} must declare android:exported explicitly")
                if (exported == 'true') {
                    actuallyExported.add(name)
                }
            }
        }
        require(actuallyExported == EXPORTED_COMPONENTS,
                "Exported component surface changed: ${actuallyExported}")
    }

    private static void verifyFileProvider(Document document) {
        Set<String> actualPaths = childElements(document.documentElement)
                .collect { Element element ->
                    ("${element.tagName}:${element.getAttribute('name')}:" +
                            element.getAttribute('path')).toString()
                } as Set
        Set<String> expectedPaths = [
                'cache-path:clipboard:clipboard/',
                'cache-path:outbound_shares:outbound-shares/'
        ] as Set
        require(actualPaths == expectedPaths,
                "FileProvider path surface changed: ${actualPaths}")
    }

    private static void verifyLegacyBackup(Document document) {
        Set<String> actualExclusions = exclusions(document.documentElement)
        require(actualExclusions.containsAll(SENSITIVE_BACKUP_EXCLUSIONS),
                'Legacy backup rules include device-bound credentials or hosts: ' +
                        actualExclusions)
    }

    private static void verifyModernBackup(Document document) {
        Element root = document.documentElement
        Element cloudBackup = firstElement(root, 'cloud-backup')
        Element deviceTransfer = firstElement(root, 'device-transfer')
        require(cloudBackup != null,
                'Modern backup rules have no cloud-backup policy')
        require(deviceTransfer != null,
                'Modern backup rules have no device-transfer policy')
        require(cloudBackup.getAttribute('disableIfNoEncryptionCapabilities') ==
                'true',
                'Cloud backup must require encryption capability')
        Set<String> cloudExclusions = exclusions(cloudBackup)
        Set<String> transferExclusions = exclusions(deviceTransfer)
        require(cloudExclusions.containsAll(SENSITIVE_BACKUP_EXCLUSIONS),
                'Cloud backup includes device-bound credentials or hosts: ' +
                        cloudExclusions)
        require(transferExclusions.containsAll(SENSITIVE_BACKUP_EXCLUSIONS),
                'Device transfer includes device-bound credentials or hosts: ' +
                        transferExclusions)
    }

    private static void verifyNetworkSecurity(Document document) {
        List<Element> rootChildren = childElements(document.documentElement)
        require(rootChildren.size() == 1 &&
                rootChildren.first().tagName == 'base-config',
                'Network security config gained an unreviewed trust scope')
        Element baseConfig = rootChildren.first()
        require(baseConfig.getAttribute('cleartextTrafficPermitted') == 'true',
                'GameStream LAN cleartext exception changed unexpectedly')
        def certificates = baseConfig.getElementsByTagName('certificates')
        require(certificates.length == 1 &&
                ((Element) certificates.item(0)).getAttribute('src') == 'system',
                'Only the Android system trust store may be a trust anchor')
    }

    private static Set<String> exclusions(Element parent) {
        return childElements(parent)
                .findAll { it.tagName == 'exclude' }
                .collect { Element element ->
                    ("${element.getAttribute('domain')}:" +
                            element.getAttribute('path')).toString()
                } as Set
    }

    private static Element firstElement(Element parent, String tagName) {
        return childElements(parent).find { it.tagName == tagName }
    }

    private static List<Element> childElements(Element parent) {
        List<Element> elements = []
        for (Node node = parent.firstChild;
             node != null;
             node = node.nextSibling) {
            if (node.nodeType == Node.ELEMENT_NODE) {
                elements.add((Element) node)
            }
        }
        return elements
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
