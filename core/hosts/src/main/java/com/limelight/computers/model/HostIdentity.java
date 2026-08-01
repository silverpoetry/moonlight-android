package com.limelight.computers.model;

import java.util.Objects;

/** Stable host identity separated from user-controlled presentation. */
public final class HostIdentity {
    public static final int MAXIMUM_NAME_LENGTH = 256;

    private final HostId id;
    private final String advertisedName;
    private final String userAlias;

    public HostIdentity(
            HostId id,
            String advertisedName,
            String userAlias) {
        this.id = Objects.requireNonNull(id, "id");
        this.advertisedName = requireName(
                advertisedName, "advertisedName");
        this.userAlias = normalizeAlias(userAlias);
    }

    private static String requireName(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() ||
                normalized.length() > MAXIMUM_NAME_LENGTH) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        return normalized;
    }

    private static String normalizeAlias(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return requireName(value, "userAlias");
    }

    public HostId getId() {
        return id;
    }

    public String getAdvertisedName() {
        return advertisedName;
    }

    public String getUserAlias() {
        return userAlias;
    }

    public String getDisplayName() {
        return userAlias == null ? advertisedName : userAlias;
    }

    public HostIdentity withAdvertisedName(String name) {
        return new HostIdentity(id, name, userAlias);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HostIdentity)) {
            return false;
        }
        HostIdentity identity = (HostIdentity) other;
        return id.equals(identity.id) &&
                advertisedName.equals(identity.advertisedName) &&
                Objects.equals(userAlias, identity.userAlias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, advertisedName, userAlias);
    }
}
