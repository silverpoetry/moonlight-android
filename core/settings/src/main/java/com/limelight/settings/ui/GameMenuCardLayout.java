package com.limelight.settings.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable persisted references for the first page of the stream menu.
 *
 * <p>Only stable card IDs are stored. Labels, icons, actions, and shortcut
 * payloads remain owned by their current catalogs.</p>
 */
public final class GameMenuCardLayout {
    public static final int MAXIMUM_CARD_COUNT = 512;
    public static final int MAXIMUM_CARD_ID_LENGTH = 256;

    private final List<String> orderedCardIds;
    private final Set<String> hiddenCardIds;

    public GameMenuCardLayout(
            List<String> orderedCardIds,
            Set<String> hiddenCardIds) {
        this.orderedCardIds = immutableOrderedIds(
                orderedCardIds);
        this.hiddenCardIds = immutableIdSet(hiddenCardIds);
    }

    public List<String> getOrderedCardIds() {
        return orderedCardIds;
    }

    public Set<String> getHiddenCardIds() {
        return hiddenCardIds;
    }

    private static List<String> immutableOrderedIds(
            List<String> source) {
        Objects.requireNonNull(source, "orderedCardIds");
        LinkedHashSet<String> unique =
                validatedIds(source, "orderedCardIds");
        return Collections.unmodifiableList(
                new ArrayList<>(unique));
    }

    private static Set<String> immutableIdSet(Set<String> source) {
        Objects.requireNonNull(source, "hiddenCardIds");
        return Collections.unmodifiableSet(
                validatedIds(source, "hiddenCardIds"));
    }

    private static LinkedHashSet<String> validatedIds(
            Iterable<String> source,
            String name) {
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        for (String id : source) {
            if (id == null ||
                    id.isEmpty() ||
                    id.length() > MAXIMUM_CARD_ID_LENGTH) {
                throw new IllegalArgumentException(
                        name + " contains an invalid card ID");
            }
            copy.add(id);
            if (copy.size() > MAXIMUM_CARD_COUNT) {
                throw new IllegalArgumentException(
                        name + " exceeds the card limit");
            }
        }
        return copy;
    }
}
