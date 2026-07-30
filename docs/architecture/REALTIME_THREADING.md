# Realtime Threading Contract

Moonlight is a realtime media and remote-input client. Generic Android UI
architecture rules are insufficient for its hot paths, so this contract defines
the required scheduling and ownership behavior.

## Execution domains

### Android main thread

Allowed:

- View rendering and lifecycle callbacks;
- Android input event capture;
- permission and system UI requests;
- creation or destruction of UI-scoped controllers.

Forbidden:

- disk or network I/O;
- waiting for stream, transfer, audio, or input work;
- directory enumeration and clipboard payload loading;
- per-frame diagnostic formatting in release builds.

Input captured on the main thread may synchronously execute bounded in-memory
arbitration and enqueue or call a non-blocking protocol port. It must not wait
for remote acknowledgement.

### Input hot path

The input path owns contact and gesture state on one serialized execution
domain. While Android currently delivers touchscreen events on the main thread,
the input domain is treated as logically separate so it can be moved without
changing behavior.

Requirements:

- preserve Android event order and historical samples;
- never intentionally sample or coalesce protocol-significant events;
- no blocking locks, futures, sleeps, or unbounded queues;
- no file, clipboard, discovery, or logging I/O;
- no new per-MOVE collections after warm-up;
- cancellation and focus loss release every owned remote state.

### Stream-session executor

Connection start, stop, capability setup, and terminal errors are serialized by
the session owner. Session callbacks carry a generation identifier or are
otherwise rejected after their owner is destroyed.

`stop()` must be safe before start, during start, after failure, and after a
previous stop.

### Transfer executor

Clipboard and file work uses a bounded executor independent from input and
stream control. Copying metadata does not enumerate or read file payloads.
Payload transfer starts only after an explicit remote paste, pull, or share
action.

### Audio callbacks

Capture and playback callbacks perform bounded buffer conversion only. They do
not allocate unbounded buffers, access preferences, update Views, or write
per-packet logs.

### Video and render callbacks

Surface and decoder resources have a single owner. A callback may publish
statistics through a rate-limited snapshot, but cannot mutate UI Views directly
from a codec or GL thread.

## Thread annotations

New public entry points must use the narrowest applicable AndroidX annotation:

- `@MainThread`
- `@WorkerThread`
- `@AnyThread`

When a class is confined to one executor but no platform annotation accurately
describes it, its class documentation names the owner and executor explicitly.

## Backpressure and overload

- Input protocol events are not silently dropped.
- UI statistics may replace an older unpublished snapshot with a newer one.
- File payload queues are bounded and cancelable.
- Logs use counters, periodic summaries, or a bounded debug trace buffer rather
  than one line per event.

Any overload policy is part of the component contract and has a deterministic
test.

## Performance acceptance

On the designated 120 Hz Android test device:

- no main-thread I/O is attributable to migrated components;
- input processing adds no statistically significant regression from the
  recorded baseline;
- client-side input processing remains below 1 ms at p99;
- steady-state touch MOVE handling introduces no new unbounded allocation;
- a large clipboard or file operation cannot stall keyboard or pointer input.
