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
import android.widget.FrameLayout;
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
    public void everyVisibleSectionReusesActivityAndBackReturnsToRoot()
            throws InterruptedException {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        StreamSettings activity =
                startSettingsActivity(instrumentation);
        try {
            boolean wideLayout = activity.findViewById(
                    R.id.settings_detail_container) != null;
            FrameLayout contentContainer = activity.findViewById(
                    R.id.settings_content_container);
            View wideScreenPage = wideLayout
                    ? contentContainer.getChildAt(0)
                    : null;
            View wideDetailContainer = activity.findViewById(
                    R.id.settings_detail_container);
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
                waitForTransition(instrumentation);

                assertFalse(activity.isFinishing());
                if (wideLayout) {
                    assertEquals(1, contentContainer.getChildCount());
                    assertSame(wideScreenPage,
                            contentContainer.getChildAt(0));
                    assertSame(wideDetailContainer,
                            activity.findViewById(
                                    R.id.settings_detail_container));
                    continue;
                }
                assertNull(findText(
                        activity.getWindow().getDecorView(),
                        activity.getString(
                                R.string.settings_featured_settings)));

                instrumentation.runOnMainSync(activity::onBackPressed);
                waitForTransition(instrumentation);

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
    public void returningFromSectionRestoresRootScrollPosition()
            throws InterruptedException {
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
            waitForTransition(instrumentation);

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

    @Test
    public void sectionNavigationUsesStackMotionOrStableWideShell()
            throws InterruptedException {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        StreamSettings activity =
                startSettingsActivity(instrumentation);
        try {
            FrameLayout contentContainer =
                    activity.findViewById(
                            R.id.settings_content_container);
            SettingsSection firstSection =
                    SettingsRegistry.load(activity).get(0);
            TextView sectionTitle = findText(
                    activity.getWindow().getDecorView(),
                    firstSection.title);
            assertNotNull(contentContainer);
            assertNotNull(sectionTitle);
            boolean wideLayout = contentContainer.findViewById(
                    R.id.settings_detail_container) != null;
            View originalScreenPage = contentContainer.getChildAt(0);
            View originalWideDetail = contentContainer.findViewById(
                    R.id.settings_detail_container);
            View sectionRow =
                    (View) sectionTitle.getParent().getParent();
            instrumentation.runOnMainSync(sectionRow::performClick);

            if (wideLayout) {
                assertEquals(1, contentContainer.getChildCount());
                assertSame(originalScreenPage,
                        contentContainer.getChildAt(0));
                assertSame(originalWideDetail,
                        contentContainer.findViewById(
                                R.id.settings_detail_container));
                assertSettled(contentContainer);
                return;
            }

            assertStackTransition(contentContainer);
            assertSame(originalScreenPage,
                    contentContainer.getChildAt(0));
            waitForTransition(instrumentation);
            assertSettled(contentContainer);
            View detailScreenPage = contentContainer.getChildAt(0);

            instrumentation.runOnMainSync(activity::onBackPressed);
            assertStackTransition(contentContainer);
            assertSame(detailScreenPage,
                    contentContainer.getChildAt(1));
            waitForTransition(instrumentation);
            assertSettled(contentContainer);
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

    private static void assertSettled(FrameLayout container) {
        assertEquals(1, container.getChildCount());
        View currentPage = container.getChildAt(0);
        assertEquals(0f, currentPage.getTranslationX(), 0f);
        assertEquals(1f, currentPage.getAlpha(), 0f);
        assertNotNull(currentPage.getBackground());
    }

    private static void assertStackTransition(FrameLayout container) {
        assertEquals(2, container.getChildCount());
        for (int index = 0; index < container.getChildCount(); index++) {
            View page = container.getChildAt(index);
            assertEquals(1f, page.getAlpha(), 0f);
            assertNotNull(page.getBackground());
        }
    }

    private static void waitForTransition(
            Instrumentation instrumentation)
            throws InterruptedException {
        long transitionDuration = instrumentation
                .getTargetContext()
                .getResources()
                .getInteger(android.R.integer.config_mediumAnimTime);
        Thread.sleep(transitionDuration + 100L);
        instrumentation.waitForIdleSync();
    }

    private interface ActivityAssertion {
        void verify(StreamSettings activity);
    }
}
