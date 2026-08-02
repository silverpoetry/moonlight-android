package com.limelight.architecture;

import android.app.Activity;
import android.content.Intent;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

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
    public void productionCodeDoesNotUsePlatformPreferenceWidgets() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android.preference..")
                .because(
                        "preferences.xml is presentation schema and persistence uses the typed settings repository")
                .check(productionClasses);
    }

    @Test
    public void activitiesDoNotAddressSharedPreferences() {
        noClasses()
                .that()
                .areAssignableTo(Activity.class)
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "Activities compose persistence adapters but do not own storage names or migration formats")
                .check(productionClasses);
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
    public void siblingViewCoordinateMappingHasOnlyApprovedConsumers() {
        noClasses()
                .that()
                .resideOutsideOfPackages(
                        "com.limelight.binding.input.pointer..",
                        "com.limelight.binding.input.touch..",
                        "com.limelight.ui")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.utils.ViewCoordinateMapper")
                .because(
                        "stream input and the native cursor share one sibling-view transform")
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
    public void gameDelegatesKeyboardInputHostAdaptation() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.KeyboardInputController$Host")
                .because(
                        "keyboard Activity actions and delayed scheduling belong to their adapter")
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
    public void gameDelegatesSessionUiEffectAdaptation() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamSessionUiEffects$Host")
                .because(
                        "the Android session UI host owns window, GameManager, and capture effects")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesSessionPresentationAdaptation() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamSessionPresentationController$Host")
                .because(
                        "Android session presentation belongs to its concrete adapter")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesRenderSurfaceHostAdaptation() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamRenderSurfaceController$Host")
                .because(
                        "render startup transactions and Surface lifecycle adaptation have one owner")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesSystemUiVisibilityListening() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.view.View$OnSystemUiVisibilityChangeListener")
                .because(
                        "immersive-window state and delayed restoration have one lifecycle owner")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesConnectingDialogOwnership() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.utils.SpinnerDialog")
                .because(
                        "connecting-dialog nullability and cleanup have one lifecycle owner")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesNativeCursorViewOwnership() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.NativeCursorOverlayView")
                .because(
                        "native cursor attachment, scaling, and thread confinement have one owner")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesControllerFeedbackRouting() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamSessionCallbackRouter$FeedbackHost")
                .because(
                        "controller feedback routing belongs to its tested session adapter")
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
    public void productionDoesNotDependOnRemovedLegacyPreferences() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.PreferenceConfiguration")
                .because(
                        "all settings consumers use domain-scoped snapshots")
                .check(productionClasses);
    }

    @Test
    public void hostPairingUseCaseIsPlatformAndTransportIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.computers.pairing.HostPairingUseCase")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.computers\\.pairing\\.HostPairingUseCase\\$.*")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..",
                        "com.limelight.nvstream..",
                        "com.limelight.ui..")
                .because(
                        "pairing policy runs through transport, credential, and UI ports")
                .check(productionClasses);
    }

    @Test
    public void immutableHostRepositoryModelsArePlatformIndependent() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.computers.model..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..",
                        "com.limelight.nvstream..",
                        "com.limelight.ui..")
                .because(
                        "persistent host identity, endpoints, credentials, and connection state are immutable domain values")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.computers.HostRepository")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..",
                        "com.limelight.nvstream..",
                        "com.limelight.ui..")
                .because(
                        "host application policy depends on an immutable repository port, not SQLite or protocol DTOs")
                .check(productionClasses);
    }

    @Test
    public void hostServiceDoesNotStoreMutableProtocolDtos() {
        noFields()
                .that()
                .areDeclaredInClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.computers.ComputerManagerService")
                .or()
                .areDeclaredInClassesThat()
                .haveNameMatching(
                        "com\\.limelight\\.computers\\.ComputerManagerService\\$.*")
                .should()
                .haveRawType(
                        "com.limelight.nvstream.http.ComputerDetails")
                .because(
                        "the host service stores only immutable runtime snapshots and converts mutable protocol DTOs at its edges")
                .check(productionClasses);
    }

    @Test
    public void hostBinderDoesNotExposeMutableProtocolDtos() {
        noMethods()
                .that()
                .areDeclaredInClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.computers.ComputerManagerService$ComputerManagerBinder")
                .should()
                .haveRawReturnType(
                        "com.limelight.nvstream.http.ComputerDetails")
                .orShould()
                .haveRawParameterTypes(
                        "com.limelight.nvstream.http.ComputerDetails")
                .because(
                        "the Binder boundary publishes immutable host values and accepts typed host identities or endpoints")
                .check(productionClasses);
    }

    @Test
    public void hostCallbacksDoNotExposeMutableProtocolDtos() {
        noMethods()
                .that()
                .areDeclaredInClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.computers.ComputerManagerListener")
                .should()
                .haveRawReturnType(
                        "com.limelight.nvstream.http.ComputerDetails")
                .orShould()
                .haveRawParameterTypes(
                        "com.limelight.nvstream.http.ComputerDetails")
                .because(
                        "host observers receive immutable snapshots rather than mutable transport records")
                .check(productionClasses);
    }

    @Test
    public void hostGridRendersImmutableDomainState() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.grid.PcGridAdapter")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.nvstream.http.ComputerDetails")
                .because(
                        "host-list rendering consumes immutable runtime snapshots and emits no protocol mutations")
                .check(productionClasses);
    }

    @Test
    public void hostSessionPolicyIsPlatformAndTransportIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.computers.session.HostQuitUseCase")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.computers\\.session\\.HostQuitUseCase\\$.*")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.computers.session.HostUnpairUseCase")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.computers\\.session\\.HostUnpairUseCase\\$.*")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.computers.session.DeferredHostQuitController")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.computers\\.session\\.DeferredHostQuitController\\$.*")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..",
                        "com.limelight.nvstream..",
                        "com.limelight.ui..")
                .because(
                        "host session state and deferred execution are pure application policy")
                .check(productionClasses);
    }

    @Test
    public void hostPairingLifecycleDoesNotDependOnActivity() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.hosts.HostPairingController")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.ui\\.hosts\\.HostPairingController\\$.*")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android.app..")
                .because(
                        "pairing execution and stale-callback rejection have a lifecycle port")
                .check(productionClasses);
    }

    @Test
    public void foregroundHostOperationsHaveOnePlatformIndependentOwner() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.hosts.HostUiOperationController")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.ui\\.hosts\\.HostUiOperationController\\$.*")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.computers.model.ManualHostEndpointParser")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..", "androidx..")
                .because(
                        "manual-host parsing, cancellation, and stale-callback policy are pure")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.AddComputerManually")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.AppView")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.PcView")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "java.util.concurrent.Executors")
                .because(
                        "host Activities delegate foreground worker ownership to HostUiOperationController")
                .check(productionClasses);
    }

    @Test
    public void hostReachabilityPolicyIsPlatformAndTransportIndependent() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.computers.reachability..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..",
                        "com.limelight.nvstream..",
                        "com.limelight.ui..")
                .because(
                        "endpoint ordering, selection, and cancellation run through pure ports")
                .check(productionClasses);
    }

    @Test
    public void hostRepositoryLeaseStateIsPlatformIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.computers.HostRepositoryLeaseManager")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.computers\\.HostRepositoryLeaseManager\\$.*")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..", "androidx..")
                .because(
                        "repository lifetime is a pure atomic ownership policy")
                .check(productionClasses);
    }

    @Test
    public void hostServiceLifecyclePrimitivesArePlatformIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.computers.HostPollingOwnership")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.computers\\.HostPollingOwnership\\$.*")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.computers.ComputerDetailsSnapshot")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.computers.InFlightOperationTracker")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.computers\\.InFlightOperationTracker\\$.*")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.computers.HostPollingClientLifecycle")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.computers\\.HostPollingClientLifecycle\\$.*")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.computers.HostAdmissionGate")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.computers\\.HostAdmissionGate\\$.*")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..", "androidx..")
                .because(
                        "polling ownership and DTO snapshots are pure state boundaries")
                .check(productionClasses);
    }

    @Test
    public void hostScreensDoNotOpenTheHostDatabaseDirectly() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.PcView")
                .or()
                .haveFullyQualifiedName("com.limelight.AppView")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ShortcutTrampoline")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.computers.ComputerDatabaseManager")
                .because(
                        "host UI must use the service-owned repository boundary")
                .check(productionClasses);
    }

    @Test
    public void hostServiceBindingHasOnePlatformIndependentOwner() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.hosts.HostServiceBindingController")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.ui\\.hosts\\.HostServiceBindingController\\$.*")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..", "androidx..")
                .because(
                        "binding replacement, cancellation, and stale-result disposal are pure lifecycle policy")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.PcView")
                .or()
                .haveFullyQualifiedName("com.limelight.AppView")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ShortcutTrampoline")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("java.lang.Thread")
                .because(
                        "host screens delegate binding workers to HostServiceBindingController")
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
    public void gameDelegatesFailureDiagnosticsInfrastructure() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamFailureDiagnostics")
                .because(
                        "the Android diagnostics factory owns probe and dispatch infrastructure")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesLaunchReportingInfrastructure() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.utils.ShortcutHelper")
                .because(
                        "launch reporting snapshots and shortcut services belong to their factory")
                .check(productionClasses);
    }

    @Test
    public void streamSessionPresentationPolicyIsAndroidIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamConnectionMessages")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamSessionPresentationController")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..")
                .because(
                        "connection presentation policy is tested through narrow platform ports")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamSessionPresentationController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamFailureDiagnostics")
                .because(
                        "the controller owns diagnostics through its lifecycle port")
                .check(productionClasses);
    }

    @Test
    public void streamSessionConfigurationPlanningIsNativeIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamSessionConfigurationPlanner")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..")
                .because(
                        "stream startup policy consumes already-sampled immutable inputs")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamSessionConfigurationPlanner")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.nvstream.StreamConfiguration")
                .because(
                        "the pure configuration document must not load JNI-backed transport defaults")
                .check(productionClasses);
    }

    @Test
    public void decoderCrashPolicyIsPlatformIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.binding.video.DecoderCrashTracker")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.binding.video.DecoderCrashState")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.binding.video.DecoderCrashStore")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.binding.video.DecoderCrashNotificationPolicy")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.binding\\.video\\." +
                                "DecoderCrashNotificationPolicy\\$.*")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..")
                .because(
                        "crash accounting runs through its persistence port")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.decoder." +
                                "AndroidDecoderCrashNotificationController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "decoder-crash presentation consumes the store port rather than its Android persistence")
                .check(productionClasses);
    }

    @Test
    public void clipboardCheckpointHasOneAndroidPersistenceBoundary() {
        noClasses()
                .that()
                .resideInAPackage(
                        "com.limelight.nvstream.clipboard")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..", "androidx..")
                .because(
                        "clipboard loop-suppression state is an immutable platform-independent document")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.nvstream.ClipboardSyncController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "the connection state machine persists checkpoints through its port")
                .check(productionClasses);
    }

    @Test
    public void hiddenAppSelectionIsPlatformIndependent() {
        noClasses()
                .that()
                .resideInAPackage("com.limelight.computers.apps")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..", "androidx..")
                .because(
                        "host-scoped hidden app selection crosses a repository port")
                .check(productionClasses);
    }

    @Test
    public void glDeviceSnapshotHasOneAndroidPersistenceBoundary() {
        noClasses()
                .that()
                .resideInAPackage(
                        "com.limelight.binding.video.gl")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..", "androidx..")
                .because(
                        "the build-scoped GL identity is an immutable platform-independent snapshot")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.PcView")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences." +
                                "AndroidSettingsDisplayCapabilities")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream." +
                                "AndroidStreamMediaRuntimeFactory")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "GL consumers receive a snapshot or store port instead of addressing its cache")
                .check(productionClasses);
    }

    @Test
    public void streamHdrRequestPolicyIsPlatformIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamHdrRequestPolicy")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.ui\\.stream\\.StreamHdrRequestPolicy\\$.*")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..")
                .because(
                        "HDR request policy consumes immutable sampled device facts")
                .check(productionClasses);
    }

    @Test
    public void streamDisplayRefreshPolicyIsPlatformIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamDisplayRefreshPolicy")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..")
                .because(
                        "display refresh policy consumes immutable settings and device facts")
                .check(productionClasses);
    }

    @Test
    public void externalDisplaySelectionPolicyIsPlatformIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.ExternalDisplaySelectionPolicy")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..")
                .because(
                        "external display selection consumes sampled display IDs")
                .check(productionClasses);
    }

    @Test
    public void streamPictureInPictureStateIsPlatformIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamPictureInPictureState")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..")
                .because(
                        "PiP session and suppression state must be deterministic in JVM tests")
                .check(productionClasses);
    }

    @Test
    public void streamOverlayVisibilityControllerIsPlatformIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamOverlayVisibilityController")
                .or()
                .haveNameMatching(
                        "com\\.limelight\\.ui\\.stream\\.StreamOverlayVisibilityController\\$.*")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..")
                .because(
                        "PiP overlay coordination runs through a narrow host port")
                .check(productionClasses);
    }

    @Test
    public void streamInputCaptureStateIsPlatformIndependent() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.capture.StreamInputCaptureState")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..")
                .because(
                        "input grab and local-cursor policy must remain JVM-testable")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesPhysicalDisplayPreparation() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamDisplayModeSelector")
                .because(
                        "the Android display controller owns physical mode selection")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.StreamLayoutGeometry")
                .because(
                        "the Android display controller owns render-surface geometry")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesExternalDisplayPresentation() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android.hardware.display..")
                .because(
                        "the external-display controller owns discovery and Presentation lifecycle")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("android.app.Presentation")
                .because(
                        "the external-display controller owns Presentation lifecycle")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesPictureInPictureParameters() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.app.PictureInPictureParams")
                .because(
                        "the PiP controller owns platform parameters and API dispatch")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesInputCaptureConstructionAndVendorApi() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.capture.InputCaptureManager")
                .because(
                        "the input-capture controller owns provider selection")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("java.lang.reflect..")
                .because(
                        "vendor input-capture reflection belongs at the Android adapter boundary")
                .check(productionClasses);
    }

    @Test
    public void gameDoesNotConstructConcreteDecoderRuntime() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.binding.video.MediaCodecDecoderRenderer")
                .because(
                        "the Android media factory owns decoder construction and capability sampling")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.binding.video.MediaCodecHelper")
                .because(
                        "codec initialization is part of media runtime construction")
                .check(productionClasses);
    }

    @Test
    public void streamMicrophoneControllerUsesOnlyItsEndpointPort() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamMicrophoneController")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..")
                .because(
                        "microphone interaction policy is platform-independent")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamMicrophoneController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.nvstream.NvConnection")
                .because(
                        "microphone UI orchestration depends on its narrow endpoint port")
                .check(productionClasses);
    }

    @Test
    public void gameDelegatesAndroidMicrophoneComposition() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamMicrophoneController$PermissionGateway")
                .because(
                        "the Android microphone factory owns permission adaptation")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.stream.StreamMicrophoneController$Feedback")
                .because(
                        "the Android microphone factory owns localized feedback")
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
    public void typedAppPresentationSettingsDoNotDependOnAndroidOrLegacyPreferences() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.settings.app..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "com.limelight.preferences..")
                .because(
                        "application presentation is a platform-independent snapshot")
                .check(productionClasses);
    }

    @Test
    public void settingsScreenModelsDoNotDependOnAndroid() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsItem")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsSection")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsScreenModel")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsValueReader")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..")
                .because(
                        "settings metadata and dependency policy are platform-independent")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.StreamSettings")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsRegistry")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.settings.android.SharedPreferencesSettingsRepository")
                .because(
                        "only SettingsStore adapts screen metadata to Android persistence")
                .check(productionClasses);
    }

    @Test
    public void settingsDocumentOperationsHaveOneAndroidBoundary() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsDocumentAction")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsDocumentActionRouter")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..")
                .because(
                        "settings document action routing is platform-independent")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.StreamSettings")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.limelight.computers..")
                .because(
                        "the settings Activity delegates database I/O to its lifecycle controller")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.StreamSettings")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.utils.FileUriUtils")
                .because(
                        "the settings Activity delegates document I/O to its lifecycle controller")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.StreamSettings")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("android.app.AlertDialog")
                .because(
                        "settings value editor windows belong to SettingsDialogPresenter")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.StreamSettings")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android.widget..")
                .because(
                        "settings Views and row rendering belong to SettingsScreenRenderer")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsScreenRenderer")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsItem")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsStore")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsValueReader")
                .because(
                        "the renderer consumes only immutable settings screen state")
                .check(productionClasses);
    }

    @Test
    public void settingsCapabilityPolicyDoesNotDependOnAndroid() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsDeviceCapabilities")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsVisibilityPolicy")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..")
                .because(
                        "device visibility decisions must be testable without Android")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsRuntimeValues")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsRuntimeScreenController")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsRuntimeText")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsMutationController")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..")
                .because(
                        "runtime-derived screen metadata must be testable without Android")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsDisplayCapabilities")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsDisplayPolicy")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsDisplayController")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.SettingsDisplayText")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..")
                .because(
                        "display capability decisions must be testable without Android")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.preferences.StreamSettings")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.binding.video.MediaCodecHelper")
                .because(
                        "the settings Activity must not probe decoder capabilities directly")
                .check(productionClasses);
    }

    @Test
    public void defaultSettingsStorageHasOneAndroidCompositionPackage() {
        noClasses()
                .that()
                .resideOutsideOfPackages(
                        "com.limelight.settings.android..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.preference.PreferenceManager")
                .because(
                        "default preferences are composed only through AndroidSettingsRepository")
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
    public void typedTransferSettingsDoNotDependOnAndroidOrLegacyPreferences() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.settings.transfer..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "com.limelight.preferences..")
                .because(
                        "typed transfer settings are immutable domain models")
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
    public void virtualControlRuntimeDoesNotAddressSharedPreferences() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.binding.input.virtual_controller..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "the active overlay consumes typed layout and settings repositories")
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
    public void clipboardTransferUiUsesTypedSettingsRepository() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.clipboard.RemoteClipboardFileTransferController")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..")
                .because(
                        "clipboard destination policy crosses the typed repository boundary")
                .check(productionClasses);
    }

    @Test
    public void clipboardTransferUiDoesNotDependOnSharedPreferences() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.clipboard.RemoteClipboardFileTransferController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "clipboard destination policy is accessed through typed keys")
                .check(productionClasses);
    }

    @Test
    public void touchSettingsUiUsesTypedSettingsIntents() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameTouchFragment")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..")
                .because(
                        "touch settings UI emits typed domain intents")
                .check(productionClasses);
    }

    @Test
    public void touchSettingsUiDoesNotDependOnSharedPreferences() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameTouchFragment")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "touch settings UI cannot address persistence directly")
                .check(productionClasses);
    }

    @Test
    public void deviceSettingsUiUsesTypedControllerIntents() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayDeviceFragment")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..")
                .because(
                        "device settings UI emits typed controller intents")
                .check(productionClasses);
    }

    @Test
    public void deviceSettingsUiDoesNotDependOnSharedPreferences() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayDeviceFragment")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "device settings UI cannot address persistence directly")
                .check(productionClasses);
    }

    @Test
    public void streamOverlayUiDoesNotReadSettingsStorage() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.performance.StreamPerformanceOverlayController")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.performance.PerformanceOverlayFormatter")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.floatingview.FloatingMagnetView")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.floatingview.StreamFloatingControlController")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..",
                        "com.limelight.settings.android..")
                .because(
                        "overlay runtime consumes immutable settings snapshots")
                .check(productionClasses);
    }

    @Test
    public void sharedPlatformAdaptersRemainPolicyFree() {
        noClasses()
                .that()
                .resideInAPackage("com.limelight.platform..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.limelight.preferences..",
                        "com.limelight.settings..",
                        "com.limelight.ui..")
                .because(
                        "shared Android compatibility adapters expose platform facts, not product policy")
                .check(productionClasses);
    }

    @Test
    public void productionCodeDoesNotUseLegacyActivityResults() {
        noClasses()
                .should()
                .callMethod(
                        Activity.class,
                        "startActivityForResult",
                        Intent.class,
                        int.class)
                .because(
                        "Activity Result launchers own lifecycle-safe result delivery")
                .check(productionClasses);
        noMethods()
                .that()
                .areDeclaredInClassesThat()
                .resideInAPackage("com.limelight..")
                .should()
                .haveName("onActivityResult")
                .because(
                        "Activity Result callbacks replace request-code routing")
                .check(productionClasses);
    }

    @Test
    public void floatingViewDoesNotOwnMutableSettingsState() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.floatingview.FloatingMagnetView")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.settings.ui.StreamUiSettingsState")
                .because(
                        "the View renders an immutable snapshot and emits position events")
                .check(productionClasses);
    }

    @Test
    public void streamOverlayUiDoesNotUseSharedPreferences() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.performance.StreamPerformanceOverlayController")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.floatingview.FloatingMagnetView")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.floatingview.StreamFloatingControlController")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "runtime UI cannot address persistence directly")
                .check(productionClasses);
    }

    @Test
    public void gameUsesOnlyTheFloatingControlOwner() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.floatingview.FloatingMagnetView")
                .because(
                        "the Activity delegates floating-control view ownership")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.ui.floatingview.FloatingControlView")
                .because(
                        "the Activity delegates floating-control view ownership")
                .check(productionClasses);
    }

    @Test
    public void virtualControlLifecycleHasOneActivityBoundary() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.virtual_controller.keyboard.StreamVirtualControlsController")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("android..")
                .because(
                        "virtual-overlay lifecycle policy is platform-independent")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.virtual_controller.keyboard.KeyBoardController")
                .because(
                        "the Activity delegates editable-overlay ownership to one controller")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.Game")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.binding.input.virtual_controller.keyboard.KeyBoardLayoutController")
                .because(
                        "the Activity delegates full-keyboard ownership to one controller")
                .check(productionClasses);
    }

    @Test
    public void displaySettingsUiUsesTypedSettingsIntents() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayFpsFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayResolutionFragment")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..",
                        "com.limelight.settings.android..")
                .because(
                        "display settings UI consumes snapshots, emits typed intents, and depends on a storage port")
                .check(productionClasses);
    }

    @Test
    public void displaySettingsUiDoesNotUseSharedPreferences() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayFpsFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayResolutionFragment")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "display settings UI cannot address persistence directly")
                .check(productionClasses);
    }

    @Test
    public void displaySettingsUiDoesNotDependOnActivityImplementations() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayFpsFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayResolutionFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayBitrateFragment")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.Game")
                .because(
                        "recreated display dialogs resolve only their lifecycle-bound host contract")
                .check(productionClasses);
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayFpsFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayResolutionFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameDisplayBitrateFragment")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.AppView")
                .because(
                        "recreated display dialogs resolve only their lifecycle-bound host contract")
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

    @Test
    public void streamMenuAndOrientationUseTypedSettingsContracts() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuHost")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.utils.StreamOrientationController")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.utils.StreamOrientationRequest")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..",
                        "com.limelight.settings.android..")
                .because(
                        "menu presentation and orientation policy consume immutable typed projections")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuHost")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.utils.StreamOrientationController")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.utils.StreamOrientationRequest")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "runtime menu and orientation code cannot address persistence")
                .check(productionClasses);
    }

    @Test
    public void gameMenuCardUiCannotAddressPersistence() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuHost")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuCardEditor")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuCardConfiguration")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..",
                        "com.limelight.settings.android..")
                .because(
                        "card UI renders immutable references and emits save intents through its host")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuHost")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuCardEditor")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuCardConfiguration")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.settings.SettingsRepository")
                .because(
                        "the Activity composition root owns the card repository")
                .check(productionClasses);
    }

    @Test
    public void gameMenuCardSettingsDomainIsPlatformIndependent() {
        noClasses()
                .that()
                .haveSimpleNameStartingWith("GameMenuCard")
                .and()
                .resideInAnyPackage(
                        "com.limelight.settings.ui..")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.settings.ui.SettingsGameMenuCardLayoutRepository")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..")
                .because(
                        "card identity, codec, schema, and repository port are pure Java")
                .check(productionClasses);
    }

    @Test
    public void gameMenuShortcutUiCannotAddressPersistence() {
        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameListQuickFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuShortcutCatalog")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuShortcutMapper")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuCardCatalog")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android.preference..",
                        "com.limelight.preferences..",
                        "com.limelight.settings.android..",
                        "com.limelight.shortcuts.android..")
                .because(
                        "shortcut UI consumes immutable snapshots and emits intents through its host")
                .check(productionClasses);

        noClasses()
                .that()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameListQuickFragment")
                .or()
                .haveFullyQualifiedName(
                        "com.limelight.ui.gamemenu.GameMenuShortcutCatalog")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "android.content.SharedPreferences")
                .because(
                        "the Activity composition root owns shortcut storage")
                .check(productionClasses);
    }

    @Test
    public void gameMenuShortcutDomainIsPlatformIndependent() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.shortcuts")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..",
                        "com.limelight.ui..")
                .because(
                        "shortcut identity, documents, codecs, and persistence port are pure Java")
                .check(productionClasses);
    }

    @Test
    public void streamLaunchDomainIsPlatformIndependent() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.stream.launch")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "android..",
                        "androidx..",
                        "com.limelight.nvstream..",
                        "com.limelight.ui..",
                        "com.limelight.preferences..")
                .because(
                        "stream launch admission and reconnect policy are pure domain state")
                .check(productionClasses);
    }

    @Test
    public void hostScreensCannotConstructGameLaunchIntents() {
        noClasses()
                .that()
                .haveFullyQualifiedName("com.limelight.AppView")
                .or()
                .haveFullyQualifiedName("com.limelight.PcView")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.Game")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.stream.launch.android.AndroidStreamLaunchIntentFactory")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.limelight.stream.launch.android.SharedPreferencesRecentStreamSessionRepository")
                .because(
                        "host screens launch through one lifecycle-owned application boundary")
                .check(productionClasses);
    }

    @Test
    public void onlyLaunchIntentAdapterTargetsGameActivity() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.limelight.stream.launch.android..")
                .and()
                .doNotHaveFullyQualifiedName(
                        "com.limelight.stream.launch.android.AndroidStreamLaunchIntentFactory")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.limelight.Game")
                .because(
                        "the launch contract and platform adapters must not depend on the destination Activity")
                .check(productionClasses);
    }
}
