package com.limelight.computers.session;

import java.util.Objects;

/** Transport-independent use case for ending the host's active session. */
public final class HostQuitUseCase {
    public enum Outcome {
        QUIT,
        REJECTED,
        NOT_SESSION_OWNER
    }

    public interface Backend {
        boolean quit() throws Exception;
    }

    /** Raised by a backend when another client owns the active session. */
    public static final class NotSessionOwnerException
            extends Exception {
        public NotSessionOwnerException(Throwable cause) {
            super("The active session belongs to another client", cause);
        }
    }

    public Outcome execute(Backend backend) throws Exception {
        Objects.requireNonNull(backend, "backend");
        try {
            return backend.quit()
                    ? Outcome.QUIT
                    : Outcome.REJECTED;
        }
        catch (NotSessionOwnerException error) {
            return Outcome.NOT_SESSION_OWNER;
        }
    }
}
