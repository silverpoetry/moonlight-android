package com.limelight.binding.video;

import android.media.MediaCodecInfo;
import android.os.Build;
import android.util.Range;

import androidx.annotation.RequiresApi;

import com.limelight.LimeLog;
import com.limelight.settings.stream.StreamDecoderSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Discovers Android hardware decoders and freezes their stream capabilities
 * before the renderer starts its codec lifecycle.
 */
final class AndroidDecoderDiscovery {
    private static final String MIME_AVC = "video/avc";
    private static final String MIME_HEVC = "video/hevc";
    private static final String MIME_AV1 = "video/av01";

    static final class Selection {
        final MediaCodecInfo avcDecoder;
        final MediaCodecInfo hevcDecoder;
        final MediaCodecInfo av1Decoder;
        final DecoderCapabilityProfile capabilityProfile;

        private Selection(
                MediaCodecInfo avcDecoder,
                MediaCodecInfo hevcDecoder,
                MediaCodecInfo av1Decoder,
                DecoderCapabilityProfile capabilityProfile) {
            this.avcDecoder = avcDecoder;
            this.hevcDecoder = hevcDecoder;
            this.av1Decoder = av1Decoder;
            this.capabilityProfile = capabilityProfile;
        }
    }

    private AndroidDecoderDiscovery() {
    }

    static Selection discover(
            StreamDecoderSettings settings,
            boolean requestedHdr,
            int consecutiveCrashCount) {
        Objects.requireNonNull(settings, "settings");

        MediaCodecInfo avcDecoder = findAvcDecoder();
        logSelection("AVC", avcDecoder, true);

        MediaCodecInfo hevcDecoder = findHevcDecoder(
                settings,
                requestedHdr,
                avcDecoder);
        logSelection("HEVC", hevcDecoder, false);

        MediaCodecInfo av1Decoder = findAv1Decoder(settings);
        logSelection("AV1", av1Decoder, false);

        DecoderCapabilityProfile capabilityProfile =
                createCapabilityProfile(
                        settings,
                        avcDecoder,
                        hevcDecoder,
                        av1Decoder,
                        consecutiveCrashCount);
        return new Selection(
                avcDecoder,
                hevcDecoder,
                av1Decoder,
                capabilityProfile);
    }

    private static MediaCodecInfo findAvcDecoder() {
        MediaCodecInfo decoder =
                MediaCodecHelper.findProbableSafeDecoder(
                        MIME_AVC,
                        MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
        if (decoder == null) {
            decoder = MediaCodecHelper.findFirstDecoder(MIME_AVC);
        }
        return decoder;
    }

    private static MediaCodecInfo findHevcDecoder(
            StreamDecoderSettings settings,
            boolean requestedHdr,
            MediaCodecInfo avcDecoder) {
        if (!DecoderSelectionPolicy.shouldAttemptHevc(
                settings.getVideoFormat())) {
            return null;
        }

        // Prefer hardware AVC over an unknown HEVC implementation unless the
        // user, HDR, resolution, or measured performance requires HEVC.
        MediaCodecInfo hevcDecoder =
                MediaCodecHelper.findProbableSafeDecoder(MIME_HEVC, -1);
        if (hevcDecoder == null ||
                MediaCodecHelper.decoderIsWhitelistedForHevc(
                        hevcDecoder)) {
            return hevcDecoder;
        }

        LimeLog.info(
                "Found HEVC decoder, but it's not whitelisted - " +
                        hevcDecoder.getName());
        DecoderSelectionPolicy.NonWhitelistedHevcDecision decision =
                DecoderSelectionPolicy.evaluateNonWhitelistedHevc(
                        settings.getVideoFormat(),
                        requestedHdr,
                        settings.getWidth(),
                        settings.getHeight());
        switch (decision) {
            case ACCEPT_FORCED:
                LimeLog.info(
                        "Forcing HEVC enabled despite " +
                                "non-whitelisted decoder");
                break;
            case ACCEPT_HDR_REQUIRED:
                LimeLog.info("Forcing HEVC enabled for HDR streaming");
                break;
            case ACCEPT_RESOLUTION_REQUIRED:
                LimeLog.info(
                        "Forcing HEVC enabled for over 4K streaming");
                break;
            case CHECK_PERFORMANCE:
                if (avcDecoder == null ||
                        !canMeetWithHevcAndNotAvc(
                                settings,
                                hevcDecoder,
                                avcDecoder)) {
                    return null;
                }
                LimeLog.info(
                        "Using non-whitelisted HEVC decoder to meet " +
                                "performance point");
                break;
            default:
                throw new AssertionError(decision);
        }
        return hevcDecoder;
    }

    private static MediaCodecInfo findAv1Decoder(
            StreamDecoderSettings settings) {
        if (!DecoderSelectionPolicy.shouldAttemptAv1(
                settings.getVideoFormat())) {
            return null;
        }

        MediaCodecInfo decoder =
                MediaCodecHelper.findProbableSafeDecoder(MIME_AV1, -1);
        if (decoder != null &&
                !MediaCodecHelper.isDecoderWhitelistedForAv1(decoder)) {
            LimeLog.info(
                    "Found AV1 decoder, but it's not whitelisted - " +
                            decoder.getName());
            LimeLog.info(
                    "Forcing AV1 enabled despite non-whitelisted decoder");
        }
        return decoder;
    }

    private static boolean canMeetWithHevcAndNotAvc(
            StreamDecoderSettings settings,
            MediaCodecInfo hevcDecoder,
            MediaCodecInfo avcDecoder) {
        MediaCodecInfo.VideoCapabilities avcCapabilities =
                avcDecoder.getCapabilitiesForType(MIME_AVC)
                        .getVideoCapabilities();
        MediaCodecInfo.VideoCapabilities hevcCapabilities =
                hevcDecoder.getCapabilitiesForType(MIME_HEVC)
                        .getVideoCapabilities();
        return !canMeetTarget(settings, avcCapabilities) &&
                canMeetTarget(settings, hevcCapabilities);
    }

    private static boolean canMeetTarget(
            StreamDecoderSettings settings,
            MediaCodecInfo.VideoCapabilities capabilities) {
        return DecoderPerformanceEvaluator.canMeetTarget(
                Build.VERSION.SDK_INT,
                settings.getWidth(),
                settings.getHeight(),
                settings.getFps(),
                new AndroidVideoCapabilities(
                        capabilities,
                        settings));
    }

    private static DecoderCapabilityProfile createCapabilityProfile(
            StreamDecoderSettings settings,
            MediaCodecInfo avcDecoder,
            MediaCodecInfo hevcDecoder,
            MediaCodecInfo av1Decoder,
            int consecutiveCrashCount) {
        int avcOptimalSlicesPerFrame = 0;
        int hevcOptimalSlicesPerFrame = 0;
        boolean directSubmit = false;
        boolean refFrameInvalidationAvc = false;
        boolean refFrameInvalidationHevc = false;
        boolean refFrameInvalidationAv1 = false;

        if (avcDecoder != null) {
            String decoderName = avcDecoder.getName();
            directSubmit =
                    MediaCodecHelper.decoderCanDirectSubmit(decoderName);
            refFrameInvalidationAvc = MediaCodecHelper
                    .decoderSupportsRefFrameInvalidationAvc(
                            decoderName,
                            settings.getHeight());
            avcOptimalSlicesPerFrame = MediaCodecHelper
                    .getDecoderOptimalSlicesPerFrame(decoderName);

            if (directSubmit) {
                LimeLog.info(
                        "Decoder " + decoderName +
                                " will use direct submit");
            }
            if (refFrameInvalidationAvc) {
                LimeLog.info(
                        "Decoder " + decoderName +
                                " will use reference frame invalidation " +
                                "for AVC");
            }
            logSlicePreference(
                    decoderName,
                    avcOptimalSlicesPerFrame);
        }

        if (hevcDecoder != null) {
            String decoderName = hevcDecoder.getName();
            refFrameInvalidationHevc = MediaCodecHelper
                    .decoderSupportsRefFrameInvalidationHevc(hevcDecoder);
            hevcOptimalSlicesPerFrame = MediaCodecHelper
                    .getDecoderOptimalSlicesPerFrame(decoderName);
            if (refFrameInvalidationHevc) {
                LimeLog.info(
                        "Decoder " + decoderName +
                                " will use reference frame invalidation " +
                                "for HEVC");
            }
            logSlicePreference(
                    decoderName,
                    hevcOptimalSlicesPerFrame);
        }

        if (av1Decoder != null) {
            refFrameInvalidationAv1 = MediaCodecHelper
                    .decoderSupportsRefFrameInvalidationAv1(av1Decoder);
            if (refFrameInvalidationAv1) {
                LimeLog.info(
                        "Decoder " + av1Decoder.getName() +
                                " will use reference frame invalidation " +
                                "for AV1");
            }
        }

        if (consecutiveCrashCount % 2 == 1) {
            LimeLog.warning("Disabling RFI due to previous crash");
        }

        DecoderCapabilityProfile profile =
                DecoderCapabilityProfile.create(
                        directSubmit,
                        refFrameInvalidationAvc,
                        refFrameInvalidationHevc,
                        refFrameInvalidationAv1,
                        avcOptimalSlicesPerFrame,
                        hevcOptimalSlicesPerFrame,
                        consecutiveCrashCount);
        LimeLog.info(
                "Requesting " + profile.getOptimalSlicesPerFrame() +
                        " slices per frame");
        return profile;
    }

    private static void logSelection(
            String codec,
            MediaCodecInfo decoder,
            boolean missingIsWarning) {
        if (decoder != null) {
            LimeLog.info(
                    "Selected " + codec + " decoder: " +
                            decoder.getName());
        } else if (missingIsWarning) {
            LimeLog.warning("No " + codec + " decoder found");
        } else {
            LimeLog.info("No " + codec + " decoder found");
        }
    }

    private static void logSlicePreference(
            String decoderName,
            int slicesPerFrame) {
        LimeLog.info(
                "Decoder " + decoderName + " wants " +
                        slicesPerFrame + " slices per frame");
    }

    private static final class AndroidVideoCapabilities
            implements DecoderPerformanceEvaluator.Capabilities {
        private final MediaCodecInfo.VideoCapabilities capabilities;
        private final StreamDecoderSettings settings;

        private AndroidVideoCapabilities(
                MediaCodecInfo.VideoCapabilities capabilities,
                StreamDecoderSettings settings) {
            this.capabilities = Objects.requireNonNull(
                    capabilities,
                    "capabilities");
            this.settings = Objects.requireNonNull(settings, "settings");
        }

        @Override
        @RequiresApi(Build.VERSION_CODES.Q)
        public List<DecoderPerformanceEvaluator.PerformancePoint>
                getSupportedPerformancePoints() {
            List<MediaCodecInfo.VideoCapabilities.PerformancePoint>
                    androidPoints =
                    capabilities.getSupportedPerformancePoints();
            if (androidPoints == null) {
                return null;
            }

            MediaCodecInfo.VideoCapabilities.PerformancePoint target =
                    new MediaCodecInfo.VideoCapabilities.PerformancePoint(
                            settings.getWidth(),
                            settings.getHeight(),
                            settings.getFps());
            List<DecoderPerformanceEvaluator.PerformancePoint> points =
                    new ArrayList<>(androidPoints.size());
            for (MediaCodecInfo.VideoCapabilities.PerformancePoint point :
                    androidPoints) {
                points.add(() -> point.covers(target));
            }
            return points;
        }

        @Override
        public Double getAchievableFrameRateUpper(
                int width,
                int height) {
            Range<Double> range = capabilities
                    .getAchievableFrameRatesFor(width, height);
            return range == null ? null : range.getUpper();
        }

        @Override
        public boolean isSizeAndRateSupported(
                int width,
                int height,
                double framesPerSecond) {
            return capabilities.areSizeAndRateSupported(
                    width,
                    height,
                    framesPerSecond);
        }
    }
}
