package com.limelight.settings.ui;

import com.limelight.settings.SettingKey;

import java.util.Set;

/**
 * Canonical and one-time legacy keys for stream-menu card layout.
 */
public final class GameMenuCardSettingKeys {
    public static final SettingKey<String> ORDER_DOCUMENT =
            SettingKey.boundedStringKey(
                    "stream.ui.game_menu.card_order",
                    "",
                    GameMenuCardLayoutCodec
                            .MAXIMUM_DOCUMENT_LENGTH)
                    .renamedFrom("game_menu_card_order_v2");
    public static final SettingKey<Set<String>> HIDDEN_CARD_IDS =
            SettingKey.boundedStringCollectionKey(
                    "stream.ui.game_menu.hidden_cards",
                    GameMenuCardLayout.MAXIMUM_CARD_COUNT,
                    GameMenuCardLayout.MAXIMUM_CARD_ID_LENGTH)
                    .renamedFrom("game_menu_card_hidden_v2");

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
