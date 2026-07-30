# ADR 0002: Preserve Java and the Existing View Stack During Core Refactoring

- Status: Accepted
- Date: 2026-07-31

## Context

The application is predominantly Java and Android Views. It contains low-latency
Surface, input, GL, audio, JNI, and hardware-controller paths. A simultaneous
language, UI toolkit, dependency-injection, and architecture migration would
make regressions difficult to attribute.

## Decision

- Keep Java and the current View-based UI while core ownership and dependency
  boundaries are corrected.
- Apply immutable UI state and one-way event flow where useful without requiring
  a Compose rewrite.
- Use constructor injection and small factories before considering a DI
  framework.
- Keep Surface, codec, audio, and session resources out of ViewModel objects.

Language or toolkit migration requires a separate ADR after the architecture
refactor, with a demonstrated product or maintenance benefit.

## Consequences

- Structural commits remain behavior-focused and reviewable.
- Existing device and rendering behavior stays comparable throughout migration.
- Modern architectural boundaries do not depend on adopting a particular UI
  toolkit.

