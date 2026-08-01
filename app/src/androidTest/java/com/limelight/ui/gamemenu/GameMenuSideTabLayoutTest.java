package com.limelight.ui.gamemenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class GameMenuSideTabLayoutTest {
    @Test
    public void sideTabsUseControlledCompoundDrawableSpacing() {
        Context context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        View root = LayoutInflater.from(context).inflate(
                R.layout.dialog_game_menu,
                null,
                false);
        int expectedSpacing = Math.round(
                6 * context.getResources()
                        .getDisplayMetrics()
                        .density);
        int[] sideTabIds = {
                R.id.btn_soft_function,
                R.id.btn_soft_keyboard,
                R.id.btn_desktop,
                R.id.btn_window,
        };

        for (int sideTabId : sideTabIds) {
            GameMenuSideTabView sideTab =
                    root.findViewById(sideTabId);
            assertTrue(sideTab.isClickable());
            ImageView icon = sideTab.findViewById(
                    R.id.game_menu_side_tab_icon);
            TextView label = sideTab.findViewById(
                    R.id.game_menu_side_tab_label);
            LinearLayout.LayoutParams labelLayout =
                    (LinearLayout.LayoutParams) label.getLayoutParams();
            assertEquals(expectedSpacing, labelLayout.topMargin);

            int width = Math.round(
                    64 * context.getResources()
                            .getDisplayMetrics()
                            .density);
            int height = Math.round(
                    180 * context.getResources()
                            .getDisplayMetrics()
                            .density);
            sideTab.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            width,
                            View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(
                            height,
                            View.MeasureSpec.EXACTLY));
            sideTab.layout(0, 0, width, height);
            assertEquals(
                    expectedSpacing,
                    label.getTop() - icon.getBottom());
        }
    }
}
