package com.limelight.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

import com.limelight.binding.input.StreamInputGateway;

public class StreamView extends SurfaceView {
    private double desiredAspectRatio;
    private StreamInputGateway inputGateway;
    private boolean imeActive;

    public void setDesiredAspectRatio(double aspectRatio) {
        double safeAspectRatio =
                Double.isFinite(aspectRatio) && aspectRatio > 0
                        ? aspectRatio
                        : 0.0;
        if (desiredAspectRatio == safeAspectRatio) {
            return;
        }
        desiredAspectRatio = safeAspectRatio;
        requestLayout();
    }

    public void setInputGateway(StreamInputGateway inputGateway) {
        this.inputGateway = inputGateway;
    }

    public void setImeActive(boolean imeActive) {
        this.imeActive = imeActive;
    }

    public boolean isImeActive() {
        return imeActive;
    }

    public StreamView(Context context) {
        super(context);
        init();
    }

    public StreamView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StreamView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public StreamView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return imeActive;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (!imeActive) {
            return null;
        }

        outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT |
                EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT |
                EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN;
        EditorInfoCompat.setContentMimeTypes(
                outAttrs,
                new String[]{"image/*"});
        StreamImeInputConnection inputConnection =
                new StreamImeInputConnection(this, inputGateway);
        return InputConnectionCompat.createWrapper(
                inputConnection,
                outAttrs,
                (contentInfo, flags, opts) ->
                        handleImeContent(contentInfo, flags));
    }

    private boolean handleImeContent(
            InputContentInfoCompat contentInfo,
            int flags) {
        if (inputGateway == null || contentInfo == null ||
                !contentInfo.getDescription().hasMimeType("image/*")) {
            return false;
        }

        boolean permissionGranted = false;
        try {
            if ((flags & InputConnectionCompat
                    .INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                contentInfo.requestPermission();
                permissionGranted = true;
            }

            boolean releasePermission = permissionGranted;
            boolean accepted = inputGateway.sendImeContent(
                    contentInfo.getContentUri(),
                    success -> {
                        if (releasePermission) {
                            contentInfo.releasePermission();
                        }
                    });
            if (!accepted && permissionGranted) {
                contentInfo.releasePermission();
            }
            return accepted;
        }
        catch (Throwable error) {
            if (permissionGranted) {
                contentInfo.releasePermission();
            }
            return false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If no fixed aspect ratio has been provided, simply use the default onMeasure() behavior
        if (desiredAspectRatio == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // Based on code from: https://www.buzzingandroid.com/2012/11/easy-measuring-of-custom-views-with-specific-aspect-ratio/
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        StreamLayoutGeometry.Size measuredSize =
                StreamLayoutGeometry.fitWithin(
                        widthSize,
                        heightSize,
                        desiredAspectRatio);
        setMeasuredDimension(measuredSize.width, measuredSize.height);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            imeActive = false;
        }

        // This callbacks allows us to override dumb IME behavior like when
        // Samsung's default keyboard consumes Shift+Space.
        if (inputGateway != null && inputGateway.sendKeyEvent(event)) {
            return true;
        }

        return super.onKeyPreIme(keyCode, event);
    }

}
