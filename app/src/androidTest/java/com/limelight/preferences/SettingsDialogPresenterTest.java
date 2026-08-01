package com.limelight.preferences;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.settings.SettingKey;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class SettingsDialogPresenterTest {
    @Test
    public void destroyDismissesActiveDialog() {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        StreamSettings activity = startSettingsActivity(instrumentation);
        SettingsDialogPresenter[] presenter =
                new SettingsDialogPresenter[1];

        try {
            instrumentation.runOnMainSync(() -> {
                SettingsItem item = new SettingsItem();
                item.key = "test.dialog.value";
                item.settingKey = SettingKey.stringKey(
                        item.key,
                        "first");
                item.type = SettingsItem.Type.LIST;
                item.title = "Test dialog";
                item.entries = new CharSequence[] {
                        "First",
                        "Second",
                };
                item.entryValues = new CharSequence[] {
                        "first",
                        "second",
                };

                presenter[0] = new SettingsDialogPresenter(
                        activity,
                        new SettingsStore(activity),
                        new NoOpListener());
                presenter[0].showList(item);
                assertTrue(presenter[0].isShowing());

                presenter[0].destroy();
                assertFalse(presenter[0].isShowing());
            });
        }
        finally {
            activity.finish();
        }
    }

    @Test
    public void integerListReadsTypedIntegerStorage() {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        StreamSettings activity = startSettingsActivity(instrumentation);
        SettingsDialogPresenter[] presenter =
                new SettingsDialogPresenter[1];

        try {
            instrumentation.runOnMainSync(() -> {
                SettingsItem item = new SettingsItem();
                item.key = "test.dialog.integer";
                item.settingKey = SettingKey.integerSetKey(
                        item.key,
                        3,
                        0,
                        3,
                        5);
                item.type = SettingsItem.Type.INTEGER_LIST;
                item.title = "Integer dialog";
                item.entries = new CharSequence[] {
                        "Off",
                        "Three",
                        "Five",
                };
                item.entryValues = new CharSequence[] {
                        "0",
                        "3",
                        "5",
                };

                presenter[0] = new SettingsDialogPresenter(
                        activity,
                        new SettingsStore(activity),
                        new NoOpListener());
                presenter[0].showList(item);
                assertTrue(presenter[0].isShowing());
                presenter[0].destroy();
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

    private static final class NoOpListener
            implements SettingsDialogPresenter.Listener {
        @Override
        public void onListValueSelected(
                SettingsItem item,
                String value) {
        }

        @Override
        public void onSliderValueSelected(
                SettingsItem item,
                int value) {
        }

        @Override
        public CharSequence onTextValueSubmitted(
                SettingsItem item,
                String value) {
            return null;
        }
    }
}
