# Stream Viewport Geometry

The stream screen uses explicit coordinate spaces. No component may infer a
status-bar, cutout, or navigation-bar offset independently.

## Coordinate spaces

| Space | Owner | Meaning |
| --- | --- | --- |
| Encoded frame | stream configuration | Decoder pixel dimensions |
| Host reference | input/cursor protocol event | Addressable host pixel grid for that event |
| Stream view | Android layout | Local, untransformed coordinates of the video `View` |
| Overlay view | Android layout | Local coordinates used to draw the cursor and overlays |
| Activity content | window policy | Shared parent rectangle after the single inset policy |

`UiHelper.configureStreamWindowInsets()` applies safe-area padding only to the
shared content root. Video, input surfaces, and the cursor overlay therefore
move together; child components never add those insets again.
`StreamWindowPolicy` owns the pure decision to use the entire physical display;
the Activity adapter only supplies Android display-mode observations.

## Mapping rules

- Pixel positions map endpoint-to-endpoint:
  `[0, sourceSize - 1] -> [0, targetSize - 1]`.
- Lengths and bitmap dimensions use `targetSize / sourceSize`.
- Protocol cursor scale is first converted from Q16 capture-to-encoded scale,
  then from encoded pixels to stream-view pixels.
- The complete Android `View` matrix maps points and basis vectors between the
  stream and overlay siblings. Layout translation affects positions but not
  lengths; scale and rotation affect both.
- Android reports touch/tool major and minor axes in display pixels even after
  it transforms event positions and orientation into a child `View`. Direct
  contact input therefore reconstructs physical axis directions through the
  event source matrix, normalizes them in display space, and transforms them
  into the stream view before protocol normalization.
- Invalid protocol coordinates are clamped. Invalid dimensions reject the
  update or use an identity dimension scale; they never introduce NaN or
  infinity.

`StreamViewportGeometry` owns the pure pixel-grid math.
`ViewCoordinateMapper` owns Android sibling transforms and is shared by cursor
rendering, direct touch/pen input, external pointer input, and the legacy
absolute-mouse gesture path. Device-absolute pointer events remain in their
hardware-reported coordinate space because their reference dimensions come
from the device motion ranges rather than an Android `View`.
`NativeCursorOverlayView` composes both, so cursor position, hotspot, and shape
scale use the same viewport geometry.
`ViewWindowGeometry` is the only adapter that publishes transformed View bounds
in window coordinates, including the source rectangle used by picture-in-picture
transitions.

## Layout rules

`StreamLayoutGeometry` is the single pure-Java source for aspect-fit
measurement and legacy display-aspect compatibility. `StreamView` delegates
to that calculation, so layout and coordinate mapping cannot diverge by
rounding or branch choice. View measurement preserves Android's historical
pixel result.

## Viewport pinch gestures

`StreamPinchZoomController` recognizes local zoom before the existing touch-mode
and keyboard-gesture routing. Recognition uses coordinates in the shared,
unscaled content parent. Single-finger input passes through immediately. Only
the ambiguous two-contact prefix is buffered, for at most the platform double-tap
timeout from the second contact and a bounded number of events. Finger-relative
motion must dominate common translation, and radial span change must exceed a
physical movement threshold and account for a substantial part of that relative
motion before zoom takes ownership. This separates pinching from scrolling and
rotation without comparing translation against radial motion alone. The
threshold does not grow with video scale or initial finger separation, and the
same physical-distance comparisons apply when the path is reversed.
Parallel scrolling, additional fingers, an existing drag/press, or the decision
deadline hands the original events back in order. Released taps use the same
timing-preserving dispatcher as keyboard-gesture arbitration.

A recognized pinch cancels the remote contacts and owns its remaining contacts
until all fingers lift. It scales and pans the video around the finger focus.
Lifting one finger pauses the transform; replacing it rebases the focus and span
without a jump, then continues the owned pinch. All contacts lifting or a cancel
ends ownership, so the next gesture starts fresh. Single-finger dragging and
double tapping retain their selected input
mode's meanings. `StreamView` only presents video; it does not intercept touch
events or suspend the input mode when zoom is enabled.

Timed tap replay distinguishes its already classified prefix from queued new
input. Only the prefix bypasses recognition. While a keyboard prefix is replaying,
new events queue before viewport recognition, so replay cannot deliver a second
`DOWN` to the pinch state machine or reset its pending decision. Each hardware
event's history is mapped with one source transform before the viewport changes.

`StreamZoomGeometry` computes focus-preserving transforms and viewport bounds.
Normal absolute input continues through `ViewCoordinateMapper` exactly once;
touchpad motion remains in the unchanged physical input surface. Each viewport
change also reprojects the cached local cursor position and scale, without
waiting for another host cursor packet or changing its predicted position.

## Performance

While dragging the floating mouse near a screen edge, `FloatingMouseEdgePan`
temporarily translates the stream into the hotspot's reachable rectangle.
This applies at native scale as well as zoomed scale. The control remains on
screen; the remote cursor continues through the same inverse View transform.
Frame callbacks run only during active edge dragging and stop on release,
cancellation, or reaching the content boundary. Closing the control restores
the prior translation unless a separate zoom or layout change superseded it.
Edge speed ramps with proximity to the boundary up to 900 dp/s. A continuous
Choreographer frame clock owns integration; touch events update velocity without
restarting that clock. Pointer reprojection occurs after each viewport update.
Each axis requires an outward drag to arm edge panning. Reversing direction
disarms that axis immediately, even within the edge zone; stationary contact
only continues an already armed pan.
The mouse opens centered on the floating ball, constrained to the screen.
Minimizing applies only the actual drag displacement to the saved ball position
and resumes its normal edge docking and idle-collapse lifecycle.

Mouse-position callbacks reuse preallocated point, basis, and matrix scratch
objects. No collection or geometry object is allocated per cursor movement.

## View hierarchy invariant

The primary stream View, background input View, and cursor overlay are direct
siblings under one untransformed content parent. The parent may be
translated by the single inset policy, but it is not scaled or rotated.
`ViewCoordinateMapper` verifies the sibling relationship and rejects mapping
rather than guessing when that invariant is not satisfied.

Moving rendering into another window, such as an external-display
`Presentation`, is a render-ownership transition. It must move the complete
render/overlay ownership set and provide an explicit cross-window input policy;
reparenting only the decoder View is not a coordinate conversion.

## Protocol and presentation ownership audit

The following table is the phase-four closeout inventory. A new sender or
consumer must be added here or be covered by an equivalent domain document.

| Path | Coordinate space | Canonical owner |
| --- | --- | --- |
| Native cursor position, hotspot, and bitmap size | host reference -> encoded frame -> stream View -> overlay View | `NativeCursorOverlayView`, `StreamViewportGeometry`, `ViewCoordinateMapper` |
| Touchscreen absolute mouse | event View -> stream View, then stream View reference dimensions | `TouchInputController` maps before `AbsoluteTouchContext` dispatch |
| Floating mouse hotspot | panel -> common window root -> inverse stream View transform | `FloatingMousePanel` maps nested ancestors and zoom; `NvConnection` owns absolute position and cursor notification |
| Android direct touch and pen | event View -> stream View -> normalized protocol position/contact axes | `DirectContactInputController`, `ViewCoordinateMapper` |
| External pointer in View-absolute mode | event View -> stream View and stream View reference dimensions | `ExternalPointerInputController`, `ViewCoordinateMapper` |
| Device-absolute pointer | device motion ranges and hardware-reported coordinates | `ExternalPointerInputController`; deliberately independent from View geometry |
| Relative mouse and touchscreen touchpad cursor motion | deltas plus host reference dimensions | `TouchpadMotionSender`; no absolute View offset |
| Native touchscreen touchpad frames | physical input-surface position, pressure, size, and pointer IDs | `TouchscreenTouchpadHandler`; the surface is the emulated physical touchpad |
| Physical controller touchpad | controller device motion ranges | `ControllerHandler`; controller-local hardware space |
| evdev mouse motion | relative device deltas | `Game` platform callback pending runtime-controller extraction |
| Stream aspect fit | layout and surface pixel dimensions | `StreamLayoutGeometry` |
| Safe-area/cutout content rectangle | window insets -> shared content-root padding | `WindowInsetsPolicy`, `UiHelper` |
| Full-physical-display eligibility | settings/native-resolution state plus display modes | `StreamWindowPolicy` |
| Picture-in-picture animation hint | transformed visible stream bounds in Activity window coordinates | `ViewWindowGeometry` |

The audit intentionally distinguishes spatial coordinates from hardware and
relative coordinate spaces. Converting every path through a View mapper would
be incorrect: controller touchpads and device-absolute pointers define their
own protocol reference grids, while relative motion contains no absolute
position to map.
