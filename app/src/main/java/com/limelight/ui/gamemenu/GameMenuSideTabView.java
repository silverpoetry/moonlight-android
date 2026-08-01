package com.limelight.ui.gamemenu;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limelight.R;

/**
 * Side-navigation action with deterministic icon and label geometry.
 *
 * <p>The platform Button compound-drawable layout varies by theme and OEM.
 * Keeping the icon and label as explicit children guarantees a fixed visual
 * gap while this composite still exposes itself as one accessible button.</p>
 */
public final class GameMenuSideTabView extends LinearLayout {
    public GameMenuSideTabView(Context context) {
        this(context, null);
    }

    public GameMenuSideTabView(
            Context context,
            AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameMenuSideTabView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setGravity(android.view.Gravity.CENTER);
        LayoutInflater.from(context).inflate(
                R.layout.view_game_menu_side_tab_content,
                this,
                true);

        TypedArray attributes = context.obtainStyledAttributes(
                attrs,
                R.styleable.GameMenuSideTabView,
                defStyleAttr,
                0);
        try {
            ImageView icon = findViewById(
                    R.id.game_menu_side_tab_icon);
            TextView label = findViewById(
                    R.id.game_menu_side_tab_label);
            icon.setImageResource(attributes.getResourceId(
                    R.styleable.GameMenuSideTabView_sideTabIcon,
                    0));
            CharSequence text = attributes.getText(
                    R.styleable.GameMenuSideTabView_sideTabText);
            label.setText(text);
            setContentDescription(text);
        }
        finally {
            attributes.recycle();
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(
            AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Button.class.getName());
    }
}
