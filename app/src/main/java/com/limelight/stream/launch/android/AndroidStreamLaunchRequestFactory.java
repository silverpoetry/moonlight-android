package com.limelight.stream.launch.android;

import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.stream.launch.StreamLaunchRequest;

import java.security.cert.CertificateEncodingException;
import java.util.Objects;

/** Translates detached host/app protocol models into a launch document. */
public final class AndroidStreamLaunchRequestFactory {
    private AndroidStreamLaunchRequestFactory() {
    }

    public static StreamLaunchRequest create(
            ComputerDetails computer,
            NvApp app,
            String uniqueId) throws CertificateEncodingException {
        Objects.requireNonNull(computer, "computer");
        Objects.requireNonNull(app, "app");
        if (computer.activeAddress == null) {
            throw new IllegalArgumentException(
                    "computer has no active address");
        }

        byte[] encodedCertificate = computer.serverCert == null
                ? null
                : computer.serverCert.getEncoded();
        return new StreamLaunchRequest(
                computer.activeAddress.address,
                computer.activeAddress.port,
                computer.httpsPort,
                app.getAppName(),
                app.getAppId(),
                app.isHdrSupported(),
                uniqueId,
                computer.uuid,
                computer.name,
                encodedCertificate);
    }
}
