package com.limelight.architecture;

import static org.junit.Assert.assertTrue;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Prevents settings from being persisted and loaded into an immutable runtime
 * snapshot without any production consumer.
 *
 * <p>The Android registry test already proves that every persisted screen item
 * resolves to the typed schema. Loader tests prove the schema-to-snapshot
 * boundary. This final structural check closes the chain by requiring every
 * public snapshot query to be consumed outside the settings implementation.
 * Package-private projection inputs remain implementation details and do not
 * expand the runtime contract.</p>
 */
public final class SettingsRuntimeConsumptionTest {
    private static final Set<String> SETTINGS_MODELS = new HashSet<>(
            Arrays.asList(
                    "com.limelight.settings.app.AppPresentationSettings",
                    "com.limelight.settings.audio.StreamAudioSettings",
                    "com.limelight.settings.controller.ControllerSettings",
                    "com.limelight.settings.input.InputSettings",
                    "com.limelight.settings.stream.StreamDecoderSettings",
                    "com.limelight.settings.stream.StreamDisplaySettings",
                    "com.limelight.settings.stream.StreamVideoSettings",
                    "com.limelight.settings.transfer.TransferSettings",
                    "com.limelight.settings.ui.StreamUiSettings",
                    "com.limelight.settings.virtualcontrols.VirtualControlSettings"));

    private static JavaClasses productionClasses;

    @BeforeClass
    public static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.limelight");
    }

    @Test
    public void everyRuntimeSettingsQueryHasAProductionConsumer() {
        List<String> unconsumedQueries = new ArrayList<>();
        for (String modelName : SETTINGS_MODELS) {
            JavaClass model = productionClasses.get(modelName);
            for (JavaMethod method : model.getMethods()) {
                if (!isSettingsQuery(method)) {
                    continue;
                }

                if (!isConsumedOutsideSettings(method)) {
                    unconsumedQueries.add(
                            modelName + "#" + method.getName());
                }
            }
        }

        Collections.sort(unconsumedQueries);
        assertTrue(
                "Settings queries without a production runtime consumer: " +
                        unconsumedQueries,
                unconsumedQueries.isEmpty());
    }

    private static boolean isSettingsQuery(JavaMethod method) {
        if (!method.getModifiers().contains(JavaModifier.PUBLIC) ||
                !method.getRawParameterTypes().isEmpty()) {
            return false;
        }
        String name = method.getName();
        return startsWithQueryVerb(name) &&
                !"getClass".equals(name);
    }

    private static boolean startsWithQueryVerb(String name) {
        return hasBeanStylePrefix(name, "get") ||
                hasBeanStylePrefix(name, "is") ||
                hasBeanStylePrefix(name, "are") ||
                hasBeanStylePrefix(name, "has") ||
                hasBeanStylePrefix(name, "should") ||
                hasBeanStylePrefix(name, "uses");
    }

    private static boolean hasBeanStylePrefix(
            String name,
            String prefix) {
        return name.length() > prefix.length() &&
                name.startsWith(prefix) &&
                Character.isUpperCase(name.charAt(prefix.length()));
    }

    private static boolean isConsumedOutsideSettings(JavaMethod method) {
        for (JavaAccess<?> access : method.getAccessesToSelf()) {
            JavaClass consumer = access.getOriginOwner();
            String consumerPackage = consumer
                    .getPackageName();
            if (!consumerPackage.startsWith("com.limelight.settings") ||
                    consumerPackage.startsWith(
                            "com.limelight.settings.android")) {
                return true;
            }
            if (consumer.getSimpleName().endsWith("SettingsLoader") &&
                    hasRuntimeConsumer(consumer)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRuntimeConsumer(JavaClass projection) {
        for (JavaMethod method : projection.getMethods()) {
            for (JavaAccess<?> access : method.getAccessesToSelf()) {
                String consumerPackage = access.getOriginOwner()
                        .getPackageName();
                if (!consumerPackage.startsWith("com.limelight.settings") ||
                        consumerPackage.startsWith(
                                "com.limelight.settings.android")) {
                    return true;
                }
            }
        }
        return false;
    }

}
