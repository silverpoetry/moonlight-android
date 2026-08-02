package com.limelight.ui.filepush;

import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.PersistedHost;

import java.util.Objects;

/** Selects a usable upload endpoint without exposing repository DTOs to UI. */
public final class FilePushTargetFactory {
    private FilePushTargetFactory() {
    }

    public static FilePushTargetSelection create(
            PersistedHost host) {
        PersistedHost source = Objects.requireNonNull(host, "host");
        if (source.getPinnedCertificate() == null) {
            return FilePushTargetSelection.missing();
        }

        HostRecord record = source.getRecord();
        HostEndpoint endpoint = selectEndpoint(record);
        if (endpoint == null) {
            return FilePushTargetSelection.missing();
        }
        return FilePushTargetSelection.found(new FilePushTarget(
                record.getIdentity().getAdvertisedName(),
                endpoint,
                source.getPinnedCertificate()));
    }

    private static HostEndpoint selectEndpoint(HostRecord record) {
        HostEndpoint endpoint = record.getEndpoint(
                HostEndpoint.Kind.MANUAL);
        if (endpoint == null) {
            endpoint = record.getEndpoint(
                    HostEndpoint.Kind.LOCAL_IPV4);
        }
        if (endpoint == null) {
            endpoint = record.getEndpoint(
                    HostEndpoint.Kind.LOCAL_IPV6);
        }
        if (endpoint == null) {
            endpoint = record.getEndpoint(
                    HostEndpoint.Kind.REMOTE);
        }
        return endpoint;
    }
}
