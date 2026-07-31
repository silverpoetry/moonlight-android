package com.limelight.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Executable dependency rules for boundaries that have completed migration.
 *
 * <p>Rules are added when a boundary is made clean. Existing legacy packages
 * are not broadly excluded or frozen behind a baseline.</p>
 */
public final class ArchitectureBoundaryTest {
    private static JavaClasses productionClasses;

    @BeforeClass
    public static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.limelight");
    }

    @Test
    public void extractedStreamUiDoesNotDependOnGameActivity() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.ui.gamemenu..",
                        "com.limelight.ui.clipboard..",
                        "com.limelight.ui.performance..",
                        "com.limelight.ui.stream..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.Game")
                .because("stream UI must communicate through narrow host contracts")
                .check(productionClasses);
    }

    @Test
    public void pointerInputCoreDoesNotDependOnStreamUi() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.binding.input.pointer..",
                        "com.limelight.binding.input.touch..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.limelight.ui.gamemenu..",
                        "com.limelight.ui.clipboard..",
                        "com.limelight.ui.performance..")
                .because("input state machines must remain independent from stream UI")
                .check(productionClasses);
    }

    @Test
    public void pointerInputCoreDoesNotDependOnConcreteConnection() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.binding.input.pointer..",
                        "com.limelight.binding.input.touch..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.nvstream.NvConnection")
                .because("input state machines emit through PointerInputSink")
                .check(productionClasses);
    }

    @Test
    public void pointerInputCoreDoesNotDependOnGameActivity() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.binding.input.pointer..",
                        "com.limelight.binding.input.touch..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.Game")
                .because("the Activity adapts callbacks into the input controller")
                .check(productionClasses);
    }

    @Test
    public void streamInputOrchestratorDoesNotDependOnConcreteConnection() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.StreamInputController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.nvstream.NvConnection")
                .because("input orchestration dispatches to protocol ports")
                .check(productionClasses);
    }

    @Test
    public void streamInputOrchestratorDoesNotDependOnGameActivity() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.StreamInputController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.Game")
                .because("the Activity supplies UI policy through a host port")
                .check(productionClasses);
    }

    @Test
    public void keyboardInputControllerDoesNotDependOnConcreteConnection() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.KeyboardInputController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.nvstream.NvConnection")
                .because("keyboard protocol output uses KeyboardInputSink")
                .check(productionClasses);
    }

    @Test
    public void keyboardInputControllerDoesNotDependOnGameActivity() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.KeyboardInputController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.Game")
                .because("local keyboard actions use the host contract")
                .check(productionClasses);
    }

    @Test
    public void keyboardChordSenderDoesNotDependOnConcreteConnection() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.KeyboardChordSender")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.nvstream.NvConnection")
                .because("chords emit through KeyboardInputSink")
                .check(productionClasses);
    }

    @Test
    public void streamSessionControllerDoesNotDependOnGameActivity() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.nvstream.StreamSessionController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.Game")
                .because("the session state machine must outlive UI refactors")
                .check(productionClasses);
    }

    @Test
    public void streamSessionControllerDoesNotDependOnStreamUi() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.nvstream.StreamSessionController")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.limelight.ui..")
                .because("session lifecycle emits through its listener port")
                .check(productionClasses);
    }

    @Test
    public void gameActivityDoesNotImplementTransportCallbacks() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.nvstream.NvConnectionListener")
                .because("transport callbacks are owned by the session router")
                .check(productionClasses);
    }

    @Test
    public void streamFailureDiagnosticsDoesNotDependOnConnection() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamFailureDiagnostics")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.nvstream.NvConnection")
                .because("diagnostics run through an injected probe")
                .check(productionClasses);
    }

    @Test
    public void typedStreamSettingsDoNotDependOnAndroidOrLegacyPreferences() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.settings.stream..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "com.limelight.preferences..")
                .because("typed stream settings are immutable domain models")
                .check(productionClasses);
    }

    @Test
    public void typedInputSettingsDoNotDependOnAndroidOrLegacyPreferences() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.settings.input..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "com.limelight.preferences..")
                .because("typed input settings are immutable domain models")
                .check(productionClasses);
    }

    @Test
    public void typedControllerSettingsDoNotDependOnAndroidOrLegacyPreferences() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.settings.controller..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "com.limelight.preferences..")
                .because(
                        "typed controller settings are immutable domain models")
                .check(productionClasses);
    }

    @Test
    public void typedAudioSettingsDoNotDependOnAndroidOrLegacyPreferences() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.settings.audio..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "com.limelight.preferences..")
                .because(
                        "typed audio settings are immutable domain models")
                .check(productionClasses);
    }

    @Test
    public void typedVirtualControlSettingsDoNotDependOnAndroidOrLegacyPreferences() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.settings.virtualcontrols..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "com.limelight.preferences..")
                .because(
                        "virtual-control settings are immutable domain models")
                .check(productionClasses);
    }

    @Test
    public void virtualControlLayoutDomainDoesNotDependOnAndroid() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.virtualcontrols.layout")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..", "androidx..")
                .because(
                        "layout identity and persistence ports are platform independent")
                .check(productionClasses);
    }

    @Test
    public void virtualControlRuntimeDoesNotDependOnFileUtilities() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.binding.input.virtual_controller..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.utils.FileUriUtils")
                .because(
                        "editable layouts are loaded through their repository port")
                .check(productionClasses);
    }

    @Test
    public void videoBindingDoesNotDependOnLegacyPreferences() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.binding.video..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.PreferenceConfiguration")
                .because("video binding consumes immutable settings snapshots")
                .check(productionClasses);
    }

    @Test
    public void migratedInputRuntimeDoesNotDependOnLegacyPreferences() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.binding.input.pointer..",
                        "com.limelight.binding.input.touch..")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.KeyboardInputController")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.StreamInputController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.PreferenceConfiguration")
                .because("input callbacks consume an immutable session snapshot")
                .check(productionClasses);
    }

    @Test
    public void controllerHandlerDoesNotReadPersistenceOrLegacySettings() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.ControllerHandler")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..")
                .because(
                        "controller callbacks consume one immutable session snapshot")
                .check(productionClasses);
    }

    @Test
    public void controllerHandlerDoesNotDependOnSharedPreferences() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.ControllerHandler")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "controller callbacks cannot perform persistence I/O")
                .check(productionClasses);
    }

    @Test
    public void audioBindingDoesNotReadPersistenceOrLegacySettings() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.binding.audio..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..",
                        "com.limelight.settings.android..")
                .because(
                        "real-time audio callbacks consume immutable settings snapshots")
                .check(productionClasses);
    }

    @Test
    public void audioBindingDoesNotDependOnSharedPreferences() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.binding.audio..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "real-time audio callbacks cannot perform persistence I/O")
                .check(productionClasses);
    }

    @Test
    public void microphoneLifecycleDomainDoesNotDependOnAndroidOrJni() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.nvstream.mic..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..",
                        "com.limelight.nvstream.jni..")
                .because(
                        "microphone lifecycle and protocol invariants are platform-independent")
                .check(productionClasses);
    }

    @Test
    public void connectionDependsOnMicrophonePortNotAndroidCapture() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.nvstream.NvConnection")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.limelight.binding.audio.mic..")
                .because(
                        "the composition root injects the microphone capture adapter")
                .check(productionClasses);
    }

    @Test
    public void usbDriverServiceDoesNotReadPersistenceOrLegacySettings() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.driver.UsbDriverService")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..",
                        "com.limelight.settings.android..")
                .because(
                        "the bound stream configures one typed controller-policy state before USB enumeration")
                .check(productionClasses);
    }

    @Test
    public void virtualControlRuntimeDoesNotReadPersistenceOrLegacySettings() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.binding.input.virtual_controller..")
                .and()
                .haveSimpleNameNotEndingWith(
                        "ConfigurationLoader")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..")
                .because(
                        "overlay rendering and input callbacks consume typed snapshots")
                .check(productionClasses);
    }
}
