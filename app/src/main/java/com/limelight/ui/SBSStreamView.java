package com.limelight.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;


public class SBSStreamView extends TextureView {
   private StreamInputCallbacks inputCallbacks;
   private boolean imeActive;

   public SBSStreamView(Context context) {
      super(context);
      init();
   }

   public SBSStreamView(Context context, AttributeSet attrs) {
      super(context, attrs);
      init();
   }

   public SBSStreamView(Context context, AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
      init();
   }

   private double desiredAspectRatio;

   public void setDesiredAspectRatio(double aspectRatio) {
      this.desiredAspectRatio = aspectRatio;
   }

   public void setInputCallbacks(StreamInputCallbacks callbacks) {
      this.inputCallbacks = callbacks;
   }

   public void setImeActive(boolean imeActive) {
      this.imeActive = imeActive;
   }

   public boolean isImeActive() {
      return imeActive;
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
   public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
      if (!imeActive) {
         return null;
      }

      outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT |
              EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT |
              EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
      outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN;
      return new StreamImeInputConnection(this, inputCallbacks);
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

      int measuredHeight, measuredWidth;
      if (widthSize > heightSize * desiredAspectRatio) {
         measuredHeight = heightSize;
         measuredWidth = (int)(measuredHeight * desiredAspectRatio);
      } else {
         measuredWidth = widthSize;
         measuredHeight = (int)(measuredWidth / desiredAspectRatio);
      }

      setMeasuredDimension(measuredWidth, measuredHeight);
   }

   @Override
   public boolean onKeyPreIme(int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
         imeActive = false;
      }

      // This callbacks allows us to override dumb IME behavior like when
      // Samsung's default keyboard consumes Shift+Space.
      if (inputCallbacks != null) {
         if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (inputCallbacks.handleKeyDown(event)) {
               return true;
            }
         }
         else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (inputCallbacks.handleKeyUp(event)) {
               return true;
            }
         }
      }

      return super.onKeyPreIme(keyCode, event);
   }

}
