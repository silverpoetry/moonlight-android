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

`StreamSessionUiEffects` is the idempotent main-thread owner for the
connecting/connected/ended platform notifications and the keep-screen-on flag.
It also owns the delayed input-grab task. Every accepted end path cancels that
task, releases input capture, clears the flag, and publishes the ended state
once.

`StreamLaunchReporter` owns the single-use background task that updates Android
shortcut and TV-channel launch metadata. The task cannot run on the connection
callback or main thread, is accepted at most once, contains platform failures,
and is canceled when the Activity is destroyed. Its helpers depend on
application-safe `Context` unless an operation explicitly requires an
`Activity`.

`StreamSessionCallbackRouter` is the only UI-layer implementation of
`NvConnectionListener`. It serializes presentation, window, HDR, and cursor
callbacks on the main thread; snapshots mutable native payloads before crossing
that asynchronous boundary; and leaves controller feedback on the immediate
low-latency path. Destruction atomically rejects new callbacks and removes
queued presentation work. `Game` now supplies narrow UI and feedback hosts
instead of implementing the transport listener.

`StreamMediaResourceOwner` is the main-thread owner for the decoder reference,
single-use audio renderer reference, and render-target binding. The transport
still performs renderer cleanup after a successful start; the UI owner controls
reachability. Failed starts, normal stops, and Activity destruction all clear
the active audio reference through the same boundary.

`StreamRenderSurfaceController` owns the Activity-scoped `SurfaceHolder`
registration and render-target readiness. It applies the selected frame-rate
hint, starts the session only after both the Surface and composed dependencies
are ready, prepares the decoder before Surface loss, and detaches its callback
before session teardown. Its pure transition state rejects out-of-order
callbacks and requires a recreated Surface to become valid again.

Activity teardown detaches session callbacks, cancels owned UI tasks, releases
controllers, registrations, locks, and media resources, and only then invokes
the framework `super.onDestroy()` callback.

## Startup latency diagnostics

Treat the displayed stage as presentation state, not a measurement. Debug
builds record preparation-stage durations on the transport callback thread and
the delay before the main thread presents each stage. HTTP diagnostics identify
only the operation and measure response headers and complete body receipt.
Native RTSP diagnostics separate TCP connection, first-byte, total transaction
time, and retry count. Release builds remove these diagnostics.

Encrypted RTSP responses end when their declared frame length has arrived;
waiting for the peer's TCP close can add latency through an intermediary.
Length validation rejects empty, oversized, downgraded, and excess frames;
the existing AES-GCM authentication and message parser still validate the frame.
Plaintext responses retain the legacy close-delimited behavior. Fragmented
headers and payloads remain incomplete until the entire frame is received.

The `rtsp-framing-test` CTest target covers fragmentation, completion without
EOF, length limits, invalid encryption flags, and excess bytes. Compare startup
on the same device and route, separating app launch/resume, RTSP, decoder setup,
and Activity handoff. Device and host wall clocks may differ; compare durations
within each clock and correlate protocol events before aligning timestamps.

## Negotiated startup connection reuse

For encrypted TCP RTSP, OPTIONS requests advertise
`X-SS-Persistent-RTSP: 1`. Only an authenticated 200 OPTIONS response with the
same version permits socket reuse. DESCRIBE, SETUP, ANNOUNCE and PLAY then share
one connection. Handshake exit, a protocol error or a transport failure closes
the socket. An unsupported host retains the original connection lifecycle;
state-changing requests are never automatically replayed after a lost response.

HTTP transports retain their TLS session context only after an authenticated
serverinfo response advertises `TlsSessionResumption=1`. This capability requires
the host's mutual-TLS session ID context to be initialized. Old hosts continue
using a new context per request. Pin changes invalidate both the capability and
cached sessions; trust, hostname validation and OkHttp's TLS protocol policy
remain in force. The cache belongs to one NvHTTP instance, not a global client.

The Android launch indicator describes the composite host phase as preparing
the host session and the RTSP phase as negotiating stream parameters. Transport
callbacks retain their original stage names for diagnostics and failure routing.

`rtsp-connection-reuse-test` verifies the exact opt-in and legacy fallback.
`NvHttpTlsStateTest` covers session context ownership, authenticated-capability
gating, invalidation and protocol restrictions. Validate complete startup against
both a supporting Sunshine and a legacy host before deploying the two endpoints.
