package com.limelight.binding.input.touch;

import org.junit.Test;

import static com.limelight.binding.input.touch.PinchZoomGesture.Action.*;
import static com.limelight.binding.input.touch.PinchZoomGesture.Result.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PinchZoomGestureTest {
    private final PinchZoomGesture gesture = new PinchZoomGesture(8f, 300L);

    @Test
    public void singleTapAndDoubleTapAreNeverDeferred() {
        assertEquals(FORWARD, send(DOWN, 0, 1, 100, 200, 0, 0));
        assertEquals(FORWARD, send(UP, 20, 1, 100, 200, 0, 0));
        assertEquals(FORWARD, send(DOWN, 50, 1, 100, 200, 0, 0));
        assertEquals(FORWARD, send(UP, 70, 1, 100, 200, 0, 0));
    }

    @Test
    public void twoFingerTapReleasesItsOriginalPrefix() {
        beginPair();
        assertEquals(FORWARD, send(POINTER_UP, 30, 2, 100, 200, 300, 200));
        assertEquals(FORWARD, send(UP, 40, 1, 100, 200, 0, 0));
    }

    @Test
    public void parallelScrollCannotTurnIntoLocalZoomLater() {
        beginPair();
        assertEquals(FORWARD, send(MOVE, 25, 2, 120, 200, 320, 200));
        assertEquals(FORWARD, send(MOVE, 40, 2, 80, 200, 360, 200));
    }

    @Test
    public void smallSpanNoiseDuringScrollStaysRemote() {
        beginPair();
        assertEquals(FORWARD, send(MOVE, 25, 2, 115, 200, 330, 200));
    }

    @Test
    public void symmetricOpeningPinchAcquiresAndKeepsOwnership() {
        beginPair();
        assertEquals(BEGIN_ZOOM, send(MOVE, 30, 2, 80, 200, 320, 200));
        assertEquals(ZOOM, send(MOVE, 50, 2, 100, 220, 340, 220));
    }

    @Test
    public void closingPinchAndAnchoredFingerAreSupported() {
        beginPair();
        assertEquals(BEGIN_ZOOM, send(MOVE, 30, 2, 100, 200, 270, 200));
    }

    @Test
    public void wideClosingPinchDoesNotRequireMovementProportionalToFingerSpacing() {
        send(DOWN, 0, 1, 100, 200, 0, 0);
        send(POINTER_DOWN, 10, 2, 100, 200, 1500, 200);
        assertEquals(BEGIN_ZOOM, send(MOVE, 60, 2, 112, 200, 1488, 200));
    }

    @Test
    public void slightlyDiagonalAnchoredClosingPinchIsNotPrematurelyCommittedToScroll() {
        beginPair();
        assertEquals(DEFER, send(MOVE, 30, 2, 100, 200, 288, 211));
        assertEquals(BEGIN_ZOOM, send(MOVE, 60, 2, 100, 200, 270, 214));
    }

    @Test
    public void sequentialFingerPlacementAndGradualOpeningHaveTimeToResolve() {
        send(DOWN, 0, 1, 100, 200, 0, 0);
        assertEquals(DEFER, send(POINTER_DOWN, 160, 2, 100, 200, 300, 200));
        assertEquals(DEFER, send(MOVE, 270, 2, 96, 200, 304, 200));
        assertEquals(BEGIN_ZOOM, send(MOVE, 310, 2, 86, 200, 314, 200));
    }

    @Test
    public void verticalAnchoredClosingPinchUsesTheSameMovementRule() {
        send(DOWN, 0, 1, 200, 100, 0, 0);
        send(POINTER_DOWN, 10, 2, 200, 100, 200, 1500);
        assertEquals(DEFER, send(MOVE, 30, 2, 200, 100, 211, 1488));
        assertEquals(BEGIN_ZOOM, send(MOVE, 60, 2, 200, 100, 214, 1470));
    }

    @Test
    public void diagonalOpeningAndClosingUseTheSameSlopAllowance() {
        for (float span : new float[] {200, 1400}) {
            for (int direction : new int[] {-1, 1}) {
                for (float movement : new float[] {30, 50}) {
                    send(DOWN, 0, 1, 100, 200, 0, 0);
                    send(POINTER_DOWN, 10, 2, 100, 200, 100 + span, 200);
                    assertEquals("span=" + span + ", direction=" + direction +
                                    ", movement=" + movement,
                            BEGIN_ZOOM, send(MOVE, 60, 2, 100, 200,
                                    100 + span + direction * movement, 200 + movement));
                    send(UP, 70, 1, 100, 200, 0, 0);
                }
            }
        }
    }

    @Test
    public void rotationWithOnlySmallSpanNoiseStillGoesToRemoteInput() {
        beginPair();
        assertEquals(FORWARD, send(MOVE, 30, 2, 100, 200, 100, 408));
        assertEquals(FORWARD, send(MOVE, 50, 2, 100, 200, 100, 450));
    }

    @Test
    public void consecutiveOpeningAndClosingPinchesStartFresh() {
        for (int attempt = 0; attempt < 20; attempt++) {
            long time = attempt * 350L;
            send(DOWN, time, 1, 100, 200, 0, 0);
            assertEquals(DEFER, send(POINTER_DOWN, time + 120, 2, 100, 200, 300, 200));
            float end = attempt % 2 == 0 ? 340 : 260;
            assertEquals(BEGIN_ZOOM, send(MOVE, time + 230, 2, 100, 200, end, 200));
            assertEquals(CONSUME, send(POINTER_UP, time + 240, 2, 100, 200, end, 200));
            assertEquals(CONSUME, send(UP, time + 250, 1, 100, 200, 0, 0));
        }
    }

    @Test
    public void rotationIsNotClassifiedAsZoom() {
        beginPair();
        assertEquals(FORWARD, send(MOVE, 30, 2, 200, 100, 200, 300));
    }

    @Test
    public void thirdFingerImmediatelyHandsBackToTheOriginalMode() {
        beginPair();
        assertEquals(FORWARD, send(POINTER_DOWN, 20, 3, 100, 200, 300, 200));
        assertEquals(FORWARD, send(POINTER_UP, 30, 3, 100, 200, 300, 200));
        assertEquals(FORWARD, send(MOVE, 40, 2, 80, 200, 320, 200));
    }

    @Test
    public void singleFingerDragCannotBeStolenEvenAfterReturningToItsOrigin() {
        send(DOWN, 0, 1, 100, 200, 0, 0);
        send(MOVE, 10, 1, 120, 200, 0, 0);
        send(MOVE, 20, 1, 100, 200, 0, 0);
        assertEquals(FORWARD, send(POINTER_DOWN, 30, 2, 100, 200, 300, 200));
    }

    @Test
    public void lateSecondFingerAndLongHoldRemainRemote() {
        send(DOWN, 0, 1, 100, 200, 0, 0);
        assertEquals(FORWARD, send(POINTER_DOWN, 500, 2, 100, 200, 300, 200));
        assertEquals(FORWARD, send(MOVE, 530, 2, 80, 200, 320, 200));
    }

    @Test
    public void undecidedPairHasABoundedDeadline() {
        beginPair();
        assertEquals(310L, gesture.getDeadlineMs());
        assertEquals(DEFER, send(MOVE, 60, 2, 98, 200, 302, 200));
        assertEquals(FORWARD, send(MOVE, 310, 2, 80, 200, 320, 200));
    }

    @Test
    public void competingForcePressCommitsThePairToRemoteInput() {
        beginPair();
        gesture.useRemoteInput();
        assertFalse(gesture.isPending());
        assertEquals(FORWARD, send(MOVE, 30, 2, 80, 200, 320, 200));
    }

    @Test
    public void zoomRegripRebasesAndContinuesUntilAllFingersLift() {
        beginPair();
        send(MOVE, 30, 2, 80, 200, 320, 200);
        assertEquals(CONSUME, send(POINTER_UP, 40, 2, 80, 200, 320, 200));
        assertEquals(CONSUME, send(MOVE, 50, 1, 90, 200, 0, 0));
        assertEquals(REBASE_ZOOM, send(POINTER_DOWN, 60, 2, 90, 200, 310, 200));
        assertEquals(ZOOM, send(MOVE, 70, 2, 90, 200, 280, 200));
        assertEquals(CONSUME, send(POINTER_UP, 80, 2, 90, 200, 280, 200));
        assertEquals(REBASE_ZOOM, send(POINTER_DOWN, 90, 2, 90, 200, 340, 200));
        assertEquals(ZOOM, send(MOVE, 100, 2, 90, 200, 300, 200));
        assertEquals(CONSUME, send(UP, 110, 1, 90, 200, 0, 0));
        assertFalse(gesture.isConsuming());
        assertEquals(FORWARD, send(DOWN, 150, 1, 200, 200, 0, 0));
    }

    @Test
    public void disablingZoomConsumesTheTailWithoutContinuingToScale() {
        beginPair();
        send(MOVE, 30, 2, 80, 200, 320, 200);
        gesture.useRemoteInput();
        assertTrue(gesture.isConsuming());
        assertEquals(CONSUME, send(MOVE, 40, 2, 60, 200, 340, 200));
        assertEquals(CONSUME, send(CANCEL, 50, 2, 60, 200, 340, 200));
        assertFalse(gesture.isConsuming());
    }

    private void beginPair() {
        assertEquals(FORWARD, send(DOWN, 0, 1, 100, 200, 0, 0));
        assertEquals(DEFER, send(POINTER_DOWN, 10, 2, 100, 200, 300, 200));
    }

    private PinchZoomGesture.Result send(PinchZoomGesture.Action action, long time,
                                        int count, float x0, float y0, float x1, float y1) {
        return gesture.onTouch(action, time, count, x0, y0, x1, y1);
    }
}
