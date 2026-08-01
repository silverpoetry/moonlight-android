package com.limelight.ui.gamemenu;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

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
public final class AndroidGameMenuController
        implements StreamGameMenuHost.MenuSession {
    private static final int MENU_WIDTH_DP = 364;

    private final FragmentActivity activity;
    private final GameMenuSession<GameMenuFragment> session =
            new GameMenuSession<>();

    private GameMenuFragment menu;
    private boolean destroyed;

    public AndroidGameMenuController(FragmentActivity activity) {
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

        Fragment restored = activity.getSupportFragmentManager()
                .findFragmentByTag(GameMenuFragment.FRAGMENT_TAG);
        if (restored instanceof GameMenuFragment) {
            menu = (GameMenuFragment) restored;
            session.open(menu, inputDevice);
            return;
        }

        menu = GameMenuFragment.newInstance(
                UiHelper.dpToPx(activity, MENU_WIDTH_DP));
        session.open(menu, inputDevice);
        menu.show(activity.getSupportFragmentManager());
    }

    public boolean isVisible() {
        return menu != null && menu.isVisible();
    }

    @Override
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

    @Override
    public boolean isMouseEmulationAvailable() {
        return session.isMouseEmulationAvailable();
    }

    @Override
    public void toggleMouseEmulation() {
        if (!destroyed) {
            session.toggleMouseEmulation();
        }
    }

    @Override
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
