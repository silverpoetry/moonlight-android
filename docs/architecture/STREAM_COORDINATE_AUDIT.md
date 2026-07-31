# Stream Coordinate Audit

This document records every production path that submits a protocol-visible
pointer/contact position or renders the host cursor. It closes the coordinate
geometry migration by making each source space, transformation, and target
space explicit.

## Shared invariant

The stream View, touchscreen event View, and native-cursor overlay are direct
siblings under one content parent. `ViewCoordinateMapper` is the only Android
transform between those sibling spaces. It applies the source View matrix and
layout position, then the inverse target View matrix. System bars and display
cutouts are not added again because the common parent has already applied the
content insets.

Points, vectors, dimensions, and pixel-grid coordinates are distinct:

- points include translation, scale, and rotation;
- basis vectors include scale and rotation but no translation;
- dimensions use size ratios;
- host/reference pixel positions use endpoint-to-endpoint pixel-grid mapping.

## Protocol and overlay paths

| Path | Source space | Transformation | Protocol/render target |
| --- | --- | --- | --- |
| `DirectContactInputController` touch/pen position | receiving event View | sibling point mapping when the source differs, then stream-View normalization | Moonlight normalized touch/pen coordinates |
| `DirectContactInputController` contact axes/orientation | Android display-pixel contact basis | source matrix, unit-basis normalization, inverse stream matrix, per-axis stream normalization | Moonlight contact major/minor and rotation |
| `TouchInputController` legacy absolute gestures | receiving event View | sibling point mapping before `AbsoluteTouchContext`/swapped context | absolute mouse reference coordinates |
| `ExternalPointerInputController` View-relative mouse/stylus | receiving event View | sibling point mapping to stream View | absolute mouse reference coordinates |
| `ExternalPointerInputController` device absolute axes | hardware axis range | no viewport mapping; the device-reported range is the protocol reference | absolute mouse reference coordinates |
| `TouchpadMotionSender` relative pointer motion | physical finger delta | physical-DPI acceleration and sensitivity, then configured reference scaling | relative mouse delta or absolute-position delta |
| `TouchscreenTouchpadHandler` native contacts | physical touchscreen gesture surface | event-View normalization and physical millimeter sizing | native touchpad frame coordinates and dimensions |
| `NvConnection` absolute cursor owner | supplied mouse reference | one locked clamp to `[0, referenceSize - 1]`; listener and common-c receive the same values | host absolute mouse packet and local cursor state |
| `NativeCursorOverlayView` predicted/absolute position | exact `NvConnection` listener reference | pixel-grid mapping into stream View, then sibling point mapping into overlay | locally rendered host cursor |
| `NativeCursorOverlayView` cursor shape/hotspot | captured cursor/encoded frame | capture-to-encoded dimension scale, stream-to-overlay basis mapping | bitmap size and hotspot in overlay pixels |

Native touchpad coordinates intentionally do not use stream viewport mapping.
They describe a gesture surface consumed by the host's touchpad stack; applying
letterbox, zoom, or video transforms would change the gesture itself.

## Audit result

- No protocol position sender adds system-bar or cutout offsets.
- Every cross-sibling point uses `ViewCoordinateMapper`.
- Direct touch positions, contact axes, and rotation end in the same stream
  reference.
- The local cursor receives the exact clamped absolute position submitted to
  common-c. There is no host round-trip correction or second local acceleration
  path.
- Relative mode remains intentionally non-pixel-exact because host pointer
  acceleration owns the final cursor position.
- Geometry tests cover translated/scaled/rotated siblings, mapped basis
  vectors, pixel-grid endpoints, cursor dimensions, direct contacts, and both
  absolute and relative input ownership.

`ArchitectureBoundaryTest.siblingViewCoordinateMappingHasOnlyApprovedConsumers`
prevents unrelated features from taking a dependency on the sibling mapper.
New protocol position paths must be added to this audit and reuse the existing
point/vector/pixel-grid primitives before they are accepted.
