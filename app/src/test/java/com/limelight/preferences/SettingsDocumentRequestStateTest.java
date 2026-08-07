package com.limelight.preferences;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class SettingsDocumentRequestStateTest {
    @Test
    public void validRestoredRequestSurvivesRecreationUntilConsumed() {
        SettingsDocumentController.RequestState state =
                new SettingsDocumentController.RequestState(
                        SettingsDocumentController.REQUEST_CLIPBOARD_DIRECTORY);

        assertEquals(
                SettingsDocumentController.REQUEST_CLIPBOARD_DIRECTORY,
                state.peek());
        assertEquals(
                SettingsDocumentController.REQUEST_CLIPBOARD_DIRECTORY,
                state.consume());
        assertEquals(
                SettingsDocumentController.NO_PENDING_REQUEST,
                state.peek());
    }

    @Test
    public void invalidRestoredRequestIsDiscarded() {
        SettingsDocumentController.RequestState state =
                new SettingsDocumentController.RequestState(9999);

        assertEquals(
                SettingsDocumentController.NO_PENDING_REQUEST,
                state.peek());
    }

    @Test
    public void onlyOnePickerRequestCanBeActive() {
        SettingsDocumentController.RequestState state =
                new SettingsDocumentController.RequestState(
                        SettingsDocumentController.NO_PENDING_REQUEST);
        state.begin(
                SettingsDocumentController.REQUEST_CONFIGURATION_IMPORT);

        try {
            state.begin(
                    SettingsDocumentController.REQUEST_ACCESSIBILITY_IMPORT);
            fail("Expected overlapping request to be rejected");
        }
        catch (IllegalStateException expected) {
            assertEquals(
                    SettingsDocumentController
                            .REQUEST_CONFIGURATION_IMPORT,
                    state.peek());
        }
    }

    @Test
    public void failedLaunchClearsPendingRequestAndPropagatesFailure() {
        SettingsDocumentController.RequestState state =
                new SettingsDocumentController.RequestState(
                        SettingsDocumentController.NO_PENDING_REQUEST);
        RuntimeException launchFailure = new RuntimeException(
                "No document provider");

        try {
            state.launch(
                    SettingsDocumentController.REQUEST_CONFIGURATION_IMPORT,
                    () -> {
                        throw launchFailure;
                    });
            fail("Expected launch failure to be propagated");
        }
        catch (RuntimeException error) {
            assertEquals(launchFailure, error);
        }

        assertEquals(
                SettingsDocumentController.NO_PENDING_REQUEST,
                state.peek());
    }
}
