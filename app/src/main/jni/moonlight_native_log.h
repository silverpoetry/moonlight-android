#ifndef MOONLIGHT_NATIVE_LOG_H
#define MOONLIGHT_NATIVE_LOG_H

/*
 * First-party native logging boundary.
 *
 * Android Debug variants define MOONLIGHT_DEBUG_LOGGING from ndk-build.
 * Release variants compile these calls away so high-rate native paths cannot
 * bypass the application's release logging policy.
 */
#ifdef MOONLIGHT_DEBUG_LOGGING
#include <android/log.h>

#define MOONLIGHT_NATIVE_LOG(...) __android_log_print(__VA_ARGS__)
#define MOONLIGHT_NATIVE_VLOG(...) __android_log_vprint(__VA_ARGS__)
#else
#define MOONLIGHT_NATIVE_LOG(...) ((void) 0)
#define MOONLIGHT_NATIVE_VLOG(...) ((void) 0)
#endif

#endif
