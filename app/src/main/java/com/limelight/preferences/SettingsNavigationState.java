package com.limelight.preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stable navigation and scroll state for the settings surface.
 *
 * <p>Pages are identified by section ID rather than their mutable list index.
 * Each page retains its own content position, while the wide-layout section
 * rail has an independent position.</p>
 */
final class SettingsNavigationState {
    private static final String ROOT_PAGE_ID = "";

    private final Map<String, SettingsScrollPosition> contentScrollByPage =
            new HashMap<>();
    private String selectedSectionId;
    private SettingsScrollPosition sectionRailScroll =
            SettingsScrollPosition.START;

    String getSelectedSectionId() {
        return selectedSectionId;
    }

    boolean hasSelectedSection() {
        return selectedSectionId != null;
    }

    void selectSection(String sectionId) {
        selectedSectionId = Objects.requireNonNull(
                sectionId,
                "sectionId");
    }

    void selectFeatured() {
        selectedSectionId = null;
    }

    boolean returnToRoot() {
        if (selectedSectionId == null) {
            return false;
        }
        selectedSectionId = null;
        return true;
    }

    void captureContentScroll(SettingsScrollPosition position) {
        if (position != null) {
            setContentScroll(selectedSectionId, position);
        }
    }

    SettingsScrollPosition getContentScrollPosition() {
        return getContentScroll(selectedSectionId);
    }

    SettingsScrollPosition getContentScroll(String sectionId) {
        SettingsScrollPosition position = contentScrollByPage.get(
                pageId(sectionId));
        return position == null
                ? SettingsScrollPosition.START
                : position;
    }

    void setContentScroll(
            String sectionId,
            SettingsScrollPosition position) {
        contentScrollByPage.put(
                pageId(sectionId),
                Objects.requireNonNull(position, "position"));
    }

    void captureSectionRailScroll(SettingsScrollPosition position) {
        if (position != null) {
            sectionRailScroll = position;
        }
    }

    SettingsScrollPosition getSectionRailScrollPosition() {
        return sectionRailScroll;
    }

    void setSectionRailScroll(SettingsScrollPosition position) {
        sectionRailScroll = Objects.requireNonNull(
                position,
                "position");
    }

    private static String pageId(String sectionId) {
        return sectionId == null ? ROOT_PAGE_ID : sectionId;
    }
}
