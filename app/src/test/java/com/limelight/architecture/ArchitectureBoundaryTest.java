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
                        "com.limelight.ui.performance..")
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
}
