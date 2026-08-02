package com.limelight.computers.http.android;

import android.content.Context;

import com.limelight.binding.PlatformBinding;
import com.limelight.computers.http.HostHttpTarget;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvHTTP;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Objects;

/** Creates credential-aware NvHTTP clients from detached Android inputs. */
public final class AndroidNvHttpClientFactory {
    private AndroidNvHttpClientFactory() {
    }

    public static NvHTTP create(
            Context context,
            HostRuntimeSnapshot host,
            String uniqueId) throws IOException {
        HostRuntimeSnapshot source = Objects.requireNonNull(
                host,
                "host");
        final HostHttpTarget target;
        try {
            target = HostHttpTarget.from(source, uniqueId);
        }
        catch (IllegalArgumentException | NullPointerException error) {
            throw new IOException(error.getMessage(), error);
        }
        return create(context, target);
    }

    public static NvHTTP create(
            Context context,
            ComputerDetails computer,
            String uniqueId) throws IOException {
        Objects.requireNonNull(computer, "computer");
        if (computer.activeAddress == null) {
            throw new IOException("Host has no active address");
        }
        return create(
                context,
                computer.activeAddress,
                computer.httpsPort,
                uniqueId,
                computer.serverCert);
    }

    public static NvHTTP create(
            Context context,
            HostHttpTarget target) throws IOException {
        HostHttpTarget source = Objects.requireNonNull(target, "target");
        return create(
                context,
                new ComputerDetails.AddressTuple(
                        source.getAddress(),
                        source.getPort()),
                source.getHttpsPort(),
                source.getUniqueId(),
                source.getPinnedCertificate());
    }

    private static NvHTTP create(
            Context context,
            ComputerDetails.AddressTuple activeAddress,
            int httpsPort,
            String uniqueId,
            X509Certificate serverCertificate) throws IOException {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(activeAddress, "activeAddress");
        Objects.requireNonNull(uniqueId, "uniqueId");
        return new NvHTTP(
                activeAddress,
                httpsPort,
                uniqueId,
                serverCertificate,
                PlatformBinding.getCryptoProvider(context));
    }
}
