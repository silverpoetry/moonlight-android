# Settings Architecture

## Ownership

`SettingsRepository` is the persistence boundary. A `SettingKey<T>` owns the
stored name, storage type, canonical default, and validation for one value. The
Android adapter maps those keys to default `SharedPreferences`.

Runtime components never receive a repository or `SharedPreferences` merely to
query policy at arbitrary times. The app composition root reads once and builds
immutable domain snapshots:

```text
SharedPreferences
    -> SharedPreferencesSettingsRepository
        -> typed keys and versioned migrations
            -> immutable domain settings snapshots
                -> runtime controllers
```

Settings UI writes typed keys or domain update intents through the repository.
It does not duplicate parsing or defaults.

## Invalid and legacy data

- A missing key resolves to its canonical default.
- A stored value of the wrong primitive type resolves to the canonical default
  and never throws into Activity creation.
- Numeric constraints are applied by the key schema on reads and writes.
- String-backed enumerations parse into explicit domain enums.
- Legacy transformations are versioned, ordered, idempotent, and committed as
  one batch before a snapshot is published.
- Downgrade/forward-compatibility behavior for an unknown enum string is
  explicit in the domain codec; it is never an accidental `else` branch.

The resolution aggregate is the first migrated compound setting. Resolution,
selection mode, aspect policy, and FPS are decoded together by
`StreamResolutionCodec`; `StreamResolutionSettingsLoader` replaces the former
scattered parsing and commits all repairs in one editor transaction. Valid
custom dimensions retain their mode when only textual canonicalization is
required, while named legacy presets and malformed values follow explicit,
tested migration rules.

`SettingsMigrationRunner` owns the ordered schema version. Version 1 removes
the old 5.1-audio, never-drop-frames, and image-only clipboard switches in one
transaction while preserving their user-visible choices. It also detects
legacy values reintroduced by a downgrade without ever reducing a newer stored
schema version. Version 2 repairs the invalid historical on-screen gamepad
layout default to the first valid layout identifier.

## Snapshot lifecycle

A stream session receives one snapshot during composition. A setting that
requires stream restart changes the next snapshot, not the active session.
Settings that are intentionally live-updateable use a typed controller method
and publish a replacement immutable value on the controller's owning execution
domain.

No input, audio, video, controller-report, or transfer packet callback may read
persistent settings.

## Migration ledger

| Domain | Typed snapshot | Runtime storage reads removed | Legacy adapter removed |
| --- | --- | --- | --- |
| Stream display/FSR/window | `StreamDisplaySettings` | `Game` no longer reads FSR target, sharpness, HDR mode, or gravity | Pending full stream settings migration |
| Stream video/decoder | `StreamDecoderSettings`; typed resolution aggregate | Decoder and performance-statistics paths no longer receive `PreferenceConfiguration`; resolution/FPS parsing and repair have one safe codec | Pending remaining stream settings migration |
| Input and gestures | `InputSettings` with one atomic `InputSettingsState` per stream | Pointer, touchscreen/touchpad, gesture, keyboard, and virtual-touchpad runtime paths no longer read storage or receive `PreferenceConfiguration`; explicitly live settings publish one replacement snapshot | Temporary legacy UI fields remain to be migrated |
| Physical controllers | `ControllerSettings` with one atomic `ControllerSettingsState` per stream | `ControllerHandler` no longer receives `PreferenceConfiguration` or rereads storage from controller, sensor, rumble, battery, or USB callbacks | USB service settings remain to be migrated |
| On-screen controls | `VirtualControlSettings` with one atomic `VirtualControlSettingsState` per stream | Active virtual gamepad, virtual-key, touchpad-button, and full-keyboard rendering/input paths consume typed snapshots; stream-menu writers emit immutable domain updates | Named layout persistence still requires a dedicated repository; unused legacy configuration-loader paths remain to be removed |
| Audio and microphone | Pending | Pending | Pending |
| Clipboard and transfer | Pending | Pending | Pending |
| General UI and host list | Pending | Pending | Pending |

The ledger is complete only when direct default-preference reads are confined to
the repository, legacy migrations, and platform preference widgets that have
not yet emitted a typed update intent.
