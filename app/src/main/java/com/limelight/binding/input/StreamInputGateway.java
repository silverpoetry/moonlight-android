package com.limelight.binding.input;

import android.net.Uri;
import android.view.KeyEvent;

/**
 * Lifecycle-bound input port for the active streaming session.
 *
 * <p>Callers use this interface instead of depending on the stream Activity.
 * Implementations must ignore input when {@link #isInputReady()} is false.</p>
 */
public interface StreamInputGateway {
    boolean isInputReady();

    boolean sendKeyEvent(KeyEvent event);

    void sendRelativeMouseMove(int deltaX, int deltaY);

    void sendMouseButton(int buttonId, boolean down);

    void sendHighResolutionScroll(boolean up);

    void sendImeText(String text);

    void sendImeBackspace(int count);

    void sendImeForwardDelete(int count);

    boolean sendImeContent(Uri contentUri, ImeContentCallback callback);
}
