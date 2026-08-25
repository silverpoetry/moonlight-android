package com.limelight.integration.xiaomi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public final class XiaomiRefreshRateOverrideControllerTest {
    @Test
    public void enabledPolicyRemovesOnlyMoonlightFromRefreshLists() {
        XiaomiRefreshRateOverrideController.PowerKeeperState state =
                XiaomiRefreshRateOverrideController.PowerKeeperState.parse(
                        "fpsconfig mPrivGames={" +
                                "com.silverpoetry.moonlight=144, " +
                                "com.tencent.jkchess=120}\n" +
                                "fpsconfig mDefaultGameApp(60):{" +
                                "com.silverpoetry.moonlight, " +
                                "com.miui.weather2}");
        assertNotNull(state);
        assertEquals(
                "am broadcast --user current --receiver-foreground" +
                        " -p com.miui.powerkeeper" +
                        " -a com.xiaomi.joyose.HIGH_FPS_LIST" +
                        " --esal high_fps_list com.tencent.jkchess:120" +
                        " && am broadcast --user current --receiver-foreground" +
                        " -p com.miui.powerkeeper" +
                        " -a intent.action.TOP_GAME_LIST" +
                        " --ez isAppend false" +
                        " --es gameList com.miui.weather2",
                XiaomiRefreshRateOverrideController
                        .buildPolicyUpdateCommand(
                        "com.silverpoetry.moonlight",
                        state,
                        true));
        assertFalse(state.defaultGames.isEmpty());
    }

    @Test
    public void disabledPolicyRestoresMoonlightToDefaultGameList() {
        XiaomiRefreshRateOverrideController.PowerKeeperState state =
                XiaomiRefreshRateOverrideController.PowerKeeperState.parse(
                        "fpsconfig mPrivGames={com.tencent.jkchess=120}\n" +
                                "fpsconfig mDefaultGameApp(60):{" +
                                "com.miui.weather2}");
        assertNotNull(state);
        assertEquals(
                "am broadcast --user current --receiver-foreground" +
                        " -p com.miui.powerkeeper" +
                        " -a com.xiaomi.joyose.HIGH_FPS_LIST" +
                        " --esal high_fps_list com.tencent.jkchess:120" +
                        " && am broadcast --user current --receiver-foreground" +
                        " -p com.miui.powerkeeper" +
                        " -a intent.action.TOP_GAME_LIST" +
                        " --ez isAppend false" +
                        " --es gameList " +
                        "com.miui.weather2,com.silverpoetry.moonlight",
                XiaomiRefreshRateOverrideController
                        .buildPolicyUpdateCommand(
                                "com.silverpoetry.moonlight",
                                state,
                                false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void commandRejectsShellMetacharacters() {
        XiaomiRefreshRateOverrideController.buildPolicyUpdateCommand(
                "com.silverpoetry.moonlight;id",
                new XiaomiRefreshRateOverrideController.PowerKeeperState(
                        new java.util.LinkedHashMap<>(),
                        new java.util.LinkedHashSet<>()),
                true);
    }
}
