package com.limelight.preferences;

import com.limelight.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Central presentation taxonomy for the settings experience.
 *
 * <p>The typed settings schema owns persistence and behavior. This catalog
 * owns only information architecture: which related controls belong in the
 * same visual group and how top-level sections are clustered.</p>
 */
final class SettingsPresentationCatalog {
    static final class Group {
        private final String id;
        private final int titleRes;
        private final int order;

        private Group(String id, int titleRes, int order) {
            this.id = Objects.requireNonNull(id, "id");
            this.titleRes = titleRes;
            this.order = order;
        }

        String getId() {
            return id;
        }

        int getTitleRes() {
            return titleRes;
        }

        int getOrder() {
            return order;
        }
    }

    private static final Group GENERAL =
            new Group("general", R.string.settings_group_general, 900);
    private static final Map<String, Group> ITEM_GROUPS = new HashMap<>();
    private static final Map<String, Group> FEATURED_GROUPS = new HashMap<>();
    private static final Map<String, Group> SECTION_GROUPS = new HashMap<>();
    private static final Map<String, Integer> SECTION_SUMMARIES = new HashMap<>();

    static {
        group("section_video_display", "stream_format",
                R.string.settings_group_stream_format, 10,
                "stream.video.resolution",
                "stream.video.aspect_ratio",
                "stream.video.frame_rate");
        group("section_video_display", "bitrate",
                R.string.settings_group_bitrate, 20,
                "stream.video.bitrate_kbps",
                "editor_video_bitrate_mbps");
        group("section_video_display", "decoder",
                R.string.settings_group_decoder_latency, 30,
                "stream.video.codec",
                "stream.video.frame_pacing",
                "stream.video.low_latency_decode",
                "stream.video.unlock_frame_rates",
                "stream.video.reduce_refresh_rate");
        group("section_video_display", "color",
                R.string.settings_group_hdr_color, 40,
                "stream.video.hdr.enabled",
                "stream.video.full_range");
        group("section_video_display", "display",
                R.string.settings_group_display_layout, 50,
                "stream.display.portrait",
                "stream.display.stretch_video",
                "stream.display.use_cutout_area",
                "stream.display.automatic_orientation",
                "stream.display.gravity",
                "stream.display.external_display");

        group("section_audio", "audio_output",
                R.string.settings_group_audio_output, 10,
                "stream.audio.channel_layout",
                "stream.audio.effects",
                "stream.audio.play_on_host",
                "stream.audio.muted");

        group("section_touch_mouse", "input_mode",
                R.string.settings_group_input_mode, 10,
                "input.pointer.mode",
                "input.touch.keyboard_gesture_finger_count",
                "input.pointer.local_system_cursor",
                "input.pointer.navigation_buttons",
                "input.pointer.absolute_mouse");
        group("section_touch_mouse", "press_click",
                R.string.settings_group_press_click, 20,
                "input.touch.force_press.enabled",
                "input.touch.force_press.threshold_milli_hpa",
                "input.touch.force_press.minimum_duration_ms",
                "input.touchpad.long_press_duration_ms");
        group("section_touch_mouse", "touchpad_pointer",
                R.string.settings_group_touchpad_pointer, 30,
                "input.touchpad.pointer_sensitivity_x",
                "input.touchpad.pointer_sensitivity_y");
        group("section_touch_mouse", "virtual_touchpad",
                R.string.settings_group_virtual_touchpad, 35,
                "input.virtual_touchpad.sensitivity_x",
                "input.virtual_touchpad.sensitivity_y");
        group("section_touch_mouse", "external_touchpad",
                R.string.settings_group_external_touchpad, 40,
                "input.external_touchpad.sensitivity_x",
                "input.external_touchpad.sensitivity_y",
                "input.external_touchpad.scroll_amount");
        group("section_touch_mouse", "input_advanced",
                R.string.settings_group_input_advanced, 50,
                "input.mouse.wheel_scroll_amount",
                "input.transport.disable_adaptive_throttling");

        group("section_gamepad", "controller_response",
                R.string.settings_group_controller_response, 10,
                "input.controller.stick_deadzone_percent",
                "input.controller.trigger_deadzone_disabled",
                "input.controller.flip_face_buttons",
                "input.controller.touchpad_as_mouse");
        group("section_gamepad", "controller_devices",
                R.string.settings_group_controller_devices, 20,
                "input.controller.multiple_controllers",
                "input.controller.usb_driver.enabled",
                "input.controller.usb_driver.claim_all_devices",
                "input.controller.joycon_compatibility",
                "input.controller.battery_reporting");
        group("section_gamepad", "controller_mouse",
                R.string.settings_group_controller_mouse, 30,
                "input.controller.mouse_emulation.enabled",
                "input.controller.mouse_emulation.button",
                "input.controller.mouse_emulation.scrolling_stick",
                "input.controller.mouse_emulation.sensitivity_percent");
        group("section_gamepad", "controller_motion",
                R.string.settings_group_controller_motion, 40,
                "input.controller.motion.enabled",
                "input.controller.motion.fallback_to_device",
                "input.controller.motion.force_gyro",
                "input.controller.motion.force_gyro_requires_left_trigger",
                "input.controller.motion.force_gyro_swap_axes",
                "input.controller.motion.force_gyro_sensitivity_percent",
                "input.virtual_gamepad.device_motion");
        group("section_gamepad", "controller_rumble",
                R.string.settings_group_controller_rumble, 50,
                "input.controller.rumble.fallback_to_device",
                "input.controller.rumble.fallback_strength_percent",
                "input.controller.rumble.flip_motors",
                "input.controller.rumble.use_device",
                "input.controller.rumble.force_strong",
                "input.controller.rumble.stop_pulse");

        group("section_virtual_controls", "virtual_gamepad",
                R.string.settings_group_virtual_gamepad, 10,
                "input.virtual_gamepad.enabled",
                "input.virtual_gamepad.layout_id",
                "input.virtual_gamepad.haptics",
                "input.virtual_gamepad.opacity_percent",
                "input.virtual_gamepad.disable_stick_click",
                "action_virtual_gamepad_import",
                "action_virtual_gamepad_export");
        group("section_virtual_controls", "virtual_keyboard",
                R.string.settings_group_virtual_keyboard, 20,
                "input.virtual_keyboard.show_on_start",
                "input.virtual_keyboard.layout_id",
                "input.virtual_keyboard.opacity_percent",
                "input.virtual_keyboard.height_dp",
                "input.virtual_keyboard.combination_mode",
                "input.virtual_keyboard.haptics",
                "action_virtual_keyboard_import",
                "action_virtual_keyboard_export");

        group("section_clipboard_files", "clipboard_files",
                R.string.settings_group_clipboard_files, 10,
                "transfer.clipboard.enabled",
                "transfer.clipboard.download_directory_uri");

        group("section_stream_interface", "stream_behavior",
                R.string.settings_group_stream_behavior, 10,
                "stream.host.optimize_game_settings",
                "stream.ui.picture_in_picture",
                "stream.display.device_screen_policy");
        group("section_stream_interface", "stream_menu",
                R.string.settings_group_stream_menu, 20,
                "input.controller.mouse_emulation.opens_game_menu",
                "stream.ui.floating_control.enabled",
                "stream.ui.floating_control.action",
                "stream.ui.floating_control.remember_position",
                "stream.ui.shortcuts.hide_built_in");
        group("section_stream_interface", "performance_overlay",
                R.string.settings_group_performance_overlay, 30,
                "stream.ui.performance_overlay.enabled",
                "stream.ui.performance_overlay.compact",
                "stream.ui.performance_overlay.interactive",
                "stream.ui.performance_overlay.details",
                "stream.ui.performance_overlay.scale_percent",
                "stream.ui.performance_overlay.margin_top_dp");
        group("section_stream_interface", "stream_notifications",
                R.string.settings_group_stream_notifications, 40,
                "stream.ui.rumble_overlay.enabled",
                "stream.ui.connection_warnings_disabled",
                "stream.ui.latency_toast");

        group("section_app_appearance", "appearance",
                R.string.settings_group_appearance, 10,
                "app.appearance.theme_mode",
                "app.language");
        group("section_system_accessibility", "accessibility",
                R.string.settings_group_accessibility, 10,
                "stream.ui.game_mode_integration_disabled",
                "action_accessibility_config_import",
                "input.accessibility.key_logging");

        group("section_backup_restore", "configuration_archive",
                R.string.settings_group_configuration_archive, 10,
                "action_configuration_export",
                "action_configuration_import");
        group("section_about", "project",
                R.string.settings_group_project, 10,
                "action_about_app",
                "action_about_help");

        featured("featured_stream", R.string.settings_featured_stream, 10,
                "stream.video.resolution",
                "stream.video.frame_rate",
                "stream.video.bitrate_kbps");
        featured("featured_input", R.string.settings_featured_input_sharing, 20,
                "input.pointer.mode",
                "transfer.clipboard.enabled");

        section("section_stream", R.string.settings_category_stream, 10,
                entry("section_video_display", R.string.settings_section_summary_video),
                entry("section_audio", R.string.settings_section_summary_audio));
        section("section_input", R.string.settings_category_input, 20,
                entry("section_touch_mouse", R.string.settings_section_summary_touch),
                entry("section_gamepad", R.string.settings_section_summary_gamepad),
                entry("section_virtual_controls", R.string.settings_section_summary_virtual_controls));
        section("section_tools", R.string.settings_category_tools, 30,
                entry("section_clipboard_files", R.string.settings_section_summary_clipboard),
                entry("section_stream_interface", R.string.settings_section_summary_stream_interface));
        section("section_app_system", R.string.settings_category_app_system, 40,
                entry("section_app_appearance", R.string.settings_section_summary_appearance),
                entry("section_system_accessibility", R.string.settings_section_summary_accessibility),
                entry("section_backup_restore", R.string.settings_section_summary_backup),
                entry("section_about", R.string.settings_section_summary_about));
    }

    private SettingsPresentationCatalog() {
    }

    static Group forItem(String sectionId, String itemId) {
        Group group = ITEM_GROUPS.get(key(sectionId, itemId));
        return group == null ? GENERAL : group;
    }

    static Group forFeaturedItem(String itemId) {
        Group group = FEATURED_GROUPS.get(itemId);
        return group == null ? GENERAL : group;
    }

    static Group forSection(String sectionId) {
        Group group = SECTION_GROUPS.get(sectionId);
        return group == null ? GENERAL : group;
    }

    static int summaryForSection(String sectionId) {
        Integer summaryRes = SECTION_SUMMARIES.get(sectionId);
        return summaryRes == null ? 0 : summaryRes;
    }

    private static void group(
            String sectionId,
            String groupId,
            int titleRes,
            int order,
            String... itemIds) {
        Group group = new Group(groupId, titleRes, order);
        for (String itemId : itemIds) {
            putUnique(ITEM_GROUPS, key(sectionId, itemId), group);
        }
    }

    private static void featured(
            String groupId,
            int titleRes,
            int order,
            String... itemIds) {
        Group group = new Group(groupId, titleRes, order);
        for (String itemId : itemIds) {
            putUnique(FEATURED_GROUPS, itemId, group);
        }
    }

    private static void section(
            String groupId,
            int titleRes,
            int order,
            SectionEntry... entries) {
        Group group = new Group(groupId, titleRes, order);
        for (SectionEntry entry : entries) {
            putUnique(SECTION_GROUPS, entry.sectionId, group);
            if (SECTION_SUMMARIES.put(entry.sectionId, entry.summaryRes) != null) {
                throw new IllegalStateException(
                        "Duplicate settings section summary: " + entry.sectionId);
            }
        }
    }

    private static SectionEntry entry(String sectionId, int summaryRes) {
        return new SectionEntry(sectionId, summaryRes);
    }

    private static final class SectionEntry {
        private final String sectionId;
        private final int summaryRes;

        private SectionEntry(String sectionId, int summaryRes) {
            this.sectionId = Objects.requireNonNull(sectionId, "sectionId");
            this.summaryRes = summaryRes;
        }
    }

    private static void putUnique(
            Map<String, Group> target,
            String key,
            Group group) {
        if (target.put(key, group) != null) {
            throw new IllegalStateException(
                    "Duplicate settings presentation mapping: " + key);
        }
    }

    private static String key(String sectionId, String itemId) {
        return sectionId + '\n' + itemId;
    }
}
