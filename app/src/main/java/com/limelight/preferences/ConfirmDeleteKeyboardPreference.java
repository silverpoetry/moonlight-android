package com.limelight.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import static com.limelight.binding.input.virtual_controller.keyboard.KeyBoardControllerConfigurationLoader.OSC_PREFERENCE;
import static com.limelight.binding.input.virtual_controller.keyboard.KeyBoardControllerConfigurationLoader.OSC_PREFERENCE_VALUE;

public class ConfirmDeleteKeyboardPreference extends DialogPreference {
    public ConfirmDeleteKeyboardPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ConfirmDeleteKeyboardPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ConfirmDeleteKeyboardPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConfirmDeleteKeyboardPreference(Context context) {
        super(context);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            String name= PreferenceManager.getDefaultSharedPreferences(getContext()).getString(OSC_PREFERENCE,OSC_PREFERENCE_VALUE);
            getContext().getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply();
        }
    }
}
