package com.limelight.ui;

import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;

public class StreamImeInputConnection extends BaseInputConnection {
    private final Editable editable = new SpannableStringBuilder();
    private final StreamInputCallbacks inputCallbacks;
    private String composingText = "";

    public StreamImeInputConnection(View targetView, StreamInputCallbacks inputCallbacks) {
        super(targetView, true);
        this.inputCallbacks = inputCallbacks;
        syncEditable();
    }

    @Override
    public Editable getEditable() {
        return editable;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        String committedText = text != null ? text.toString() : "";

        if (!TextUtils.isEmpty(committedText)) {
            replaceRemoteText(composingText, committedText);
        }

        composingText = "";
        syncEditable();
        return true;
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        String newComposingText = text != null ? text.toString() : "";
        replaceRemoteText(composingText, newComposingText);
        composingText = newComposingText;
        syncEditable();
        return true;
    }

    @Override
    public boolean finishComposingText() {
        composingText = "";
        syncEditable();
        return true;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        return deleteSurroundingTextInternal(beforeLength, afterLength);
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        return deleteSurroundingTextInternal(beforeLength, afterLength);
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (inputCallbacks == null || event == null) {
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            return inputCallbacks.handleKeyDown(event);
        }
        else if (event.getAction() == KeyEvent.ACTION_UP) {
            return inputCallbacks.handleKeyUp(event);
        }

        return false;
    }

    private boolean deleteSurroundingTextInternal(int beforeLength, int afterLength) {
        if (beforeLength > 0 && inputCallbacks != null) {
            inputCallbacks.handleImeBackspace(beforeLength);
            composingText = trimTrailingCodePoints(composingText, beforeLength);
        }

        if (afterLength > 0 && inputCallbacks != null) {
            inputCallbacks.handleImeForwardDelete(afterLength);
        }

        syncEditable();
        return true;
    }

    private void replaceRemoteText(String oldText, String newText) {
        if (inputCallbacks == null) {
            return;
        }

        int commonPrefixLength = findCommonPrefixLength(oldText, newText);
        String oldSuffix = oldText.substring(commonPrefixLength);
        String newSuffix = newText.substring(commonPrefixLength);

        int backspaceCount = codePointCount(oldSuffix);
        if (backspaceCount > 0) {
            inputCallbacks.handleImeBackspace(backspaceCount);
        }

        if (!newSuffix.isEmpty()) {
            inputCallbacks.handleImeText(newSuffix);
        }
    }

    private void syncEditable() {
        editable.clear();
        editable.append(composingText);
        Selection.setSelection(editable, editable.length());
    }

    private static int findCommonPrefixLength(String first, String second) {
        int commonPrefixLength = 0;
        int maxLength = Math.min(first.length(), second.length());

        while (commonPrefixLength < maxLength &&
                first.charAt(commonPrefixLength) == second.charAt(commonPrefixLength)) {
            commonPrefixLength++;
        }

        return commonPrefixLength;
    }

    private static int codePointCount(String text) {
        return text.codePointCount(0, text.length());
    }

    private static String trimTrailingCodePoints(String text, int count) {
        if (count <= 0 || text.isEmpty()) {
            return text;
        }

        int endIndex = text.offsetByCodePoints(text.length(), -Math.min(count, codePointCount(text)));
        return text.substring(0, endIndex);
    }
}
