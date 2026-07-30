# Stream Session Lifecycle

`StreamSessionController` is the single owner of an `NvConnection` lifecycle.
The connection and controller are single-use. Reconnection creates a new pair.

## State transitions

| Current state | Event | Next state | Observer callback |
| --- | --- | --- | --- |
| `CREATED` | `start()` accepted | `STARTING` | none |
| `CREATED` | `stop()` or `destroy()` | `STOPPING` | none |
| `STARTING` | connection started | `STREAMING` | connection started |
| `STARTING` | stage failed | `FAILED` | stage failed |
| `STARTING` | connection terminated | `TERMINATED` | terminated |
| `STREAMING` | connection terminated | `TERMINATED` | terminated |
| `STARTING`, `STREAMING`, `TERMINATED`, `FAILED` | `stop()` | `STOPPING` | none |
| `STOPPING` | cleanup completed | `STOPPED` | none |
| `STOPPING` | cleanup failed | `FAILED` | none |

All other transitions are rejected. In particular:

- start is accepted exactly once;
- stop is safe before start and is scheduled exactly once;
- callbacks received after stop or destruction are ignored;
- an intentional stop cannot surface as an unexpected termination;
- transport cleanup never runs on the Android main thread;
- cleanup failure changes the observable state and cannot crash the process from
  an uncaught stop-thread exception.

## Callback acceptance

Startup progress and startup failures are accepted only in `STARTING`.
Connection health, HDR, cursor, haptics, and controller feedback are accepted
only in `STREAMING`. User-visible transport messages are accepted in
`STARTING` and `STREAMING`.

`destroy()` first detaches the UI delegate, then requests idempotent transport
cleanup. This prevents a transport owned by a destroyed Activity from
delivering new callbacks through the session controller.

## Remaining ownership migration

The controller owns transport state. `StreamFailureDiagnostics` owns the single
cancelable worker used for connectivity probes; those probes never block the
connection callback or Android main thread, and their queued results are
invalidated when the Activity is destroyed.

`StreamWifiLockController` owns both supported Wi-Fi performance locks. It
acquires each mode independently, releases only held locks in reverse order,
and makes partial failure and repeated destruction safe.

The next migration slices move render/audio resource lifetime and stream UI
effects behind session-scoped ports. Until those slices are complete, `Game`
remains the presentation adapter for accepted callbacks.
