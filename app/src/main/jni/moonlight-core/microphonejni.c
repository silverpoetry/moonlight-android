#include <jni.h>
#include <pthread.h>
#include <stdint.h>

#include <opus.h>

#include "moonlight-common-c/src/Limelight.h"

#define MICROPHONE_SAMPLE_RATE 48000
#define MICROPHONE_FRAME_SAMPLES 960
#define MICROPHONE_MAX_PACKET_SIZE 1200
#define MICROPHONE_DEFAULT_BITRATE 40000

static pthread_mutex_t microphoneMutex = PTHREAD_MUTEX_INITIALIZER;
static OpusEncoder* microphoneEncoder;
static unsigned int microphoneFrameCount;

JNIEXPORT jboolean JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_isMicrophoneUplinkSupported(
        JNIEnv* env, jclass clazz) {
    (void)env;
    (void)clazz;
    return LiIsMicrophoneUplinkSupported() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_startMicrophoneUplink(
        JNIEnv* env, jclass clazz, jint bitrate) {
    int error;
    int result;

    (void)env;
    (void)clazz;

    pthread_mutex_lock(&microphoneMutex);
    if (microphoneEncoder != NULL) {
        pthread_mutex_unlock(&microphoneMutex);
        return 0;
    }

    if (!LiIsMicrophoneUplinkSupported()) {
        pthread_mutex_unlock(&microphoneMutex);
        return LI_ERR_UNSUPPORTED;
    }

    microphoneEncoder = opus_encoder_create(
        MICROPHONE_SAMPLE_RATE, 1, OPUS_APPLICATION_VOIP, &error);
    if (microphoneEncoder == NULL || error != OPUS_OK) {
        microphoneEncoder = NULL;
        pthread_mutex_unlock(&microphoneMutex);
        return error != OPUS_OK ? error : OPUS_ALLOC_FAIL;
    }

    if (bitrate <= 0) {
        bitrate = MICROPHONE_DEFAULT_BITRATE;
    }
    if ((error = opus_encoder_ctl(microphoneEncoder, OPUS_SET_BITRATE(bitrate))) != OPUS_OK ||
            (error = opus_encoder_ctl(microphoneEncoder, OPUS_SET_VBR(1))) != OPUS_OK ||
            (error = opus_encoder_ctl(microphoneEncoder, OPUS_SET_VBR_CONSTRAINT(1))) != OPUS_OK ||
            (error = opus_encoder_ctl(microphoneEncoder, OPUS_SET_INBAND_FEC(1))) != OPUS_OK ||
            (error = opus_encoder_ctl(microphoneEncoder, OPUS_SET_COMPLEXITY(6))) != OPUS_OK ||
            (error = opus_encoder_ctl(microphoneEncoder, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE))) != OPUS_OK ||
            (error = opus_encoder_ctl(microphoneEncoder, OPUS_SET_LSB_DEPTH(16))) != OPUS_OK ||
            (error = opus_encoder_ctl(microphoneEncoder, OPUS_SET_DTX(0))) != OPUS_OK) {
        opus_encoder_destroy(microphoneEncoder);
        microphoneEncoder = NULL;
        pthread_mutex_unlock(&microphoneMutex);
        return error;
    }

    result = LiStartMicrophoneUplink();
    if (result != 0) {
        opus_encoder_destroy(microphoneEncoder);
        microphoneEncoder = NULL;
        pthread_mutex_unlock(&microphoneMutex);
        return result;
    }

    microphoneFrameCount = 0;
    pthread_mutex_unlock(&microphoneMutex);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendMicrophonePcm(
        JNIEnv* env, jclass clazz, jshortArray pcmArray, jlong captureTimeUs) {
    uint8_t encoded[MICROPHONE_MAX_PACKET_SIZE];
    opus_int16 pcm[MICROPHONE_FRAME_SAMPLES];
    int encodedLength;
    int result;

    (void)clazz;

    if (pcmArray == NULL ||
            (*env)->GetArrayLength(env, pcmArray) != MICROPHONE_FRAME_SAMPLES) {
        return -3;
    }

    (*env)->GetShortArrayRegion(
        env, pcmArray, 0, MICROPHONE_FRAME_SAMPLES, (jshort*)pcm);
    if ((*env)->ExceptionCheck(env)) {
        return -1;
    }

    pthread_mutex_lock(&microphoneMutex);
    if (microphoneEncoder == NULL) {
        pthread_mutex_unlock(&microphoneMutex);
        return LI_ERR_UNSUPPORTED;
    }

    if ((microphoneFrameCount++ % 50) == 0) {
        opus_encoder_ctl(
            microphoneEncoder,
            OPUS_SET_PACKET_LOSS_PERC(LiGetMicrophoneUplinkPacketLossPercent()));
    }

    encodedLength = opus_encode(
        microphoneEncoder,
        pcm,
        MICROPHONE_FRAME_SAMPLES,
        encoded,
        sizeof(encoded));
    if (encodedLength < 0) {
        result = encodedLength;
    }
    else {
        result = LiSendMicrophoneOpusFrame(
            encoded,
            (uint16_t)encodedLength,
            MICROPHONE_FRAME_SAMPLES,
            (uint64_t)captureTimeUs,
            0);
    }

    pthread_mutex_unlock(&microphoneMutex);
    return result;
}

JNIEXPORT void JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_stopMicrophoneUplink(
        JNIEnv* env, jclass clazz) {
    (void)env;
    (void)clazz;

    pthread_mutex_lock(&microphoneMutex);
    LiStopMicrophoneUplink();
    if (microphoneEncoder != NULL) {
        opus_encoder_destroy(microphoneEncoder);
        microphoneEncoder = NULL;
    }
    pthread_mutex_unlock(&microphoneMutex);
}
