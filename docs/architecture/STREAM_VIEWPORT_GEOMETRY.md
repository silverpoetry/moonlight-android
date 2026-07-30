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
measurement, legacy display-aspect compatibility, and fixed FSR output sizing.
`StreamView` and `VideoProcessingGLSurfaceView` delegate to the same fit
calculation, so the decoded surface and post-processed output cannot diverge by
rounding or branch choice. View measurement truncates to Android's historical
pixel result; FSR surface requests round first and then enforce even
dimensions.

## Performance

Mouse-position callbacks reuse preallocated point, basis, and matrix scratch
objects. No collection or geometry object is allocated per cursor movement.

## View hierarchy invariant

The primary stream View, background input View, FSR output, and cursor overlay
are direct siblings under one untransformed content parent. The parent may be
translated by the single inset policy, but it is not scaled or rotated.
`ViewCoordinateMapper` verifies the sibling relationship and rejects mapping
rather than guessing when that invariant is not satisfied.

Moving rendering into another window, such as an external-display
`Presentation`, is a render-ownership transition. It must move the complete
render/overlay ownership set and provide an explicit cross-window input policy;
reparenting only the decoder View is not a coordinate conversion.
