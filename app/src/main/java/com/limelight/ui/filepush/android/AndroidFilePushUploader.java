package com.limelight.ui.filepush.android;

import android.content.Context;
import android.net.Uri;

import com.limelight.binding.PlatformBinding;
import com.limelight.computers.IdentityManager;
import com.limelight.nvstream.filetransfer.DesktopFileUploader;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.ui.filepush.FilePushController;
import com.limelight.ui.filepush.FilePushTarget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Android content-provider and NvHTTP adapter for desktop-file upload. */
public final class AndroidFilePushUploader
        implements FilePushController.Uploader {
    private final Context context;
    private final List<Uri> sourceUris;

    public AndroidFilePushUploader(
            Context context,
            List<Uri> sourceUris) {
        this.context = Objects.requireNonNull(
                context,
                "context").getApplicationContext();
        List<Uri> snapshot = new ArrayList<>(
                Objects.requireNonNull(sourceUris, "sourceUris"));
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException(
                    "sourceUris cannot be empty");
        }
        for (Uri sourceUri : snapshot) {
            Objects.requireNonNull(sourceUri, "sourceUri");
        }
        this.sourceUris = Collections.unmodifiableList(snapshot);
    }

    @Override
    public void upload(
            FilePushTarget target,
            FilePushController.ProgressListener listener)
            throws Exception {
        FilePushTarget destination = Objects.requireNonNull(
                target,
                "target");
        NvHTTP http = new NvHTTP(
                new ComputerDetails.AddressTuple(
                        destination.getEndpoint().getAddress(),
                        destination.getEndpoint().getPort()),
                0,
                new IdentityManager(context).getUniqueId(),
                destination.getPinnedCertificate(),
                PlatformBinding.getCryptoProvider(context));
        DesktopFileUploader.upload(
                context,
                http,
                sourceUris,
                listener::onProgress);
    }
}
