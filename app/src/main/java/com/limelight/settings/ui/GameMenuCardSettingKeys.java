package com.limelight.settings.ui;

import com.limelight.settings.SettingKey;

import java.util.Set;

/**
 * Canonical and one-time legacy keys for stream-menu card layout.
 */
public final class GameMenuCardSettingKeys {
    public static final SettingKey<String> ORDER_DOCUMENT =
            SettingKey.boundedStringKey(
                    "game_menu_card_order_v2",
                    "",
                    GameMenuCardLayoutCodec
                            .MAXIMUM_DOCUMENT_LENGTH);
    public static final SettingKey<Set<String>> HIDDEN_CARD_IDS =
            SettingKey.boundedStringCollectionKey(
                    "game_menu_card_hidden_v2",
                    GameMenuCardLayout.MAXIMUM_CARD_COUNT,
                    GameMenuCardLayout.MAXIMUM_CARD_ID_LENGTH);

    public static final SettingKey<String>
            LEGACY_ACTION_ORDER =
            SettingKey.boundedStringKey(
                    "game_menu_action_order_v1",
                    "",
                    GameMenuCardLayoutCodec
                            .MAXIMUM_DOCUMENT_LENGTH);
    public static final SettingKey<Set<String>>
            LEGACY_HIDDEN_ACTION_IDS =
            SettingKey.boundedStringCollectionKey(
                    "game_menu_action_hidden_v1",
                    GameMenuCardLayout.MAXIMUM_CARD_COUNT,
                    GameMenuCardLayout.MAXIMUM_CARD_ID_LENGTH);

    private GameMenuCardSettingKeys() {
    }
}
