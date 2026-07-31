package com.limelight.binding.input;

import android.view.KeyEvent;

import com.limelight.nvstream.input.ControllerPacket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerButtonMapperTest {
    @Test
    public void ignoredBackIsNotConsumedAsControllerInput() {
        ControllerButtonMapper mapper = mapperBuilder(0, 0)
                .ignoreBack(true)
                .build();

        assertEquals(
                ControllerButtonMapper.IGNORE,
                remap(
                        mapper,
                        KeyEvent.KEYCODE_BACK,
                        0,
                        false));
    }

    @Test
    public void shareScanCodeMapsOnlyForKnownShareDevice() {
        ControllerButtonMapper mapper = mapperBuilder(0, 0)
                .hasShare(true)
                .build();

        assertEquals(
                KeyEvent.KEYCODE_MEDIA_RECORD,
                remap(
                        mapper,
                        KeyEvent.KEYCODE_UNKNOWN,
                        167,
                        false));
    }

    @Test
    public void sonyClickpadAndEightBitDoModeAreCorrected() {
        ControllerButtonMapper sony =
                mapperBuilder(0x054c, 0).build();
        ControllerButtonMapper eightBitDo =
                mapperBuilder(0x2dc8, 0).build();

        assertEquals(
                KeyEvent.KEYCODE_BUTTON_1,
                remap(
                        sony,
                        KeyEvent.KEYCODE_BUTTON_SELECT,
                        317,
                        false));
        assertEquals(
                KeyEvent.KEYCODE_BUTTON_MODE,
                remap(
                        eightBitDo,
                        KeyEvent.KEYCODE_UNKNOWN,
                        306,
                        false));
    }

    @Test
    public void legacySwitchMappingIsSdkGated() {
        ControllerButtonMapper legacy =
                ControllerButtonMapper.builder(
                                0x057e,
                                0x2009,
                                28)
                        .build();
        ControllerButtonMapper modern =
                ControllerButtonMapper.builder(
                                0x057e,
                                0x2009,
                                29)
                        .build();

        assertEquals(
                KeyEvent.KEYCODE_BUTTON_A,
                remap(
                        legacy,
                        KeyEvent.KEYCODE_UNKNOWN,
                        0x130,
                        false));
        assertEquals(
                KeyEvent.KEYCODE_UNKNOWN,
                remap(
                        modern,
                        KeyEvent.KEYCODE_UNKNOWN,
                        0x130,
                        false));
    }

    @Test
    public void joyConCorrectionFollowsRuntimeSetting() {
        ControllerButtonMapper leftJoyCon =
                mapperBuilder(0x057e, 0x2006).build();
        ControllerButtonMapper rightJoyCon =
                mapperBuilder(0x057e, 0x2007).build();

        assertEquals(
                KeyEvent.KEYCODE_UNKNOWN,
                remap(
                        leftJoyCon,
                        KeyEvent.KEYCODE_UNKNOWN,
                        546,
                        false));
        assertEquals(
                KeyEvent.KEYCODE_DPAD_LEFT,
                remap(
                        leftJoyCon,
                        KeyEvent.KEYCODE_UNKNOWN,
                        546,
                        true));
        assertEquals(
                KeyEvent.KEYCODE_BUTTON_Y,
                remap(
                        rightJoyCon,
                        KeyEvent.KEYCODE_UNKNOWN,
                        307,
                        true));
    }

    @Test
    public void linuxAndNonStandardDualShockMappingsRemainDistinct() {
        ControllerButtonMapper linux =
                mapperBuilder(0x054c, 0)
                        .linuxStandardFaceButtons(true)
                        .build();
        ControllerButtonMapper nonStandard =
                mapperBuilder(0x054c, 0)
                        .nonStandardDualShock4(true)
                        .build();

        assertEquals(
                KeyEvent.KEYCODE_BUTTON_A,
                remap(
                        linux,
                        KeyEvent.KEYCODE_UNKNOWN,
                        304,
                        false));
        assertEquals(
                KeyEvent.KEYCODE_BUTTON_X,
                remap(
                        nonStandard,
                        KeyEvent.KEYCODE_UNKNOWN,
                        304,
                        false));
        assertEquals(
                ControllerButtonMapper.CONSUME,
                remap(
                        nonStandard,
                        KeyEvent.KEYCODE_UNKNOWN,
                        999,
                        false));
    }

    @Test
    public void servalAndLegacyXboxButtonsAreCorrected() {
        ControllerButtonMapper serval =
                mapperBuilder(0, 0)
                        .serval(true)
                        .build();
        ControllerButtonMapper xbox =
                mapperBuilder(0, 0)
                        .nonStandardXboxBluetooth(true)
                        .build();

        assertEquals(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                remap(
                        serval,
                        KeyEvent.KEYCODE_UNKNOWN,
                        314,
                        false));
        assertEquals(
                KeyEvent.KEYCODE_BUTTON_MODE,
                remap(
                        xbox,
                        KeyEvent.KEYCODE_MENU,
                        0,
                        false));
    }

    @Test
    public void kunaiPaddlesAndRawDpadAreCorrected() {
        ControllerButtonMapper kunai =
                mapperBuilder(0x0b05, 0x7900).build();
        ControllerButtonMapper noHat =
                mapperBuilder(0, 0).build();
        ControllerButtonMapper withHat =
                mapperBuilder(0, 0)
                        .hasHatAxes(true)
                        .build();

        assertEquals(
                KeyEvent.KEYCODE_BUTTON_START,
                remap(
                        kunai,
                        KeyEvent.KEYCODE_UNKNOWN,
                        264,
                        false));
        assertEquals(
                KeyEvent.KEYCODE_DPAD_UP,
                remap(
                        noHat,
                        KeyEvent.KEYCODE_UNKNOWN,
                        706,
                        false));
        assertEquals(
                KeyEvent.KEYCODE_UNKNOWN,
                remap(
                        withHat,
                        KeyEvent.KEYCODE_UNKNOWN,
                        706,
                        false));
    }

    @Test
    public void softKeyboardBackMapsToFaceButton() {
        ControllerButtonMapper mapper =
                mapperBuilder(0, 0).build();

        assertEquals(
                KeyEvent.KEYCODE_BUTTON_B,
                mapper.remap(
                        KeyEvent.KEYCODE_BACK,
                        0,
                        KeyEvent.FLAG_SOFT_KEYBOARD,
                        false,
                        false));
    }

    @Test
    public void realStartAndSelectDisableFallbackMappings() {
        ControllerButtonMapper mapper =
                mapperBuilder(0, 0)
                        .backIsStart(true)
                        .modeIsSelect(true)
                        .build();

        assertEquals(
                KeyEvent.KEYCODE_BUTTON_START,
                remap(
                        mapper,
                        KeyEvent.KEYCODE_BACK,
                        0,
                        false));
        assertEquals(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                remap(
                        mapper,
                        KeyEvent.KEYCODE_BUTTON_MODE,
                        0,
                        false));

        remap(
                mapper,
                KeyEvent.KEYCODE_BUTTON_START,
                0,
                false);
        remap(
                mapper,
                KeyEvent.KEYCODE_BUTTON_SELECT,
                0,
                false);

        assertEquals(
                KeyEvent.KEYCODE_BACK,
                remap(
                        mapper,
                        KeyEvent.KEYCODE_BACK,
                        0,
                        false));
        assertEquals(
                KeyEvent.KEYCODE_BUTTON_MODE,
                remap(
                        mapper,
                        KeyEvent.KEYCODE_BUTTON_MODE,
                        0,
                        false));
    }

    @Test
    public void searchFallbackAndFaceButtonFlipAreExplicit() {
        ControllerButtonMapper mapper =
                mapperBuilder(0, 0)
                        .searchIsMode(true)
                        .build();

        assertEquals(
                KeyEvent.KEYCODE_BUTTON_MODE,
                remap(
                        mapper,
                        KeyEvent.KEYCODE_SEARCH,
                        0,
                        false));
        assertEquals(
                KeyEvent.KEYCODE_BUTTON_B,
                ControllerButtonMapper.flipFaceButtons(
                        KeyEvent.KEYCODE_BUTTON_A));
        assertEquals(
                KeyEvent.KEYCODE_BUTTON_X,
                ControllerButtonMapper.flipFaceButtons(
                        KeyEvent.KEYCODE_BUTTON_Y));
    }

    @Test
    public void protocolCapabilityMapIncludesExtendedButtons() {
        assertEquals(
                Integer.valueOf(ControllerPacket.MISC_FLAG),
                ControllerButtonMapper
                        .getProtocolButtonMappings()
                        .get(KeyEvent.KEYCODE_MEDIA_RECORD));
        assertEquals(
                Integer.valueOf(ControllerPacket.TOUCHPAD_FLAG),
                ControllerButtonMapper
                        .getProtocolButtonMappings()
                        .get(KeyEvent.KEYCODE_BUTTON_1));
    }

    private static ControllerButtonMapper.Builder mapperBuilder(
            int vendorId,
            int productId) {
        return ControllerButtonMapper.builder(
                vendorId,
                productId,
                34);
    }

    private static int remap(
            ControllerButtonMapper mapper,
            int keyCode,
            int scanCode,
            boolean joyConFixEnabled) {
        return mapper.remap(
                keyCode,
                scanCode,
                0,
                true,
                joyConFixEnabled);
    }
}
