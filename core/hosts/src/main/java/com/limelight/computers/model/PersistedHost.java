package com.limelight.computers.model;

import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * One repository value containing metadata and its separately owned pinned
 * credential.
 *
 * <p>The credential is not part of {@link HostRecord}; callers must opt into
 * carrying it at the repository boundary. Network observations therefore
 * cannot overwrite a credential by construction.</p>
 */
public final class PersistedHost {
    private final HostRecord record;
    private final X509Certificate pinnedCertificate;

    public PersistedHost(
            HostRecord record,
            X509Certificate pinnedCertificate) {
        this.record = Objects.requireNonNull(record, "record");
        this.pinnedCertificate = pinnedCertificate;
    }

    public HostRecord getRecord() {
        return record;
    }

    public X509Certificate getPinnedCertificate() {
        return pinnedCertificate;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PersistedHost)) {
            return false;
        }
        PersistedHost host = (PersistedHost) other;
        return record.equals(host.record) &&
                Objects.equals(
                        pinnedCertificate,
                        host.pinnedCertificate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(record, pinnedCertificate);
    }
}
