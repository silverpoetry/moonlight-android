package com.limelight.stream.launch.android;

import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.nvstream.http.NvApp;
import com.limelight.stream.launch.StreamLaunchRequest;

import java.security.cert.CertificateEncodingException;
import java.util.Objects;

/** Translates detached host/app protocol models into a launch document. */
public final class AndroidStreamLaunchRequestFactory {
    private AndroidStreamLaunchRequestFactory() {
    }

    public static StreamLaunchRequest create(
            HostRuntimeSnapshot host,
            NvApp app,
            String uniqueId) throws CertificateEncodingException {
        HostRuntimeSnapshot source = Objects.requireNonNull(
                host,
                "host");
        Objects.requireNonNull(app, "app");
        HostEndpoint activeEndpoint = source.getConnectionState()
                .getActiveEndpoint();
        if (activeEndpoint == null) {
            throw new IllegalArgumentException(
                    "host has no active endpoint");
        }

        byte[] encodedCertificate =
                source.getPersistedHost().getPinnedCertificate() == null
                ? null
                : source.getPersistedHost().getPinnedCertificate()
                        .getEncoded();
        return new StreamLaunchRequest(
                activeEndpoint.getAddress(),
                activeEndpoint.getPort(),
                source.getConnectionState().getHttpsPort(),
                app.getAppName(),
                app.getAppId(),
                app.isHdrSupported(),
                uniqueId,
                source.getRecord().getIdentity().getId().getValue(),
                source.getRecord().getIdentity().getAdvertisedName(),
                encodedCertificate);
    }
}
