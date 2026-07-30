package com.limelight.binding.input.protocol;

import com.limelight.binding.input.PointerInputSink;
import com.limelight.nvstream.NvConnection;

import java.util.Objects;

/**
 * Adapts the active Moonlight connection to the input domain output port.
 *
 * <p>The stream session owns the connection. This adapter only forwards calls
 * and intentionally adds no queue, retry, coalescing, or throttling.</p>
 */
public final class NvConnectionPointerInputSink implements PointerInputSink {
    private final NvConnection connection;

    public NvConnectionPointerInputSink(NvConnection connection) {
        this.connection = Objects.requireNonNull(
                connection,
                "connection");
    }

    @Override
    public void sendMousePosition(short x, short y,
                                  short referenceWidth,
                                  short referenceHeight) {
        connection.sendMousePosition(
                x, y, referenceWidth, referenceHeight);
    }

    @Override
    public void sendMouseMove(short deltaX, short deltaY) {
        connection.sendMouseMove(deltaX, deltaY);
    }

    @Override
    public void sendMouseMoveAsMousePosition(
            short deltaX,
            short deltaY,
            short referenceWidth,
            short referenceHeight) {
        connection.sendMouseMoveAsMousePosition(
                deltaX,
                deltaY,
                referenceWidth,
                referenceHeight);
    }

    @Override
    public void sendMouseButtonDown(byte mouseButton) {
        connection.sendMouseButtonDown(mouseButton);
    }

    @Override
    public void sendMouseButtonUp(byte mouseButton) {
        connection.sendMouseButtonUp(mouseButton);
    }

    @Override
    public void sendMouseHighResScroll(short delta) {
        connection.sendMouseHighResScroll(delta);
    }

    @Override
    public void sendMouseHighResHScroll(short delta) {
        connection.sendMouseHighResHScroll(delta);
    }

    @Override
    public int sendTouchEvent(
            byte eventType,
            int pointerId,
            float x,
            float y,
            float pressureOrDistance,
            float contactAreaMajor,
            float contactAreaMinor,
            short rotation) {
        return connection.sendTouchEvent(
                eventType,
                pointerId,
                x,
                y,
                pressureOrDistance,
                contactAreaMajor,
                contactAreaMinor,
                rotation);
    }

    @Override
    public int sendPenEvent(
            byte eventType,
            byte toolType,
            byte penButtons,
            float x,
            float y,
            float pressureOrDistance,
            float contactAreaMajor,
            float contactAreaMinor,
            short rotation,
            byte tilt) {
        return connection.sendPenEvent(
                eventType,
                toolType,
                penButtons,
                x,
                y,
                pressureOrDistance,
                contactAreaMajor,
                contactAreaMinor,
                rotation,
                tilt);
    }

    @Override
    public int sendTouchpadFrameEvent(
            byte contactCount,
            byte[] eventTypes,
            int[] pointerIds,
            float[] x,
            float[] y,
            float[] pressure,
            short rotation,
            short deviceWidthMm,
            short deviceHeightMm,
            byte buttonState) {
        return connection.sendTouchpadFrameEvent(
                contactCount,
                eventTypes,
                pointerIds,
                x,
                y,
                pressure,
                rotation,
                deviceWidthMm,
                deviceHeightMm,
                buttonState);
    }

    @Override
    public int sendTouchpadEvent(
            byte eventType,
            int pointerId,
            float x,
            float y,
            float pressure,
            float contactAreaMajor,
            float contactAreaMinor,
            short rotation,
            short deviceWidthMm,
            short deviceHeightMm,
            byte buttonState) {
        return connection.sendTouchpadEvent(
                eventType,
                pointerId,
                x,
                y,
                pressure,
                contactAreaMajor,
                contactAreaMinor,
                rotation,
                deviceWidthMm,
                deviceHeightMm,
                buttonState);
    }
}
