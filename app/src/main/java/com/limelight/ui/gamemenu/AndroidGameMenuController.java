package com.limelight.ui.gamemenu;

import android.app.Activity;
import android.app.Fragment;

import com.limelight.binding.input.GameInputDevice;
import com.limelight.utils.UiHelper;

import java.util.Objects;

/**
 * Owns the Android fragment and transient input context for one stream menu.
 *
 * <p>This controller is scoped to its Activity. Call {@link #destroy()} from
 * the Activity's {@code onDestroy()} before releasing the menu's runtime
 * dependencies.</p>
 */
public final class AndroidGameMenuController {
    private static final int MENU_WIDTH_DP = 364;

    private final Activity activity;
    private final GameMenuSession<GameMenuFragment> session =
            new GameMenuSession<>();

    private GameMenuFragment menu;
    private boolean destroyed;

    public AndroidGameMenuController(Activity activity) {
        this.activity = Objects.requireNonNull(activity, "activity");
    }

    public void show(GameInputDevice inputDevice) {
        if (destroyed || activity.isFinishing()) {
            return;
        }

        if (menu != null && !menu.isRemoving()) {
            if (inputDevice != null) {
                session.open(menu, inputDevice);
            }
            return;
        }

        Fragment restored = activity.getFragmentManager()
                .findFragmentByTag(GameMenuFragment.FRAGMENT_TAG);
        if (restored instanceof GameMenuFragment) {
            menu = (GameMenuFragment) restored;
            session.open(menu, inputDevice);
            return;
        }

        menu = GameMenuFragment.newInstance(
                UiHelper.dpToPx(activity, MENU_WIDTH_DP));
        session.open(menu, inputDevice);
        menu.show(activity.getFragmentManager());
    }

    public boolean isVisible() {
        return menu != null && menu.isVisible();
    }

    public void dismiss() {
        if (menu != null && menu.isVisible()) {
            menu.dismiss();
        }
    }

    public void refreshMicrophoneState() {
        if (!destroyed && menu != null) {
            menu.refreshMicrophoneState();
        }
    }

    public boolean isMouseEmulationAvailable() {
        return session.isMouseEmulationAvailable();
    }

    public void toggleMouseEmulation() {
        if (!destroyed) {
            session.toggleMouseEmulation();
        }
    }

    public void onDismissed(GameMenuFragment dismissedMenu) {
        if (session.close(dismissedMenu) && menu == dismissedMenu) {
            menu = null;
        }
    }

    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        if (menu != null) {
            session.close(menu);
            menu = null;
        }
    }
}
