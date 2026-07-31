package com.limelight.binding.input;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RazerKishiHapticsControllerTest {
    @Test
    public void firstFrameRefreshesStartsAndSubmits() {
        Fixture fixture = new Fixture();
        FakeSidecar sidecar = fixture.addCandidate(7);

        assertTrue(fixture.controller.submitFrame(
                new byte[]{1},
                0.5f));

        assertEquals(1, fixture.backend.enumerationCount);
        assertEquals(1, sidecar.startCount);
        assertEquals(1, sidecar.submitCount);
    }

    @Test
    public void emptyRefreshIsThrottledUntilIntervalExpires() {
        Fixture fixture = new Fixture();

        assertFalse(fixture.controller.submitFrame(
                new byte[]{1},
                1f));
        fixture.clock.now = 1_499;
        assertFalse(fixture.controller.submitFrame(
                new byte[]{1},
                1f));
        fixture.clock.now = 1_500;
        assertFalse(fixture.controller.submitFrame(
                new byte[]{1},
                1f));

        assertEquals(2, fixture.backend.enumerationCount);
    }

    @Test
    public void stoppedSidecarIsClosedBeforeReplacement() {
        Fixture fixture = new Fixture();
        FakeSidecar first = fixture.addCandidate(7);
        fixture.controller.refresh();
        first.started = false;
        FakeSidecar second = new FakeSidecar();
        fixture.backend.candidates.set(
                0,
                new FakeCandidate(7, second));

        fixture.controller.refresh();

        assertEquals(1, first.stopCount);
        assertEquals(1, second.startCount);
    }

    @Test
    public void disabledPolicyStopsAllSidecars() {
        Fixture fixture = new Fixture();
        FakeSidecar sidecar = fixture.addCandidate(7);
        fixture.controller.refresh();

        fixture.audioEnabled = false;
        fixture.controller.refresh();

        assertEquals(1, sidecar.stopCount);
        assertFalse(fixture.controller.submitFrame(
                new byte[]{1},
                1f));
    }

    @Test
    public void destroyIsIdempotentAndTerminal() {
        Fixture fixture = new Fixture();
        FakeSidecar sidecar = fixture.addCandidate(7);
        fixture.controller.refresh();

        fixture.controller.destroy();
        fixture.controller.destroy();

        assertEquals(1, sidecar.stopCount);
        assertFalse(fixture.controller.submitFrame(
                new byte[]{1},
                1f));
        assertEquals(1, fixture.backend.enumerationCount);
    }

    @Test
    public void featureDisableStopsActiveSidecars() {
        Fixture fixture = new Fixture();
        FakeSidecar sidecar = fixture.addCandidate(7);
        fixture.controller.refresh();

        fixture.backend.featureEnabled = false;
        fixture.controller.refresh();

        assertEquals(1, sidecar.stopCount);
        assertFalse(fixture.controller.submitFrame(
                new byte[]{1},
                1f));
    }

    @Test
    public void failedStartIsClosedAndNotRetained() {
        Fixture fixture = new Fixture();
        FakeSidecar sidecar = fixture.addCandidate(7);
        sidecar.startSucceeds = false;

        fixture.controller.refresh();

        assertEquals(1, sidecar.startCount);
        assertEquals(1, sidecar.stopCount);
        assertFalse(fixture.controller.submitFrame(
                new byte[]{1},
                1f));
        assertEquals(1, fixture.backend.enumerationCount);
    }

    private static final class Fixture {
        final FakeBackend backend = new FakeBackend();
        final FakeClock clock = new FakeClock();
        boolean audioEnabled = true;
        final RazerKishiHapticsController controller =
                new RazerKishiHapticsController(
                        backend,
                        () -> audioEnabled,
                        clock);

        FakeSidecar addCandidate(int deviceId) {
            FakeSidecar sidecar = new FakeSidecar();
            backend.candidates.add(
                    new FakeCandidate(deviceId, sidecar));
            return sidecar;
        }
    }

    private static final class FakeClock
            implements RazerKishiHapticsController.Clock {
        long now;

        @Override
        public long uptimeMillis() {
            return now;
        }
    }

    private static final class FakeBackend
            implements RazerKishiHapticsController.Backend {
        final List<RazerKishiHapticsController.Candidate> candidates =
                new ArrayList<>();
        boolean featureEnabled = true;
        int enumerationCount;

        @Override
        public boolean isFeatureEnabled() {
            return featureEnabled;
        }

        @Override
        public List<RazerKishiHapticsController.Candidate>
                getCandidates() {
            enumerationCount++;
            return new ArrayList<>(candidates);
        }
    }

    private static final class FakeCandidate
            implements RazerKishiHapticsController.Candidate {
        private final int deviceId;
        private final FakeSidecar sidecar;

        private FakeCandidate(int deviceId, FakeSidecar sidecar) {
            this.deviceId = deviceId;
            this.sidecar = sidecar;
        }

        @Override
        public int getDeviceId() {
            return deviceId;
        }

        @Override
        public String getDescription() {
            return "device=" + deviceId;
        }

        @Override
        public boolean hasPermission() {
            return true;
        }

        @Override
        public RazerKishiHapticsController.Sidecar open() {
            return sidecar;
        }
    }

    private static final class FakeSidecar
            implements RazerKishiHapticsController.Sidecar {
        boolean started;
        boolean startSucceeds = true;
        int startCount;
        int submitCount;
        int stopCount;

        @Override
        public boolean isStarted() {
            return started;
        }

        @Override
        public boolean start() {
            startCount++;
            started = startSucceeds;
            return startSucceeds;
        }

        @Override
        public boolean submitFrame(
                byte[] frame,
                float intensityGain) {
            submitCount++;
            return started;
        }

        @Override
        public void stop() {
            stopCount++;
            started = false;
        }
    }
}
