package com.limelight.settings.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public final class AndroidSettingObserverTest {
    private static final SettingKey<Boolean> TEST_KEY =
            SettingKey.booleanKey(
                    "test.android.setting.observer",
                    false);

    @Test
    public void publishesInitialAndChangedValuesUntilClosed() {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        SettingsRepository repository =
                AndroidSettingsRepository.create(context);
        repository.edit().remove(TEST_KEY).commit();
        AtomicInteger updateCount = new AtomicInteger();
        AtomicBoolean latest = new AtomicBoolean(true);
        AndroidSettingObserver<Boolean> observer =
                new AndroidSettingObserver<>(
                        context,
                        TEST_KEY,
                        value -> {
                            latest.set(value);
                            updateCount.incrementAndGet();
                        });

        try {
            observer.start();
            assertEquals(1, updateCount.get());
            assertFalse(latest.get());

            repository.edit().put(TEST_KEY, true).commit();
            instrumentation.waitForIdleSync();
            assertEquals(2, updateCount.get());
            assertTrue(latest.get());

            observer.close();
            repository.edit().put(TEST_KEY, false).commit();
            instrumentation.waitForIdleSync();
            assertEquals(2, updateCount.get());
            assertTrue(latest.get());
        }
        finally {
            observer.close();
            repository.edit().remove(TEST_KEY).commit();
        }
    }
}
