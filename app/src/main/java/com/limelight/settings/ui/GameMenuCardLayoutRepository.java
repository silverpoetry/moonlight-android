package com.limelight.settings.ui;

/**
 * Storage port for stream-menu card references.
 */
public interface GameMenuCardLayoutRepository {
    GameMenuCardLayoutLoadResult load();

    void save(GameMenuCardLayout layout);
}
