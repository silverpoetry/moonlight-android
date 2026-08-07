package com.limelight.nvstream.mic;

import java.util.Objects;

/**
 * Serializes microphone uplink lifecycle transitions independently of the
 * Android capture and native transport implementation.
 */
public final class MicrophoneUplinkController {
    public interface Messages {
        String enabled();
        String changing();
        String previousCaptureStopping();
        String hostUnsupported();
        String unavailable(String reason);
        String disabled();
    }

    private final MicrophoneUplinkSessionFactory sessionFactory;
    private final Messages messages;

    private MicrophoneUplinkSession activeSession;
    private MicrophoneUplinkState state;
    private String lastMessage;

    public MicrophoneUplinkController(
            MicrophoneUplinkSessionFactory sessionFactory) {
        this(defaultMessages(), sessionFactory);
    }

    public MicrophoneUplinkController(
            Messages messages,
            MicrophoneUplinkSessionFactory sessionFactory) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.sessionFactory = Objects.requireNonNull(
                sessionFactory,
                "sessionFactory");
        state = sessionFactory.isSupported()
                ? MicrophoneUplinkState.OFF
                : MicrophoneUplinkState.UNAVAILABLE;
    }

    public synchronized boolean isSupported() {
        return sessionFactory.isSupported();
    }

    public synchronized boolean isActive() {
        refreshState();
        return state == MicrophoneUplinkState.ON;
    }

    public synchronized MicrophoneUplinkState getState() {
        refreshState();
        return state;
    }

    public synchronized String getLastMessage() {
        return lastMessage;
    }

    public synchronized boolean start() {
        refreshState();
        if (state == MicrophoneUplinkState.ON &&
                activeSession != null) {
            lastMessage = messages.enabled();
            return true;
        }
        if (state == MicrophoneUplinkState.STARTING ||
                state == MicrophoneUplinkState.STOPPING) {
            lastMessage = messages.changing();
            return false;
        }
        if (activeSession != null) {
            lastMessage = messages.previousCaptureStopping();
            return false;
        }
        if (!sessionFactory.isSupported()) {
            state = MicrophoneUplinkState.UNAVAILABLE;
            lastMessage = messages.hostUnsupported();
            return false;
        }

        state = MicrophoneUplinkState.STARTING;
        try {
            MicrophoneUplinkSession session =
                    Objects.requireNonNull(
                            sessionFactory.create(),
                            "sessionFactory.create()");
            activeSession = session;
            if (!session.start()) {
                lastMessage = session.getLastErrorMessage();
                session.stop();
                activeSession = null;
                state = MicrophoneUplinkState.ERROR;
                return false;
            }

            state = MicrophoneUplinkState.ON;
            lastMessage = messages.enabled();
            return true;
        }
        catch (RuntimeException error) {
            lastMessage = messages.unavailable(error.getMessage());
            stopFailedStartSession();
            state = MicrophoneUplinkState.ERROR;
            return false;
        }
    }

    public synchronized boolean stop() {
        refreshState();
        if ((state == MicrophoneUplinkState.OFF ||
                state == MicrophoneUplinkState.UNAVAILABLE) &&
                activeSession == null) {
            return true;
        }

        state = MicrophoneUplinkState.STOPPING;
        if (activeSession != null && !activeSession.stop()) {
            lastMessage = activeSession.getLastErrorMessage();
            state = MicrophoneUplinkState.ERROR;
            return false;
        }

        activeSession = null;
        state = sessionFactory.isSupported()
                ? MicrophoneUplinkState.OFF
                : MicrophoneUplinkState.UNAVAILABLE;
        lastMessage = messages.disabled();
        return true;
    }

    private void refreshState() {
        if (activeSession == null &&
                (state == MicrophoneUplinkState.OFF ||
                        state ==
                                MicrophoneUplinkState.UNAVAILABLE)) {
            state = sessionFactory.isSupported()
                    ? MicrophoneUplinkState.OFF
                    : MicrophoneUplinkState.UNAVAILABLE;
            return;
        }
        if (state == MicrophoneUplinkState.ON &&
                (activeSession == null ||
                        !activeSession.isRunning())) {
            if (activeSession != null) {
                lastMessage =
                        activeSession.getLastErrorMessage();
            }
            activeSession = null;
            state = MicrophoneUplinkState.ERROR;
        }
    }

    private void stopFailedStartSession() {
        if (activeSession == null) {
            return;
        }
        try {
            activeSession.stop();
        }
        catch (RuntimeException ignored) {
            // The original startup failure remains the primary error.
        }
        finally {
            activeSession = null;
        }
    }

    private static Messages defaultMessages() {
        return new Messages() {
            public String enabled() { return "Microphone enabled"; }
            public String changing() { return "Microphone state is changing"; }
            public String previousCaptureStopping() {
                return "The previous microphone capture is still stopping";
            }
            public String hostUnsupported() {
                return "The host does not support microphone uplink";
            }
            public String unavailable(String reason) {
                return "Microphone unavailable: " + reason;
            }
            public String disabled() { return "Microphone disabled"; }
        };
    }
}
