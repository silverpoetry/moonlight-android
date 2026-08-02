package com.limelight.computers.apps.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.limelight.computers.apps.HiddenAppRepository;
import com.limelight.computers.apps.HiddenAppSelection;
import com.limelight.computers.model.HostId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Android adapter for the historical host-keyed hidden-app store. */
public final class SharedPreferencesHiddenAppRepository
        implements HiddenAppRepository {
    static final String PREFERENCES_NAME = "HiddenApps";

    private final SharedPreferences preferences;

    public SharedPreferencesHiddenAppRepository(Context context) {
        Objects.requireNonNull(context, "context");
        preferences = context.getApplicationContext()
                .getSharedPreferences(
                        PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public HiddenAppSelection load(HostId hostId) {
        Objects.requireNonNull(hostId, "hostId");
        String canonicalKey = hostId.getValue();
        if (preferences.contains(canonicalKey)) {
            HiddenAppSelection selection = parseStoredIds(
                    readStringSet(canonicalKey));
            removeAliases(hostId, canonicalKey);
            return selection;
        }

        List<String> aliases = findAliases(hostId, canonicalKey);
        if (aliases.isEmpty()) {
            return HiddenAppSelection.empty();
        }

        TreeSet<Integer> merged = new TreeSet<>();
        for (String alias : aliases) {
            for (Integer appId : parseStoredIds(
                    readStringSet(alias)).getAppIds()) {
                if (merged.size() >=
                        HiddenAppSelection.MAXIMUM_APP_IDS) {
                    break;
                }
                merged.add(appId);
            }
        }
        HiddenAppSelection selection = HiddenAppSelection.of(merged);
        if (commitCanonical(canonicalKey, selection)) {
            removeKeys(aliases);
        }
        return selection;
    }

    @Override
    public void save(HostId hostId, HiddenAppSelection selection) {
        Objects.requireNonNull(hostId, "hostId");
        Objects.requireNonNull(selection, "selection");
        String canonicalKey = hostId.getValue();
        writeCanonicalAsync(canonicalKey, selection);
        removeAliases(hostId, canonicalKey);
    }

    @Override
    public void delete(HostId hostId) {
        Objects.requireNonNull(hostId, "hostId");
        List<String> keys = findAliases(hostId, null);
        keys.add(hostId.getValue());
        removeKeys(keys);
    }

    private boolean commitCanonical(
            String key,
            HiddenAppSelection selection) {
        return preferences.edit()
                .putStringSet(key, encode(selection))
                .commit();
    }

    private void writeCanonicalAsync(
            String key,
            HiddenAppSelection selection) {
        preferences.edit()
                .putStringSet(key, encode(selection))
                .apply();
    }

    private Set<String> encode(HiddenAppSelection selection) {
        Set<String> storedIds = new HashSet<>();
        for (Integer appId : selection.getAppIds()) {
            storedIds.add(Integer.toString(appId));
        }
        return storedIds;
    }

    private HiddenAppSelection parseStoredIds(Set<String> storedIds) {
        if (storedIds == null || storedIds.isEmpty()) {
            return HiddenAppSelection.empty();
        }
        TreeSet<Integer> parsed = new TreeSet<>();
        for (String storedId : storedIds) {
            if (parsed.size() >= HiddenAppSelection.MAXIMUM_APP_IDS) {
                break;
            }
            try {
                int appId = Integer.parseInt(storedId);
                parsed.add(appId);
            }
            catch (NumberFormatException ignored) {
                // Corrupt entries are isolated without invalidating the host.
            }
        }
        return HiddenAppSelection.of(parsed);
    }

    private Set<String> readStringSet(String key) {
        try {
            Set<String> stored = preferences.getStringSet(key, null);
            return stored == null
                    ? Collections.emptySet()
                    : new HashSet<>(stored);
        }
        catch (ClassCastException error) {
            return Collections.emptySet();
        }
    }

    private List<String> findAliases(
            HostId hostId,
            String excludedKey) {
        Map<String, ?> allValues = preferences.getAll();
        List<String> aliases = new ArrayList<>();
        for (String key : allValues.keySet()) {
            if (key.equals(excludedKey)) {
                continue;
            }
            try {
                if (hostId.equals(HostId.of(key))) {
                    aliases.add(key);
                }
            }
            catch (IllegalArgumentException ignored) {
                // Ignore unrelated or malformed historical keys.
            }
        }
        Collections.sort(aliases);
        return aliases;
    }

    private void removeAliases(HostId hostId, String canonicalKey) {
        removeKeys(findAliases(hostId, canonicalKey));
    }

    private void removeKeys(Collection<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : keys) {
            editor.remove(key);
        }
        editor.apply();
    }
}
