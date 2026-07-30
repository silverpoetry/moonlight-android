package com.limelight.nvstream.http;

import java.io.IOException;
import java.io.InterruptedIOException;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Executes synchronous OkHttp calls while preserving Java interruption semantics.
 *
 * <p>OkHttp's Kotlin implementation may propagate {@link InterruptedException}
 * from an interruptible internal wait even though {@link Call#execute()} only
 * declares {@link IOException}. Java callers must normalize that exception at
 * the synchronous call boundary so an intentional worker cancellation cannot
 * escape as an uncaught exception and terminate the Android process.</p>
 */
final class OkHttpCalls {
    private OkHttpCalls() {
    }

    static Response execute(Call call) throws IOException {
        try {
            return call.execute();
        }
        catch (Exception error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();

                InterruptedIOException interrupted =
                        new InterruptedIOException("HTTP call interrupted");
                interrupted.initCause(error);
                throw interrupted;
            }
            if (error instanceof IOException) {
                throw (IOException) error;
            }
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }

            throw new IOException("Unexpected HTTP call failure", error);
        }
    }
}
