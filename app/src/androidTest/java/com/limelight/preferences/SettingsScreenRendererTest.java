package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public final class SettingsScreenRendererTest {
    @Test
    public void switchViewEmitsIntentAndDestroyDetachesListener() {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        StreamSettings activity = startSettingsActivity(instrumentation);
        AtomicInteger changeCount = new AtomicInteger();
        AtomicReference<String> changedItem =
                new AtomicReference<>();

        try {
            instrumentation.runOnMainSync(() -> {
                SettingsItem item = new SettingsItem();
                item.key = "test.renderer.switch";
                item.type = SettingsItem.Type.SWITCH;
                item.title = "Renderer switch";

                SettingsSection section = new SettingsSection(
                        "test_renderer",
                        "Renderer",
                        0);
                section.items.add(item);
                ArrayList<SettingsSection> sections =
                        new ArrayList<>();
                sections.add(section);
                SettingsScreenRenderer renderer =
                        new SettingsScreenRenderer(
                                activity,
                                new NoOpListener() {
                                    @Override
                                    public void onSwitchChanged(
                                            String selected,
                                            boolean checked) {
                                        changedItem.set(selected);
                                        if (checked) {
                                            changeCount.incrementAndGet();
                                        }
                                    }
                                });
                View root = renderer.createRootView();
                renderer.setContent(
                        SettingsScreenStateFactory.create(
                                sections,
                                new FalseValues(),
                                "Open"),
                        0,
                        "Test profile");
                renderer.render();

                Switch switchView = findFirst(root, Switch.class);
                switchView.performClick();
                assertEquals(1, changeCount.get());
                assertEquals(item.key, changedItem.get());

                renderer.updateState(
                        SettingsScreenStateFactory.create(
                                sections,
                                new FalseValues(),
                                "Open"),
                        "Updated profile");
                assertFalse(switchView.isChecked());
                assertEquals(1, changeCount.get());

                renderer.destroy();
                switchView.performClick();
                assertEquals(1, changeCount.get());
            });
        }
        finally {
            activity.finish();
        }
    }

    private static StreamSettings startSettingsActivity(
            Instrumentation instrumentation) {
        Intent intent = new Intent(
                instrumentation.getTargetContext(),
                StreamSettings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        StreamSettings activity =
                (StreamSettings) instrumentation.startActivitySync(intent);
        instrumentation.waitForIdleSync();
        return activity;
    }

    private static <T extends View> T findFirst(
            View view,
            Class<T> type) {
        T match = findFirstOrNull(view, type);
        if (match == null) {
            throw new AssertionError(
                    "Expected " + type.getSimpleName());
        }
        return match;
    }

    private static <T extends View> T findFirstOrNull(
            View view,
            Class<T> type) {
        if (type.isInstance(view)) {
            return type.cast(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0;
                    index < group.getChildCount();
                    index++) {
                T match = findFirstOrNull(
                        group.getChildAt(index),
                        type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static class NoOpListener
            implements SettingsScreenRenderer.Listener {
        @Override
        public void onBackRequested() {
        }

        @Override
        public void onSectionRequested(int sectionIndex) {
        }

        @Override
        public void onItemRequested(String itemId) {
        }

        @Override
        public void onSwitchChanged(
                String itemId,
                boolean checked) {
        }
    }

    private static final class FalseValues
            implements SettingsValueReader {
        @Override
        public boolean getBoolean(SettingsItem item) {
            return false;
        }

        @Override
        public int getInt(SettingsItem item) {
            return 0;
        }

        @Override
        public String getString(SettingsItem item) {
            return "";
        }

        @Override
        public String getText(SettingsItem item) {
            return "";
        }
    }
}
