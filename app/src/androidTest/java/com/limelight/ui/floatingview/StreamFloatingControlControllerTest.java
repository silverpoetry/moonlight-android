package com.limelight.ui.floatingview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsState;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public final class StreamFloatingControlControllerTest {
    @Test
    public void visibilityHasOneLifecycleOwnedView() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> {
                    Context context = InstrumentationRegistry
                            .getInstrumentation()
                            .getTargetContext();
                    FrameLayout parent = new FrameLayout(context);
                    StreamUiSettingsState settingsState =
                            new StreamUiSettingsState(
                                    StreamUiSettings.builder().build());
                    StreamFloatingControlController controller =
                            new StreamFloatingControlController(
                                    context,
                                    parent,
                                    settingsState,
                                    (x, y, left) -> { },
                                    action -> { });

                    controller.applyEnabled(false);
                    assertEquals(0, parent.getChildCount());

                    controller.show();
                    controller.show();
                    assertEquals(1, parent.getChildCount());
                    assertTrue(controller.isVisible());

                    controller.toggleVisibility();
                    assertFalse(controller.isVisible());
                    assertEquals(
                            View.GONE,
                            parent.getChildAt(0).getVisibility());

                    controller.toggleVisibility();
                    assertTrue(controller.isVisible());

                    controller.destroy();
                    controller.destroy();
                    controller.show();
                    assertEquals(0, parent.getChildCount());
                });
    }

    @Test
    public void clickReadsLatestActionAndPositionSnapshot() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> {
                    Context context = InstrumentationRegistry
                            .getInstrumentation()
                            .getTargetContext();
                    FrameLayout parent = new FrameLayout(context);
                    StreamUiSettings initial =
                            StreamUiSettings.builder()
                                    .setRememberFloatingPosition(true)
                                    .setFloatingPosition(
                                            42f,
                                            84f,
                                            false)
                                    .setFloatingAction(
                                            StreamUiSettings.FloatingAction
                                                    .GAME_MENU)
                                    .build();
                    StreamUiSettingsState settingsState =
                            new StreamUiSettingsState(initial);
                    AtomicReference<StreamUiSettings.FloatingAction>
                            action = new AtomicReference<>();
                    StreamFloatingControlController controller =
                            new StreamFloatingControlController(
                                    context,
                                    parent,
                                    settingsState,
                                    (x, y, left) -> { },
                                    action::set);

                    controller.show();
                    View view = parent.getChildAt(0);
                    assertEquals(42f, view.getX(), 0f);
                    assertEquals(84f, view.getY(), 0f);

                    settingsState.replace(
                            initial.toBuilder()
                                    .setFloatingAction(
                                            StreamUiSettings.FloatingAction
                                                    .FULL_KEYBOARD)
                                    .build());
                    assertTrue(view.performClick());
                    assertEquals(
                            StreamUiSettings.FloatingAction.FULL_KEYBOARD,
                            action.get());

                    controller.destroy();
                });
    }
}
