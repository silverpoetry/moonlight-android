package com.limelight.settings.ui;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * Typed settings-backed implementation of the card-layout storage port.
 */
public final class SettingsGameMenuCardLayoutRepository
        implements GameMenuCardLayoutRepository {
    private final SettingsRepository settings;

    public SettingsGameMenuCardLayoutRepository(
            SettingsRepository settings) {
        this.settings = Objects.requireNonNull(
                settings,
                "settings");
    }

    @Override
    public GameMenuCardLayoutLoadResult load() {
        if (!settings.contains(
                GameMenuCardSettingKeys.ORDER_DOCUMENT)) {
            return GameMenuCardLayoutLoadResult.absent();
        }
        return GameMenuCardLayoutLoadResult.present(
                new GameMenuCardLayout(
                        GameMenuCardLayoutCodec.decodeOrder(
                                settings.get(
                                        GameMenuCardSettingKeys
                                                .ORDER_DOCUMENT)),
                        settings.get(
                                GameMenuCardSettingKeys
                                        .HIDDEN_CARD_IDS)));
    }

    @Override
    public void save(GameMenuCardLayout layout) {
        Objects.requireNonNull(layout, "layout");
        settings.edit()
                .put(
                        GameMenuCardSettingKeys.ORDER_DOCUMENT,
                        GameMenuCardLayoutCodec.encodeOrder(
                                layout.getOrderedCardIds()))
                .put(
                        GameMenuCardSettingKeys.HIDDEN_CARD_IDS,
                        layout.getHiddenCardIds())
                .apply();
    }
}
