package com.limelight.preferences;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public final class SettingsChangeEffectSchedulerTest {
    @Test
    public void immediateEffectRunsAndDestroyCancelsDelayedEffect()
            throws InterruptedException {
        AtomicInteger reloads = new AtomicInteger();
        AtomicInteger refreshes = new AtomicInteger();
        AtomicInteger recreates = new AtomicInteger();
        SettingsChangeEffectScheduler[] scheduler =
                new SettingsChangeEffectScheduler[1];

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            scheduler[0] = new SettingsChangeEffectScheduler(
                    reloads::incrementAndGet,
                    refreshes::incrementAndGet,
                    recreates::incrementAndGet);
            scheduler[0].schedule(
                    SettingsMutationController.ChangeEffect.refresh(0));
            scheduler[0].schedule(
                    SettingsMutationController.ChangeEffect.reload(80));
            scheduler[0].destroy();
        });

        Thread.sleep(160);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(1, refreshes.get());
        assertEquals(0, reloads.get());
        assertEquals(0, recreates.get());
    }
}
