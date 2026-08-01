package com.limelight.preferences;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * XML marker for a finite-choice preference backed by an integer SettingKey.
 *
 * <p>The custom settings renderer reads this metadata directly. Extending the
 * platform preference also keeps the resource meaningful to Android tooling.</p>
 */
public final class IntegerListPreference extends ListPreference {
    public IntegerListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IntegerListPreference(Context context) {
        super(context);
    }
}
