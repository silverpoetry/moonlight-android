package com.limelight.settings.stream;

import java.util.Set;

/**
 * Persistence boundary for the user-authored resolution catalog.
 */
public interface CustomResolutionRepository {
    Set<CustomResolution> load();

    void add(CustomResolution resolution);

    void remove(CustomResolution resolution);
}
