package com.limelight.ui.filepush;

import com.limelight.computers.model.HostEndpoint;

import java.security.cert.X509Certificate;
import java.util.Objects;

/** Immutable host and credential snapshot for one desktop-file upload. */
public final class FilePushTarget {
    private final String displayName;
    private final HostEndpoint endpoint;
    private final X509Certificate pinnedCertificate;

    public FilePushTarget(
            String displayName,
            HostEndpoint endpoint,
            X509Certificate pinnedCertificate) {
        String normalizedName = Objects.requireNonNull(
                displayName,
                "displayName").trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException(
                    "displayName cannot be empty");
        }
        this.displayName = normalizedName;
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.pinnedCertificate = Objects.requireNonNull(
                pinnedCertificate,
                "pinnedCertificate");
    }

    public String getDisplayName() {
        return displayName;
    }

    public HostEndpoint getEndpoint() {
        return endpoint;
    }

    public String getAddress() {
        return endpoint.getAddress();
    }

    public X509Certificate getPinnedCertificate() {
        return pinnedCertificate;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FilePushTarget)) {
            return false;
        }
        FilePushTarget target = (FilePushTarget) other;
        return displayName.equals(target.displayName) &&
                endpoint.equals(target.endpoint) &&
                pinnedCertificate.equals(target.pinnedCertificate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                displayName,
                endpoint,
                pinnedCertificate);
    }
}
