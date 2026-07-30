package com.limelight.nvstream;

import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;

/**
 * Transport owned by a {@link StreamSessionController}.
 */
interface StreamSessionConnection {
    void start(AudioRenderer audioRenderer,
               VideoDecoderRenderer videoDecoderRenderer,
               NvConnectionListener connectionListener);

    void stop();
}
