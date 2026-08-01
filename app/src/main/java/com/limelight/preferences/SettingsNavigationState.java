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

    private final Map<String, Integer> contentScrollByPage =
            new HashMap<>();
    private String selectedSectionId;
    private int sectionRailScrollY;

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

    void captureContentScroll(Integer scrollY) {
        if (scrollY != null) {
            setContentScroll(selectedSectionId, scrollY);
        }
    }

    int getContentScrollY() {
        return getContentScroll(selectedSectionId);
    }

    int getContentScroll(String sectionId) {
        Integer scrollY = contentScrollByPage.get(
                pageId(sectionId));
        return scrollY == null ? 0 : scrollY;
    }

    void setContentScroll(String sectionId, int scrollY) {
        contentScrollByPage.put(
                pageId(sectionId),
                nonNegative(scrollY));
    }

    void captureSectionRailScroll(Integer scrollY) {
        if (scrollY != null) {
            sectionRailScrollY = nonNegative(scrollY);
        }
    }

    int getSectionRailScrollY() {
        return sectionRailScrollY;
    }

    void setSectionRailScrollY(int scrollY) {
        sectionRailScrollY = nonNegative(scrollY);
    }

    private static String pageId(String sectionId) {
        return sectionId == null ? ROOT_PAGE_ID : sectionId;
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }
}
