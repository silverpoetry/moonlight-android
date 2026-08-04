package com.limelight.preferences;

/** Declares the interaction model for values whose storage type is a list. */
final class SettingsEditorCatalog {
    enum Kind {
        LIST,
        DISCRETE_SLIDER,
        SEGMENTED
    }

    private SettingsEditorCatalog() {
    }

    static Kind forItem(SettingsItem item) {
        switch (item.key) {
            case "stream.video.frame_rate":
                return Kind.DISCRETE_SLIDER;
            case "stream.video.codec":
            case "stream.audio.channel_layout":
            case "stream.display.device_screen_policy":
                return Kind.SEGMENTED;
            default:
                return isCompactChoiceSet(item)
                        ? Kind.SEGMENTED
                        : Kind.LIST;
        }
    }

    private static boolean isCompactChoiceSet(SettingsItem item) {
        if ((item.type != SettingsItem.Type.LIST &&
                item.type != SettingsItem.Type.INTEGER_LIST) ||
                item.entries.length < 2 ||
                item.entries.length > 3) {
            return false;
        }
        for (CharSequence entry : item.entries) {
            if (entry == null || entry.length() > 10) {
                return false;
            }
        }
        return true;
    }
}
