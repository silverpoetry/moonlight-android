package com.limelight.settings.android;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public final class AndroidSettingsGroupObserverTest {
    private static final SettingKey<Integer> FIRST =
            SettingKey.integerKey(
                    "test.settings.group.first",
                    0,
                    0,
                    100);
    private static final SettingKey<Integer> SECOND =
            SettingKey.integerKey(
                    "test.settings.group.second",
                    0,
                    0,
                    100);

    @Test
    public void relatedChangesPublishOneLifecycleBoundCallback() {
        Context context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        SettingsRepository repository =
                AndroidSettingsRepository.create(context);
        AtomicInteger callbackCount = new AtomicInteger();
        AndroidSettingsGroupObserver observer =
                new AndroidSettingsGroupObserver(
                        context,
                        Arrays.asList(FIRST, SECOND),
                        callbackCount::incrementAndGet);

        try {
            observer.start();
            repository.edit()
                    .put(FIRST, 1)
                    .put(SECOND, 2)
                    .apply();
            InstrumentationRegistry.getInstrumentation()
                    .waitForIdleSync();

            assertEquals(1, callbackCount.get());

            observer.close();
            repository.edit().put(FIRST, 3).apply();
            InstrumentationRegistry.getInstrumentation()
                    .waitForIdleSync();
            assertEquals(1, callbackCount.get());
        }
        finally {
            observer.close();
            repository.edit()
                    .remove(FIRST)
                    .remove(SECOND)
                    .commit();
        }
    }
}
