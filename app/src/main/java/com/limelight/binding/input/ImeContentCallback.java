package com.limelight.binding.input;

/** Completion callback for content explicitly committed by an input method. */
public interface ImeContentCallback {
    void onComplete(boolean success);
}
