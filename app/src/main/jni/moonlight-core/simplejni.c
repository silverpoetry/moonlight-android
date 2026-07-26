#include <Limelight.h>

#include <jni.h>
#include <android/log.h>

#include <arpa/inet.h>
#include <string.h>

#include "minisdl.h"
#include "controller_type.h"
#include "controller_list.h"

_Static_assert(sizeof(jbyte) == sizeof(uint8_t), "JNI byte must be 8-bit");
_Static_assert(sizeof(jint) == sizeof(uint32_t), "JNI int must be 32-bit");
_Static_assert(sizeof(jfloat) == sizeof(float), "JNI float must match native float");

JNIEXPORT void JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendMouseMove(JNIEnv *env, jclass clazz, jshort deltaX, jshort deltaY) {
    LiSendMouseMoveEvent(deltaX, deltaY);
}

JNIEXPORT void JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendMousePosition(JNIEnv *env, jclass clazz,
        jshort x, jshort y, jshort referenceWidth, jshort referenceHeight) {
    LiSendMousePositionEvent(x, y, referenceWidth, referenceHeight);
}

JNIEXPORT void JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendMouseButton(JNIEnv *env, jclass clazz, jbyte buttonEvent, jbyte mouseButton) {
    LiSendMouseButtonEvent(buttonEvent, mouseButton);
}

JNIEXPORT void JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendMultiControllerInput(JNIEnv *env, jclass clazz, jshort controllerNumber,
                                                           jshort activeGamepadMask, jint buttonFlags,
                                                           jbyte leftTrigger, jbyte rightTrigger,
                                                           jshort leftStickX, jshort leftStickY,
                                                           jshort rightStickX, jshort rightStickY) {
    LiSendMultiControllerEvent(controllerNumber, activeGamepadMask, buttonFlags,
        leftTrigger, rightTrigger, leftStickX, leftStickY, rightStickX, rightStickY);
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendTouchEvent(JNIEnv *env, jclass clazz,
                                                          jbyte eventType, jint pointerId,
                                                          jfloat x, jfloat y, jfloat pressureOrDistance,
                                                          jfloat contactAreaMajor, jfloat contactAreaMinor,
                                                          jshort rotation) {
    return LiSendTouchEvent(eventType, pointerId, x, y, pressureOrDistance,
                            contactAreaMajor, contactAreaMinor, rotation);
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendTouchpadEvent(JNIEnv *env, jclass clazz,
                                                             jbyte eventType, jint pointerId,
                                                             jfloat x, jfloat y, jfloat pressure,
                                                             jfloat contactAreaMajor, jfloat contactAreaMinor,
                                                             jshort rotation, jshort deviceWidthMm,
                                                             jshort deviceHeightMm, jbyte buttonState) {
    return LiSendTouchpadEvent(eventType, pointerId, x, y, pressure,
                               contactAreaMajor, contactAreaMinor, rotation,
                               deviceWidthMm, deviceHeightMm, buttonState);
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendTouchpadFrameEvent(JNIEnv *env, jclass clazz,
                                                                  jbyte contactCount,
                                                                  jbyteArray eventTypesArray,
                                                                  jintArray pointerIdsArray,
                                                                  jfloatArray xArray,
                                                                  jfloatArray yArray,
                                                                  jfloatArray pressureArray,
                                                                  jshort rotation,
                                                                  jshort deviceWidthMm,
                                                                  jshort deviceHeightMm,
                                                                  jbyte buttonState) {
    if (contactCount < 0) {
        return -3;
    }

    uint8_t count = (uint8_t) contactCount;
    if (count == 0) {
        return LiSendTouchpadFrameEvent(0, NULL, NULL, NULL, NULL, NULL,
                                        rotation, deviceWidthMm, deviceHeightMm, buttonState);
    }

    if (eventTypesArray == NULL || pointerIdsArray == NULL || xArray == NULL ||
        yArray == NULL || pressureArray == NULL ||
        (*env)->GetArrayLength(env, eventTypesArray) < count ||
        (*env)->GetArrayLength(env, pointerIdsArray) < count ||
        (*env)->GetArrayLength(env, xArray) < count ||
        (*env)->GetArrayLength(env, yArray) < count ||
        (*env)->GetArrayLength(env, pressureArray) < count) {
        return -3;
    }

    jbyte* eventTypes = (*env)->GetByteArrayElements(env, eventTypesArray, NULL);
    jint* pointerIds = (*env)->GetIntArrayElements(env, pointerIdsArray, NULL);
    jfloat* x = (*env)->GetFloatArrayElements(env, xArray, NULL);
    jfloat* y = (*env)->GetFloatArrayElements(env, yArray, NULL);
    jfloat* pressure = (*env)->GetFloatArrayElements(env, pressureArray, NULL);

    if (eventTypes == NULL || pointerIds == NULL || x == NULL || y == NULL || pressure == NULL) {
        if (eventTypes != NULL) {
            (*env)->ReleaseByteArrayElements(env, eventTypesArray, eventTypes, JNI_ABORT);
        }
        if (pointerIds != NULL) {
            (*env)->ReleaseIntArrayElements(env, pointerIdsArray, pointerIds, JNI_ABORT);
        }
        if (x != NULL) {
            (*env)->ReleaseFloatArrayElements(env, xArray, x, JNI_ABORT);
        }
        if (y != NULL) {
            (*env)->ReleaseFloatArrayElements(env, yArray, y, JNI_ABORT);
        }
        if (pressure != NULL) {
            (*env)->ReleaseFloatArrayElements(env, pressureArray, pressure, JNI_ABORT);
        }
        return -1;
    }

    int result = LiSendTouchpadFrameEvent(count,
                                           (const uint8_t*) eventTypes,
                                           (const uint32_t*) pointerIds,
                                           (const float*) x,
                                           (const float*) y,
                                           (const float*) pressure,
                                           rotation, deviceWidthMm, deviceHeightMm, buttonState);

    (*env)->ReleaseByteArrayElements(env, eventTypesArray, eventTypes, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, pointerIdsArray, pointerIds, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, xArray, x, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, yArray, y, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, pressureArray, pressure, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendPenEvent(JNIEnv *env, jclass clazz, jbyte eventType,
                                                        jbyte toolType, jbyte penButtons,
                                                        jfloat x, jfloat y, jfloat pressureOrDistance,
                                                        jfloat contactAreaMajor, jfloat contactAreaMinor,
                                                        jshort rotation, jbyte tilt) {
    return LiSendPenEvent(eventType, toolType, penButtons, x, y, pressureOrDistance,
                          contactAreaMajor, contactAreaMinor, rotation, tilt);
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendControllerArrivalEvent(JNIEnv *env, jclass clazz,
                                                                      jbyte controllerNumber,
                                                                      jshort activeGamepadMask,
                                                                      jbyte type,
                                                                      jint supportedButtonFlags,
                                                                      jshort capabilities) {
    return LiSendControllerArrivalEvent(controllerNumber, activeGamepadMask, type, supportedButtonFlags, capabilities);
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendControllerTouchEvent(JNIEnv *env, jclass clazz,
                                                                    jbyte controllerNumber,
                                                                    jbyte eventType,
                                                                    jint pointerId, jfloat x,
                                                                    jfloat y, jfloat pressure) {
    return LiSendControllerTouchEvent(controllerNumber, eventType, pointerId, x, y, pressure);
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendControllerMotionEvent(JNIEnv *env, jclass clazz,
                                                                     jbyte controllerNumber,
                                                                     jbyte motionType, jfloat x,
                                                                     jfloat y, jfloat z) {
    return LiSendControllerMotionEvent(controllerNumber, motionType, x, y, z);
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendControllerBatteryEvent(JNIEnv *env, jclass clazz,
                                                                      jbyte controllerNumber,
                                                                      jbyte batteryState,
                                                                      jbyte batteryPercentage) {
    return LiSendControllerBatteryEvent(controllerNumber, batteryState, batteryPercentage);
}

JNIEXPORT void JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendKeyboardInput(JNIEnv *env, jclass clazz, jshort keyCode, jbyte keyAction, jbyte modifiers, jbyte flags) {
    LiSendKeyboardEvent2(keyCode, keyAction, modifiers, flags);
}

JNIEXPORT void JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendMouseHighResScroll(JNIEnv *env, jclass clazz, jshort scrollAmount) {
    LiSendHighResScrollEvent(scrollAmount);
}

JNIEXPORT void JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendMouseHighResHScroll(JNIEnv *env, jclass clazz, jshort scrollAmount) {
    LiSendHighResHScrollEvent(scrollAmount);
}

JNIEXPORT void JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendUtf8Text(JNIEnv *env, jclass clazz, jstring text) {
    const char* utf8Text = (*env)->GetStringUTFChars(env, text, NULL);
    LiSendUtf8TextEvent(utf8Text, strlen(utf8Text));
    (*env)->ReleaseStringUTFChars(env, text, utf8Text);
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendClipboardText(JNIEnv *env, jclass clazz, jbyteArray text) {
    jsize length = (*env)->GetArrayLength(env, text);
    jbyte* textBuf = (*env)->GetByteArrayElements(env, text, NULL);
    int ret = LiSendClipboardText((const uint8_t*)textBuf, (uint32_t)length);
    (*env)->ReleaseByteArrayElements(env, text, textBuf, JNI_ABORT);
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendClipboardContent(JNIEnv *env, jclass clazz,
                                                                jbyte mimeType, jbyteArray data) {
    if (data == NULL) {
        return -1;
    }

    jsize length = (*env)->GetArrayLength(env, data);
    jbyte* dataBuf = (*env)->GetByteArrayElements(env, data, NULL);
    if (dataBuf == NULL) {
        return -1;
    }
    int ret = LiSendClipboardContent((uint8_t)mimeType,
                                     (const uint8_t*)dataBuf,
                                     (uint32_t)length);
    (*env)->ReleaseByteArrayElements(env, data, dataBuf, JNI_ABORT);
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_sendClipboardBlobReference(
        JNIEnv *env, jclass clazz, jbyte targetMimeType, jstring id,
        jint size, jbyteArray sha256) {
    if (id == NULL || sha256 == NULL || size <= 0 ||
            (*env)->GetArrayLength(env, sha256) != LI_CLIPBOARD_SHA256_BYTES) {
        return -1;
    }

    const char* idChars = (*env)->GetStringUTFChars(env, id, NULL);
    jbyte* digest = (*env)->GetByteArrayElements(env, sha256, NULL);
    if (idChars == NULL || digest == NULL) {
        if (idChars != NULL) {
            (*env)->ReleaseStringUTFChars(env, id, idChars);
        }
        if (digest != NULL) {
            (*env)->ReleaseByteArrayElements(env, sha256, digest, JNI_ABORT);
        }
        return -1;
    }

    int ret = LiSendClipboardBlobReference((uint8_t)targetMimeType,
                                           idChars,
                                           (uint32_t)size,
                                           (const uint8_t*)digest);
    (*env)->ReleaseByteArrayElements(env, sha256, digest, JNI_ABORT);
    (*env)->ReleaseStringUTFChars(env, id, idChars);
    return ret;
}

JNIEXPORT jlong JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_getClipboardOriginId(JNIEnv *env, jclass clazz) {
    return (jlong)LiGetClipboardOriginId();
}

JNIEXPORT void JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_stopConnection(JNIEnv *env, jclass clazz) {
    LiStopConnection();
}

JNIEXPORT void JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_interruptConnection(JNIEnv *env, jclass clazz) {
    LiInterruptConnection();
}

JNIEXPORT jstring JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_getStageName(JNIEnv *env, jclass clazz, jint stage) {
    return (*env)->NewStringUTF(env, LiGetStageName(stage));
}

JNIEXPORT jstring JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_findExternalAddressIP4(JNIEnv *env, jclass clazz, jstring stunHostName, jint stunPort) {
    int err;
    struct in_addr wanAddr;
    const char* stunHostNameStr = (*env)->GetStringUTFChars(env, stunHostName, NULL);

    err = LiFindExternalAddressIP4(stunHostNameStr, stunPort, &wanAddr.s_addr);
    (*env)->ReleaseStringUTFChars(env, stunHostName, stunHostNameStr);

    if (err == 0) {
        char addrStr[INET_ADDRSTRLEN];

        inet_ntop(AF_INET, &wanAddr, addrStr, sizeof(addrStr));

        __android_log_print(ANDROID_LOG_INFO, "moonlight-common-c", "Resolved WAN address to %s", addrStr);

        return (*env)->NewStringUTF(env, addrStr);
    }
    else {
        __android_log_print(ANDROID_LOG_ERROR, "moonlight-common-c", "STUN failed to get WAN address: %d", err);
        return NULL;
    }
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_getPendingAudioDuration(JNIEnv *env, jclass clazz) {
    return LiGetPendingAudioDuration();
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_getPendingVideoFrames(JNIEnv *env, jclass clazz) {
    return LiGetPendingVideoFrames();
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_testClientConnectivity(JNIEnv *env, jclass clazz, jstring testServerHostName, jint referencePort, jint testFlags) {
    int ret;
    const char* testServerHostNameStr = (*env)->GetStringUTFChars(env, testServerHostName, NULL);

    ret = LiTestClientConnectivity(testServerHostNameStr, (unsigned short)referencePort, testFlags);

    (*env)->ReleaseStringUTFChars(env, testServerHostName, testServerHostNameStr);

    return ret;
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_getPortFlagsFromStage(JNIEnv *env, jclass clazz, jint stage) {
    return LiGetPortFlagsFromStage(stage);
}

JNIEXPORT jint JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_getPortFlagsFromTerminationErrorCode(JNIEnv *env, jclass clazz, jint errorCode) {
    return LiGetPortFlagsFromTerminationErrorCode(errorCode);
}

JNIEXPORT jstring JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_stringifyPortFlags(JNIEnv *env, jclass clazz, jint portFlags, jstring separator) {
    const char* separatorStr = (*env)->GetStringUTFChars(env, separator, NULL);
    char outputBuffer[512];

    LiStringifyPortFlags(portFlags, separatorStr, outputBuffer, sizeof(outputBuffer));

    (*env)->ReleaseStringUTFChars(env, separator, separatorStr);
    return (*env)->NewStringUTF(env, outputBuffer);
}

JNIEXPORT jlong JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_getEstimatedRttInfo(JNIEnv *env, jclass clazz) {
    uint32_t rtt, variance;

    if (!LiGetEstimatedRttInfo(&rtt, &variance)) {
        return -1;
    }

    return ((uint64_t)rtt << 32U) | variance;
}

JNIEXPORT jstring JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_getLaunchUrlQueryParameters(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, LiGetLaunchUrlQueryParameters());
}

JNIEXPORT jbyte JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_guessControllerType(JNIEnv *env, jclass clazz, jint vendorId, jint productId) {
    unsigned int unDeviceID = MAKE_CONTROLLER_ID(vendorId, productId);
    for (int i = 0; i < sizeof(arrControllers) / sizeof(arrControllers[0]); i++) {
        if (unDeviceID == arrControllers[i].m_unDeviceID) {
            switch (arrControllers[i].m_eControllerType) {
                case k_eControllerType_XBox360Controller:
                case k_eControllerType_XBoxOneController:
                    return LI_CTYPE_XBOX;

                case k_eControllerType_PS3Controller:
                case k_eControllerType_PS4Controller:
                case k_eControllerType_PS5Controller:
                    return LI_CTYPE_PS;

                case k_eControllerType_WiiController:
                case k_eControllerType_SwitchProController:
                case k_eControllerType_SwitchJoyConLeft:
                case k_eControllerType_SwitchJoyConRight:
                case k_eControllerType_SwitchJoyConPair:
                case k_eControllerType_SwitchInputOnlyController:
                    return LI_CTYPE_NINTENDO;

                default:
                    return LI_CTYPE_UNKNOWN;
            }
        }
    }
    return LI_CTYPE_UNKNOWN;
}

JNIEXPORT jboolean JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_guessControllerHasPaddles(JNIEnv *env, jclass clazz, jint vendorId, jint productId) {
    // Xbox Elite and DualSense Edge controllers have paddles
    return SDL_IsJoystickXboxOneElite(vendorId, productId) || SDL_IsJoystickDualSenseEdge(vendorId, productId);
}

JNIEXPORT jboolean JNICALL
Java_com_limelight_nvstream_jni_MoonBridge_guessControllerHasShareButton(JNIEnv *env, jclass clazz, jint vendorId, jint productId) {
    // Xbox Elite and DualSense Edge controllers have paddles
    return SDL_IsJoystickXboxSeriesX(vendorId, productId);
}
