# Input Behavior Baseline

This document records protocol-visible and user-visible input behavior that must
remain unchanged during the input architecture migration. The automated source
of truth is the named test; this document explains why the behavior is
intentional.

## Soft-keyboard multi-finger gesture

| Behavior | Evidence |
| --- | --- |
| Disabling the gesture adds no delay to two-finger input | `SoftKeyboardGestureCoordinatorTest.disabledGestureAddsNoInputDelay` |
| Only the configured exact finger count can open the keyboard | `SoftKeyboardGestureDetectorTest.exactThreeFingerTapConsumesOnlyAfterRecognition`, `exactFiveFingerTapDefersEveryMultiContactPrefix` |
| The preceding two-finger prefix is deferred so it cannot trigger a host right click | `SoftKeyboardGestureCoordinatorTest.exactThreeFingerTapNeverDispatchesTwoFingerPrefix` |
| Movement, timeout, an extra finger, or another competing gesture releases the original events in order | `SoftKeyboardGestureDetectorTest` movement, timeout, extra-finger, and competing-gesture tests |
| Adding a third finger after an established scroll does not steal the host gesture | `SoftKeyboardGestureDetectorTest.addingThirdFingerToExistingScrollDoesNotTriggerKeyboard` |

The deferral window exists only when the configured keyboard gesture is
enabled. It cannot be replaced by unconditional input delay.

## Touchscreen touchpad ownership

| Behavior | Evidence |
| --- | --- |
| A standalone finger remains on the established mouse path | `TouchscreenTouchpadHandlerTest.standaloneFingerRemainsOnLegacyAbsoluteMousePath` |
| The second contact starts one atomic native touchpad frame containing both pointer IDs | `TouchscreenTouchpadHandlerTest.twoFingerGestureProtocolTraceIsStable` |
| A two-finger MOVE preserves pointer IDs and normalized positions in one frame | `TouchscreenTouchpadHandlerTest.twoFingerGestureProtocolTraceIsStable` |
| Ending a two-finger gesture emits UP for both native contacts atomically | `TouchscreenTouchpadHandlerTest.twoFingerGestureProtocolTraceIsStable` |
| Absolute-mouse mode consumes the remaining one-finger tail, preserving the host tap position | `TouchscreenTouchpadHandlerTest.absoluteMouseSuppressesTailWithoutMovingTapPosition` |
| Relative-touchpad mode moves the one-finger tail through the canonical mouse-motion path | `TouchscreenTouchpadHandlerTest.relativeTouchpadTailNeverUsesAbsoluteTouchscreenPosition` |
| No new native gesture begins until every suppressed contact is lifted | `TouchscreenTouchpadHandlerTest.suppressedTailRequiresAllPointersToLiftBeforeNextNativeGesture` |

The golden two-finger protocol trace is:

```text
2:DOWN#0@0.1000,0.2000|DOWN#1@0.7000,0.6000:b0
2:MOVE#0@0.1200,0.2200|MOVE#1@0.7200,0.6200:b0
2:UP#0@0.1300,0.2400|UP#1@0.7300,0.6400:b0
```

## Physical long press and barometer force press

| Behavior | Evidence |
| --- | --- |
| A press promotes the same Android contact to native ownership | `TouchscreenTouchpadHandlerTest.forcePressUsesNativeTouchpadButtonStateForSameContact` |
| Press and release use native touchpad button state, not mouse-button packets | force-press and stationary-long-press tests |
| Moving while pressed uses the same canonical mouse-motion sender as ordinary one-finger movement | `forcePressUsesNativeTouchpadButtonStateForSameContact` |
| The native contact remains pinned while mouse motion moves the cursor, avoiding host-side double acceleration | `forcePressUsesNativeTouchpadButtonStateForSameContact` |
| Long press and force press are mutually selected by configuration | `TouchscreenTouchpadHandler` press-source state and force-press tests |
| The minimum force-press duration is enforced before DOWN and release is tied to contact release | `BarometerForcePressDetectorTest` |

## Pointer acceleration and cursor motion

| Behavior | Evidence |
| --- | --- |
| Low-speed movement uses a reachable precision factor below unity | `TouchpadPointerAccelerationTest.lowSpeedMotionUsesPrecisionDeceleration` |
| Ordinary movement remains on the unity plateau | `ordinaryMotionUsesUnityPlateau` |
| Fast motion accelerates but stays bounded | `fastMotionUsesBoundedAcceleration` |
| Direction reversal and a long pause discard previous fast history | direction-reversal and long-pause tests |
| Shared parent Insets are not applied twice to the local cursor | `ViewCoordinateMapperTest.sharedParentInsetIsNotCountedAsCursorOffset` |
| Source and target transforms are each applied exactly once | `ViewCoordinateMapperTest.sourceAndTargetTransformsAreIncludedExactlyOnce` |

The local cursor is predictive client UI. It is updated from the same canonical
mouse delta or absolute position submitted to the host. It is not corrected
from a delayed host cursor-position response.

## Controller and external-device input

The first input migration does not alter controller mapping, evdev capture,
physical mouse buttons, stylus dead zones, or Android key translation. Those
paths remain delegated to their existing implementations until dedicated
characterization tests exist.

## Release acceptance

Before the old `Game` input implementation is deleted, the designated Android
device and Windows host must confirm:

- one-finger cursor movement and click;
- two-finger right click, scroll, and pinch;
- three-finger and configured multi-finger keyboard gestures;
- long press and barometer force press, including drag while pressed;
- no cursor jump after a multi-finger gesture;
- no stuck contacts after focus loss, cancellation, PiP, or stream exit;
- physical mouse, keyboard, stylus, and controller input;
- 120 Hz responsiveness with release logging.
