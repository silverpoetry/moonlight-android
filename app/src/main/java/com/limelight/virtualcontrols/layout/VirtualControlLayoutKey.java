package com.limelight.virtualcontrols.layout;

import java.util.Objects;

/**
 * Validated identity of one editable virtual-control layout document.
 *
 * <p>The persisted profile identifiers intentionally match the historical
 * values so existing layouts remain readable. Callers cannot supply arbitrary
 * file names; storage adapters own the legacy file-name mapping.</p>
 */
public final class VirtualControlLayoutKey {
    private final VirtualControlLayoutKind kind;
    private final String profileId;
    private final VirtualControlLayoutOrientation orientation;

    private VirtualControlLayoutKey(
            VirtualControlLayoutKind kind,
            String profileId,
            VirtualControlLayoutOrientation orientation) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.orientation =
                Objects.requireNonNull(orientation, "orientation");
        this.profileId = validateProfileId(kind, profileId);
    }

    public static VirtualControlLayoutKey keyboard(
            String profileId,
            VirtualControlLayoutOrientation orientation) {
        return new VirtualControlLayoutKey(
                VirtualControlLayoutKind.KEYBOARD,
                profileId,
                orientation);
    }

    public static VirtualControlLayoutKey gamepad(
            String profileId,
            VirtualControlLayoutOrientation orientation) {
        return new VirtualControlLayoutKey(
                VirtualControlLayoutKind.GAMEPAD,
                profileId,
                orientation);
    }

    public VirtualControlLayoutKind getKind() {
        return kind;
    }

    public String getProfileId() {
        return profileId;
    }

    public VirtualControlLayoutOrientation getOrientation() {
        return orientation;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VirtualControlLayoutKey)) {
            return false;
        }
        VirtualControlLayoutKey that =
                (VirtualControlLayoutKey) other;
        return kind == that.kind &&
                profileId.equals(that.profileId) &&
                orientation == that.orientation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, profileId, orientation);
    }

    @Override
    public String toString() {
        return "VirtualControlLayoutKey{" +
                "kind=" + kind +
                ", profileId='" + profileId + '\'' +
                ", orientation=" + orientation +
                '}';
    }

    private static String validateProfileId(
            VirtualControlLayoutKind kind,
            String profileId) {
        if (!VirtualControlLayoutProfiles.isKnown(
                kind,
                profileId)) {
            throw new IllegalArgumentException(
                    "Unknown " + kind + " layout profile: " + profileId);
        }
        return profileId;
    }
}
