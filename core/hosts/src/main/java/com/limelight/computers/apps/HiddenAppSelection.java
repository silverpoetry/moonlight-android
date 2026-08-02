package com.limelight.computers.apps;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Immutable set of app IDs hidden for one host. */
public final class HiddenAppSelection {
    public static final int MAXIMUM_APP_IDS = 10_000;
    private static final HiddenAppSelection EMPTY =
            new HiddenAppSelection(Collections.emptySet());

    private final Set<Integer> appIds;

    private HiddenAppSelection(Set<Integer> appIds) {
        this.appIds = appIds;
    }

    public static HiddenAppSelection empty() {
        return EMPTY;
    }

    public static HiddenAppSelection of(Collection<Integer> appIds) {
        Objects.requireNonNull(appIds, "appIds");
        if (appIds.size() > MAXIMUM_APP_IDS) {
            throw new IllegalArgumentException("Too many hidden app IDs");
        }
        TreeSet<Integer> normalized = new TreeSet<>();
        for (Integer appId : appIds) {
            if (appId == null) {
                throw new IllegalArgumentException("Invalid hidden app ID");
            }
            normalized.add(appId);
        }
        if (normalized.isEmpty()) {
            return EMPTY;
        }
        return new HiddenAppSelection(
                Collections.unmodifiableSet(normalized));
    }

    public Set<Integer> getAppIds() {
        return appIds;
    }
}
