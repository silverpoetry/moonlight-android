package com.limelight.ui;

import android.view.KeyEvent;

public interface StreamInputCallbacks {
    boolean handleKeyUp(KeyEvent event);
    boolean handleKeyDown(KeyEvent event);
    void handleImeText(String text);
    void handleImeBackspace(int count);
    void handleImeForwardDelete(int count);
}
