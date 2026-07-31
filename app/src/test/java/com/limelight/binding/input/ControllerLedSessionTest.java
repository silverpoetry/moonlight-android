package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public final class ControllerLedSessionTest {
    @Test
    public void convertsSignedProtocolComponentsAsUnsignedRgb() {
        RecordingTarget target = new RecordingTarget(true);
        ControllerLedSession session = new ControllerLedSession(target);

        session.setColor((byte) 0x80, (byte) 0xFF, (byte) 0x01);

        assertEquals(0xFF80FF01, target.lastArgb);
        assertEquals(1, target.applyCount);
    }

    @Test
    public void unavailableTargetDoesNotReceiveColor() {
        RecordingTarget target = new RecordingTarget(false);
        ControllerLedSession session = new ControllerLedSession(target);

        session.setColor((byte) 1, (byte) 2, (byte) 3);

        assertFalse(session.isAvailable());
        assertEquals(0, target.applyCount);
    }

    @Test
    public void migrationReappliesDesiredColorToReplacementTarget() {
        RecordingTarget oldTarget = new RecordingTarget(true);
        ControllerLedSession oldSession = new ControllerLedSession(oldTarget);
        oldSession.setColor((byte) 4, (byte) 5, (byte) 6);

        ControllerLedSession.DesiredState desiredState =
                oldSession.snapshotDesiredState();
        oldSession.destroy();
        RecordingTarget newTarget = new RecordingTarget(true);
        ControllerLedSession newSession = new ControllerLedSession(newTarget);
        newSession.restoreDesiredState(desiredState);

        assertEquals(1, oldTarget.closeCount);
        assertEquals(0xFF040506, newTarget.lastArgb);
        assertEquals(1, newTarget.applyCount);
    }

    @Test
    public void migrationWithoutDesiredColorDoesNotWriteTarget() {
        ControllerLedSession oldSession = new ControllerLedSession(
                new RecordingTarget(true));
        RecordingTarget newTarget = new RecordingTarget(true);
        ControllerLedSession newSession = new ControllerLedSession(newTarget);

        newSession.restoreDesiredState(oldSession.snapshotDesiredState());

        assertEquals(0, newTarget.applyCount);
    }

    @Test
    public void destroyClosesOnceAndRejectsLaterWrites() {
        RecordingTarget target = new RecordingTarget(true);
        ControllerLedSession session = new ControllerLedSession(target);

        session.destroy();
        session.destroy();
        session.setColor((byte) 1, (byte) 2, (byte) 3);

        assertEquals(1, target.closeCount);
        assertEquals(0, target.applyCount);
        assertFalse(session.isAvailable());
    }

    @Test
    public void unavailableSessionHasNoCapability() {
        ControllerLedSession session = ControllerLedSession.unavailable();

        assertFalse(session.isAvailable());
        session.setColor((byte) 1, (byte) 2, (byte) 3);
        session.destroy();
        assertFalse(session.isAvailable());
    }

    private static final class RecordingTarget
            implements ControllerLedSession.Target {
        private final boolean available;
        int applyCount;
        int closeCount;
        int lastArgb;

        private RecordingTarget(boolean available) {
            this.available = available;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public void applyArgb(int argb) {
            applyCount++;
            lastArgb = argb;
        }

        @Override
        public void close() {
            closeCount++;
        }
    }
}
