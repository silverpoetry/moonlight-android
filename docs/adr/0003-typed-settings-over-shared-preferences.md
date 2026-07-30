# ADR 0003: Typed Settings Over SharedPreferences Persistence

- Status: Accepted
- Date: 2026-07-31

## Context

The application stores a large established settings schema in Android default
`SharedPreferences`. Runtime code, preference widgets, and Axixi-derived
features currently read those values directly. Keys, types, defaults, parsing,
validation, device policy, and legacy migration are mixed in
`PreferenceConfiguration` and several UI/runtime classes.

Replacing the persistence technology at the same time as correcting those
boundaries would not remove the underlying coupling. It would also increase the
risk to existing user settings without providing a product benefit.

## Decision

- Retain `SharedPreferences` as the Android persistence mechanism during this
  refactor.
- Put every persisted value behind a typed `SettingKey` with one canonical
  storage type, default, and validator.
- Access storage through `SettingsRepository`; only its Android adapter depends
  on `SharedPreferences`.
- Give runtime domains immutable settings snapshots created during composition.
  Realtime paths do not reread storage.
- Perform legacy migration through versioned, idempotent repository migrations.
- Move preference UI to typed intents and repository writes after runtime
  readers have migrated.
- Preserve an unknown stored value only when existing behavior explicitly gives
  it compatibility semantics; otherwise resolve invalid data to the canonical
  safe default.

## Consequences

- Storage and UI technology can change later without changing runtime domain
  contracts.
- Existing settings and application upgrade compatibility are preserved.
- The temporary adapter from `PreferenceConfiguration` is allowed only inside
  the typed-settings phase and must be deleted before that phase closes.
- A direct `SharedPreferences` read outside platform persistence, widget state,
  or a documented migration is architectural debt and is removed domain by
  domain.
