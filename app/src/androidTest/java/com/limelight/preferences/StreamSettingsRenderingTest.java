package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StreamSettingsRenderingTest {

    @Test
    public void settingsActivityRendersWithoutCrashing() {
        withSettingsActivity(activity ->
                assertFalse(activity.isFinishing()));
    }

    @Test
    public void settingsSwitchHasVisibleMeasuredGeometry() {
        withSettingsActivity(activity -> {
            Switch settingsSwitch = findFirst(
                    activity.getWindow().getDecorView(),
                    Switch.class);
            assertNotNull(settingsSwitch);
            assertTrue(settingsSwitch.getWidth() > 0);
            assertTrue(settingsSwitch.getHeight() > 0);
        });
    }

    @Test
    public void canceledDocumentResultDoesNotCrash() {
        withSettingsActivity(activity -> {
            Instrumentation instrumentation =
                    InstrumentationRegistry.getInstrumentation();
            instrumentation.runOnMainSync(() ->
                    activity.onActivityResult(
                            SettingsDocumentController
                                    .REQUEST_BACKGROUND,
                            Activity.RESULT_CANCELED,
                            null));
            assertFalse(activity.isFinishing());
        });
    }

    @Test
    public void togglingSwitchDoesNotRecreateOrResetSettingsList()
            throws InterruptedException {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        StreamSettings activity =
                startSettingsActivity(instrumentation);
        try {
            ScrollView originalScrollView = findFirst(
                    activity.getWindow().getDecorView(),
                    ScrollView.class);
            Switch settingsSwitch = findFirst(
                    activity.getWindow().getDecorView(),
                    Switch.class);
            assertNotNull(originalScrollView);
            assertNotNull(settingsSwitch);

            instrumentation.runOnMainSync(() -> {
                originalScrollView.scrollTo(0, 300);
                settingsSwitch.performClick();
            });
            Thread.sleep(250);
            instrumentation.waitForIdleSync();

            ScrollView currentScrollView = findFirst(
                    activity.getWindow().getDecorView(),
                    ScrollView.class);
            assertSame(originalScrollView, currentScrollView);
            assertTrue(currentScrollView.getScrollY() > 0);

            instrumentation.runOnMainSync(
                    settingsSwitch::performClick);
        }
        finally {
            activity.finish();
        }
    }

    @Test
    public void everyVisibleSectionReusesActivityAndBackReturnsToRoot() {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        StreamSettings activity =
                startSettingsActivity(instrumentation);
        try {
            int visitedSections = 0;
            for (SettingsSection section : SettingsRegistry.load(activity)) {
                TextView sectionTitle = findText(
                        activity.getWindow().getDecorView(),
                        section.title);
                if (sectionTitle == null) {
                    continue;
                }
                visitedSections++;
                View sectionRow =
                        (View) sectionTitle.getParent().getParent();

                instrumentation.runOnMainSync(
                        sectionRow::performClick);
                instrumentation.waitForIdleSync();

                assertFalse(activity.isFinishing());
                assertNull(findText(
                        activity.getWindow().getDecorView(),
                        activity.getString(
                                R.string.settings_featured_settings)));

                instrumentation.runOnMainSync(activity::onBackPressed);
                instrumentation.waitForIdleSync();

                assertNotNull(findText(
                        activity.getWindow().getDecorView(),
                        activity.getString(
                                R.string.settings_featured_settings)));
            }
            assertTrue(visitedSections > 0);
        }
        finally {
            activity.finish();
        }
    }

    @Test
    public void returningFromSectionRestoresRootScrollPosition() {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        StreamSettings activity =
                startSettingsActivity(instrumentation);
        try {
            ScrollView rootScroll = findFirst(
                    activity.getWindow().getDecorView(),
                    ScrollView.class);
            SettingsSection firstSection =
                    SettingsRegistry.load(activity).get(0);
            TextView sectionTitle = findText(
                    activity.getWindow().getDecorView(),
                    firstSection.title);
            assertNotNull(rootScroll);
            assertNotNull(sectionTitle);

            instrumentation.runOnMainSync(() -> rootScroll.scrollTo(
                    0,
                    rootScroll.getChildAt(0).getHeight()));
            instrumentation.waitForIdleSync();
            int expectedScrollY = rootScroll.getScrollY();
            assertTrue(expectedScrollY > 0);

            View sectionRow =
                    (View) sectionTitle.getParent().getParent();
            instrumentation.runOnMainSync(sectionRow::performClick);
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(activity::onBackPressed);
            instrumentation.waitForIdleSync();

            ScrollView restoredScroll = findFirst(
                    activity.getWindow().getDecorView(),
                    ScrollView.class);
            assertNotNull(restoredScroll);
            assertEquals(expectedScrollY, restoredScroll.getScrollY());
        }
        finally {
            activity.finish();
        }
    }

    private static void withSettingsActivity(
            ActivityAssertion assertion) {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        StreamSettings activity =
                startSettingsActivity(instrumentation);
        try {
            assertion.verify(activity);
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
        if (type.isInstance(view)) {
            return type.cast(view);
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int index = 0;
                index < group.getChildCount();
                index++) {
            T match = findFirst(
                    group.getChildAt(index),
                    type);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static TextView findText(View view, CharSequence text) {
        if (view instanceof TextView &&
                text.toString().contentEquals(
                        ((TextView) view).getText())) {
            return (TextView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            TextView match = findText(group.getChildAt(index), text);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private interface ActivityAssertion {
        void verify(StreamSettings activity);
    }
}
