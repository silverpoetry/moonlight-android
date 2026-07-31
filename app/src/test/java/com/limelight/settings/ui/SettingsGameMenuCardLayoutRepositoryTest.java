package com.limelight.settings.ui;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SettingsGameMenuCardLayoutRepositoryTest {
    @Test
    public void absentDocumentMeansDefaultCatalogPolicy() {
        SettingsGameMenuCardLayoutRepository repository =
                new SettingsGameMenuCardLayoutRepository(
                        new FakeRepository());

        assertFalse(repository.load().hasLayout());
    }

    @Test
    public void savePersistsOneAtomicDocumentAndLoadsIt() {
        FakeRepository settings = new FakeRepository();
        SettingsGameMenuCardLayoutRepository repository =
                new SettingsGameMenuCardLayoutRepository(settings);
        GameMenuCardLayout layout =
                new GameMenuCardLayout(
                        Arrays.asList(
                                "action:disconnect",
                                "shortcut:custom:one"),
                        Collections.singleton(
                                "shortcut:custom:one"));

        repository.save(layout);
        GameMenuCardLayoutLoadResult loaded =
                repository.load();

        assertTrue(loaded.hasLayout());
        assertEquals(
                layout.getOrderedCardIds(),
                loaded.getLayout().getOrderedCardIds());
        assertEquals(
                layout.getHiddenCardIds(),
                loaded.getLayout().getHiddenCardIds());
        assertEquals(2, settings.values.size());
        assertEquals(1, settings.applyCount);
    }

    @Test
    public void corruptOrderCannotEscapeTypedBoundary() {
        FakeRepository settings = new FakeRepository();
        settings.values.put(
                GameMenuCardSettingKeys.ORDER_DOCUMENT.getName(),
                "{bad");
        settings.values.put(
                GameMenuCardSettingKeys.HIDDEN_CARD_IDS.getName(),
                Collections.singleton("action:disconnect"));
        SettingsGameMenuCardLayoutRepository repository =
                new SettingsGameMenuCardLayoutRepository(settings);

        GameMenuCardLayout loaded =
                repository.load().getLayout();

        assertTrue(loaded.getOrderedCardIds().isEmpty());
        assertEquals(
                Collections.singleton("action:disconnect"),
                loaded.getHiddenCardIds());
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values =
                new HashMap<>();
        private int applyCount;

        @Override
        public boolean contains(SettingKey<?> key) {
            return values.containsKey(key.getName());
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            return key.normalizeStoredValue(
                    values.get(key.getName()));
        }

        @Override
        public Editor edit() {
            return new Editor() {
                private final Map<String, Object> staged =
                        new HashMap<>();

                @Override
                public <T> Editor put(
                        SettingKey<T> key,
                        T value) {
                    staged.put(
                            key.getName(),
                            key.normalizeValue(value));
                    return this;
                }

                @Override
                public Editor remove(SettingKey<?> key) {
                    staged.put(key.getName(), null);
                    return this;
                }

                @Override
                public void apply() {
                    commitChanges();
                }

                @Override
                public boolean commit() {
                    commitChanges();
                    return true;
                }

                private void commitChanges() {
                    for (Map.Entry<String, Object> entry :
                            staged.entrySet()) {
                        if (entry.getValue() == null) {
                            values.remove(entry.getKey());
                        }
                        else {
                            values.put(
                                    entry.getKey(),
                                    entry.getValue());
                        }
                    }
                    applyCount++;
                }
            };
        }
    }
}
