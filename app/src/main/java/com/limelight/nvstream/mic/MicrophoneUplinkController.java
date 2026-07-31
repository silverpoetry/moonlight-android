package com.limelight.nvstream.mic;

import java.util.Objects;

/**
 * Serializes microphone uplink lifecycle transitions independently of the
 * Android capture and native transport implementation.
 */
public final class MicrophoneUplinkController {
    private final MicrophoneUplinkSessionFactory sessionFactory;

    private MicrophoneUplinkSession activeSession;
    private MicrophoneUplinkState state;
    private String lastMessage;

    public MicrophoneUplinkController(
            MicrophoneUplinkSessionFactory sessionFactory) {
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
            lastMessage = "麦克风已开启";
            return true;
        }
        if (state == MicrophoneUplinkState.STARTING ||
                state == MicrophoneUplinkState.STOPPING) {
            lastMessage = "麦克风状态正在切换";
            return false;
        }
        if (activeSession != null) {
            lastMessage = "上一次麦克风采集仍在停止";
            return false;
        }
        if (!sessionFactory.isSupported()) {
            state = MicrophoneUplinkState.UNAVAILABLE;
            lastMessage = "主机不支持麦克风上行";
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
            lastMessage = "麦克风已开启";
            return true;
        }
        catch (RuntimeException error) {
            lastMessage = "麦克风不可用：" + error.getMessage();
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
        lastMessage = "麦克风已关闭";
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
}
