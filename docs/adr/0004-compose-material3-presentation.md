# ADR 0004: Compose and Material 3 for the Android Presentation Layer

- Status: Accepted
- Date: 2026-08-03
- Supersedes: ADR 0002

## Context

The package-first refactor established typed settings, domain modules, explicit
controllers, and one-way presentation state. The remaining Android View layer
duplicated layout behavior across portrait, landscape, and large-screen XML,
and the fork-specific purple theme made new screens depend on ad-hoc drawables
and styles. The project now supports Android 6.0 and later, so current Compose
and Material 3 releases can be adopted without a compatibility dependency fork.

## Decision

- Use Jetpack Compose for non-realtime application presentation and standard
  Material 3 components, motion, typography, colors, and adaptive layouts.
- Keep `SurfaceView`, codec, input, audio, JNI, and stream-session ownership out
  of Compose. The stream control surface is an overlay on the existing realtime
  pipeline, not a rewrite of it.
- Keep Activities as thin lifecycle and navigation composition roots while
  existing Java controllers are migrated incrementally.
- Render immutable feature state and emit semantic user events. Compose code
  does not read preferences or perform protocol and file operations directly.
- Delete each replaced XML renderer and its styling resources in the same
  migration slice. Do not retain two production implementations.
- Use one application theme with light, dark, and Android 12+ dynamic color.
  Product behavior and persistent settings remain independent of the toolkit.

## Consequences

- The host, application, settings, transfer, diagnostics, about, and stream
  control surfaces share one design system and adaptive layout model.
- UI tests target semantic actions and state instead of View hierarchy details.
- Kotlin is permitted in the presentation layer; existing stable Java domain
  and realtime code is not converted without an independent reason.
- Android API 23 is the minimum supported platform.
