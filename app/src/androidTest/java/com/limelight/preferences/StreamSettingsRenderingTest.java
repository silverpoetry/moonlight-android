package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.R;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.android.AndroidSettingsRepository;
import com.limelight.settings.app.AppPresentationSettingKeys;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StreamSettingsRenderingTest {

    @Test
    public void settingsActivityRendersWithoutCrashing() {
        withSettingsActivity(activity -> {
            assertFalse(activity.isFinishing());
            TypedValue windowBackground = new TypedValue();
            assertTrue(activity.getTheme().resolveAttribute(
                    android.R.attr.windowBackground,
                    windowBackground,
                    true));
            assertEquals(
                    R.drawable.bg_gradient_main,
                    windowBackground.resourceId);
        });
    }

    @Test
    public void darkSettingsActivityRetainsOpaqueWindowBackground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        SettingsRepository repository =
                AndroidSettingsRepository.create(
                        instrumentation.getTargetContext());
        boolean originalLightTheme = repository.get(
                AppPresentationSettingKeys.LIGHT_THEME);
        StreamSettings activity = null;
        try {
            assertTrue(repository.edit()
                    .put(AppPresentationSettingKeys.LIGHT_THEME, false)
                    .commit());
            activity = startSettingsActivity(instrumentation);
            TypedValue windowBackground = new TypedValue();
            assertTrue(activity.getTheme().resolveAttribute(
                    android.R.attr.windowBackground,
                    windowBackground,
                    true));
            assertEquals(
                    R.drawable.bg_gradient_main,
                    windowBackground.resourceId);
        }
        finally {
            if (activity != null) {
                activity.finish();
            }
            assertTrue(repository.edit()
                    .put(
                            AppPresentationSettingKeys.LIGHT_THEME,
                            originalLightTheme)
                    .commit());
        }
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
                    activity.handleDocumentActivityResult(
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
    public void everyVisibleSectionUsesActivityStackOrStableWideShell()
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
            FrameLayout wideDetailContainer = activity.findViewById(
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
                if (wideLayout) {
                    View previousDetail =
                            wideDetailContainer.getChildAt(0);
                    instrumentation.runOnMainSync(
                            sectionRow::performClick);
                    assertEquals(1, contentContainer.getChildCount());
                    assertSame(wideScreenPage,
                            contentContainer.getChildAt(0));
                    assertSame(wideDetailContainer,
                            activity.findViewById(
                                    R.id.settings_detail_container));
                    assertEquals(1,
                            wideDetailContainer.getChildCount());
                    assertNotSame(previousDetail,
                            wideDetailContainer.getChildAt(0));
                    continue;
                }

                Instrumentation.ActivityMonitor monitor =
                        instrumentation.addMonitor(
                                StreamSettings.class.getName(),
                                null,
                                false);
                View rootScreenPage = contentContainer.getChildAt(0);
                instrumentation.runOnMainSync(
                        sectionRow::performClick);
                StreamSettings detailActivity = (StreamSettings)
                        instrumentation.waitForMonitorWithTimeout(
                                monitor,
                                2_000);
                instrumentation.removeMonitor(monitor);

                assertNotNull(detailActivity);
                assertNotSame(activity, detailActivity);
                assertEquals(1, contentContainer.getChildCount());
                assertSame(rootScreenPage,
                        contentContainer.getChildAt(0));
                assertNotNull(findText(
                        detailActivity.getWindow().getDecorView(),
                        section.title));
                instrumentation.runOnMainSync(detailActivity::finish);
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

            if (activity.findViewById(
                    R.id.settings_detail_container) != null) {
                return;
            }

            instrumentation.runOnMainSync(() -> rootScroll.scrollTo(
                    0,
                    rootScroll.getChildAt(0).getHeight()));
            instrumentation.waitForIdleSync();
            int expectedScrollY = rootScroll.getScrollY();
            assertTrue(expectedScrollY > 0);

            View sectionRow =
                    (View) sectionTitle.getParent().getParent();
            Instrumentation.ActivityMonitor monitor =
                    instrumentation.addMonitor(
                            StreamSettings.class.getName(),
                            null,
                            false);
            instrumentation.runOnMainSync(sectionRow::performClick);
            StreamSettings detailActivity = (StreamSettings)
                    instrumentation.waitForMonitorWithTimeout(
                            monitor,
                            2_000);
            instrumentation.removeMonitor(monitor);
            assertNotNull(detailActivity);
            instrumentation.runOnMainSync(detailActivity::finish);
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

    @Test
    public void sectionNavigationUsesSystemStackOrStableWideShell()
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

            if (wideLayout) {
                instrumentation.runOnMainSync(sectionRow::performClick);
                assertEquals(1, contentContainer.getChildCount());
                assertSame(originalScreenPage,
                        contentContainer.getChildAt(0));
                assertSame(originalWideDetail,
                        contentContainer.findViewById(
                                R.id.settings_detail_container));
                assertSettled(contentContainer);
                return;
            }

            Instrumentation.ActivityMonitor monitor =
                    instrumentation.addMonitor(
                            StreamSettings.class.getName(),
                            null,
                            false);
            instrumentation.runOnMainSync(sectionRow::performClick);
            StreamSettings detailActivity = (StreamSettings)
                    instrumentation.waitForMonitorWithTimeout(
                            monitor,
                            2_000);
            instrumentation.removeMonitor(monitor);

            assertNotNull(detailActivity);
            assertNotSame(activity, detailActivity);
            assertEquals(
                    activity.getWindow()
                            .getAttributes()
                            .windowAnimations,
                    detailActivity.getWindow()
                            .getAttributes()
                            .windowAnimations);
            assertSettled(contentContainer);
            assertSame(originalScreenPage,
                    contentContainer.getChildAt(0));
            FrameLayout detailContainer = detailActivity.findViewById(
                    R.id.settings_content_container);
            assertNotNull(detailContainer);
            assertSettled(detailContainer);
            instrumentation.runOnMainSync(detailActivity::finish);
            instrumentation.waitForIdleSync();
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

    private interface ActivityAssertion {
        void verify(StreamSettings activity);
    }
}
