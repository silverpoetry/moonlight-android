package com.limelight.ui.stream;

import android.os.Handler;

import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.utils.ServerHelper;

import java.util.Objects;

/** Creates session-scoped connectivity diagnostics for Android streams. */
public final class AndroidStreamFailureDiagnosticsFactory {
    private static final int CONNECTION_TEST_PORT = 443;

    private AndroidStreamFailureDiagnosticsFactory() {
    }

    public static StreamSessionPresentationController.Diagnostics create(
            Handler mainHandler) {
        Handler handler = Objects.requireNonNull(
                mainHandler,
                "mainHandler");
        StreamFailureDiagnostics diagnostics =
                StreamFailureDiagnostics.create(
                        portFlags -> MoonBridge.testClientConnectivity(
                                ServerHelper.CONNECTION_TEST_SERVER,
                                CONNECTION_TEST_PORT,
                                portFlags),
                        handler::post);
        return new StreamSessionPresentationController.Diagnostics() {
            @Override
            public boolean request(
                    int portFlags,
                    Callback callback) {
                return diagnostics.request(
                        portFlags,
                        result -> callback.onResult(
                                result.getPortFlags(),
                                result.getProbeResultOr(
                                        MoonBridge
                                                .ML_TEST_RESULT_INCONCLUSIVE)));
            }

            @Override
            public void destroy() {
                diagnostics.destroy();
            }
        };
    }
}
