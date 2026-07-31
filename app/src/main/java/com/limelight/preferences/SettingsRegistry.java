package com.limelight.preferences;

import android.content.Context;
import android.content.res.XmlResourceParser;

import com.limelight.R;
import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsScreenKeyCatalog;
import com.limelight.settings.stream.StreamVideoSettingKeys;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

/** Android XML adapter that produces typed settings-screen metadata. */
final class SettingsRegistry {
    private static final String ANDROID_NS =
            "http://schemas.android.com/apk/res/android";
    private static final String SEEKBAR_NS =
            "http://schemas.moonlight-stream.com/apk/res/seekbar";

    private SettingsRegistry() {
    }

    static ArrayList<SettingsSection> load(Context context) {
        ArrayList<SettingsSection> sections = new ArrayList<>();
        SettingsSection currentSection = null;
        XmlResourceParser parser = context.getResources()
                .getXml(R.xml.preferences);

        try {
            int event;
            while ((event = parser.next()) !=
                    XmlPullParser.END_DOCUMENT) {
                if (event != XmlPullParser.START_TAG) {
                    continue;
                }

                String tag = parser.getName();
                if ("PreferenceCategory".equals(tag)) {
                    String key = attrString(
                            context,
                            parser,
                            "key");
                    CharSequence title = attrText(
                            context,
                            parser,
                            "title");
                    if (isEmpty(key)) {
                        key = "category_" + sections.size();
                    }
                    currentSection = new SettingsSection(
                            key,
                            title,
                            SettingsIconCatalog.forSection(key));
                    sections.add(currentSection);
                }
                else if (currentSection != null) {
                    SettingsItem item = parseItem(
                            context,
                            parser,
                            tag);
                    if (item != null && !isEmpty(item.key)) {
                        currentSection.items.add(item);
                    }
                }
            }
        }
        catch (Exception error) {
            throw new IllegalStateException(
                    "Unable to load settings",
                    error);
        }
        finally {
            parser.close();
        }
        return sections;
    }

    private static SettingsItem parseItem(
            Context context,
            XmlResourceParser parser,
            String tag) {
        SettingsItem item = new SettingsItem();
        item.key = attrString(context, parser, "key");
        item.title = attrText(context, parser, "title");
        item.summary = attrText(context, parser, "summary");
        item.dependency = attrString(
                context,
                parser,
                "dependency");
        item.url = parser.getAttributeValue(null, "url");
        item.iconRes = SettingsIconCatalog.forItem(item.key);

        if (tag.endsWith("SmallIconCheckboxPreference") ||
                tag.endsWith("CheckBoxPreference")) {
            item.type = SettingsItem.Type.SWITCH;
        }
        else if (tag.endsWith("LanguagePreference") ||
                tag.endsWith("ListPreference")) {
            item.type = SettingsItem.Type.LIST;
            int entriesId = parser.getAttributeResourceValue(
                    ANDROID_NS,
                    "entries",
                    0);
            int valuesId = parser.getAttributeResourceValue(
                    ANDROID_NS,
                    "entryValues",
                    0);
            if (entriesId != 0) {
                item.entries = context.getResources()
                        .getTextArray(entriesId);
            }
            if (valuesId != 0) {
                item.entryValues = context.getResources()
                        .getTextArray(valuesId);
            }
        }
        else if (tag.endsWith("SeekBarPreference")) {
            item.type = SettingsItem.Type.SLIDER;
            item.min = parser.getAttributeIntValue(
                    SEEKBAR_NS,
                    "min",
                    0);
            item.max = parser.getAttributeIntValue(
                    ANDROID_NS,
                    "max",
                    100);
            item.step = parser.getAttributeIntValue(
                    SEEKBAR_NS,
                    "step",
                    1);
            item.keyStep = parser.getAttributeIntValue(
                    SEEKBAR_NS,
                    "keyStep",
                    0);
            item.divisor = parser.getAttributeIntValue(
                    SEEKBAR_NS,
                    "divisor",
                    1);
            item.decimalPlaces = Math.max(
                    0,
                    Math.min(
                            4,
                            parser.getAttributeIntValue(
                                    SEEKBAR_NS,
                                    "decimals",
                                    1)));
            item.suffix = attrText(context, parser, "text");
            item.dialogMessage = attrText(
                    context,
                    parser,
                    "dialogMessage");
        }
        else if (tag.endsWith("EditTextPreference")) {
            item.type = SettingsItem.Type.TEXT;
            item.dialogMessage = attrText(
                    context,
                    parser,
                    "dialogMessage");
        }
        else if (tag.endsWith("WebLauncherPreference")) {
            item.type = SettingsItem.Type.WEB;
        }
        else if ("Preference".equals(tag)) {
            item.type = SettingsItem.Type.ACTION;
        }
        else {
            return null;
        }

        bindSchemaKey(item);
        return item;
    }

    private static void bindSchemaKey(SettingsItem item) {
        if (item.isCustomBitrateEditor()) {
            item.settingKey = StreamVideoSettingKeys.BITRATE_KBPS;
            return;
        }

        item.settingKey = SettingsScreenKeyCatalog.find(item.key);
        if (item.type == SettingsItem.Type.ACTION ||
                item.type == SettingsItem.Type.WEB) {
            return;
        }
        if (item.settingKey == null) {
            throw new IllegalStateException(
                    "Persisted settings item has no typed schema: " +
                            item.key);
        }

        SettingKey.StorageType expected;
        switch (item.type) {
            case SWITCH:
                expected = SettingKey.StorageType.BOOLEAN;
                break;
            case LIST:
            case TEXT:
                expected = SettingKey.StorageType.STRING;
                break;
            case SLIDER:
                expected = SettingKey.StorageType.INTEGER;
                break;
            default:
                throw new AssertionError(
                        "Unhandled persisted item type: " +
                                item.type);
        }
        if (item.settingKey.getStorageType() != expected) {
            throw new IllegalStateException(
                    "Settings item " + item.key +
                            " uses " +
                            item.settingKey.getStorageType() +
                            " storage but XML requires " +
                            expected);
        }
    }

    private static CharSequence attrText(
            Context context,
            XmlResourceParser parser,
            String name) {
        int resourceId = parser.getAttributeResourceValue(
                ANDROID_NS,
                name,
                0);
        if (resourceId != 0) {
            return context.getText(resourceId);
        }
        return parser.getAttributeValue(ANDROID_NS, name);
    }

    private static String attrString(
            Context context,
            XmlResourceParser parser,
            String name) {
        CharSequence text = attrText(context, parser, name);
        return text == null ? null : text.toString();
    }

    private static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }
}
