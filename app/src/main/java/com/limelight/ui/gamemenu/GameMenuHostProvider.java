package com.limelight.ui.gamemenu;

/**
 * Activity boundary used by restored stream-menu Fragments to resolve their
 * current session-scoped host.
 */
public interface GameMenuHostProvider {
    GameMenuHost getGameMenuHost();
}
