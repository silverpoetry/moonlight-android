package com.limelight.binding.input;

/**
 * Protocol-facing output port for latency-sensitive pointer input.
 *
 * <p>Implementations must be non-blocking from the caller's perspective and
 * preserve call order. This contract does not own the underlying stream
 * session.</p>
 */
public interface PointerInputSink {
    void sendMousePosition(short x, short y,
                           short referenceWidth, short referenceHeight);

    void sendMouseMove(short deltaX, short deltaY);

    void sendMouseMoveAsMousePosition(short deltaX, short deltaY,
                                      short referenceWidth,
                                      short referenceHeight);

    void sendMouseButtonDown(byte mouseButton);

    void sendMouseButtonUp(byte mouseButton);

    void sendMouseHighResScroll(short delta);

    void sendMouseHighResHScroll(short delta);

    int sendTouchEvent(byte eventType, int pointerId, float x, float y,
                       float pressureOrDistance, float contactAreaMajor,
                       float contactAreaMinor, short rotation);

    int sendPenEvent(byte eventType, byte toolType, byte penButtons,
                     float x, float y, float pressureOrDistance,
                     float contactAreaMajor, float contactAreaMinor,
                     short rotation, byte tilt);

    int sendTouchpadFrameEvent(byte contactCount, byte[] eventTypes,
                               int[] pointerIds, float[] x, float[] y,
                               float[] pressure, short rotation,
                               short deviceWidthMm, short deviceHeightMm,
                               byte buttonState);

    int sendTouchpadEvent(byte eventType, int pointerId, float x, float y,
                          float pressure, float contactAreaMajor,
                          float contactAreaMinor, short rotation,
                          short deviceWidthMm, short deviceHeightMm,
                          byte buttonState);
}
