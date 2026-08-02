# Local patches

This module is based on the exact upstream revision recorded in
`UPSTREAM-METADATA.txt`, with the following reviewed local adaptation:

- Route exceptional diagnostics through a module-local, Debug-only logging
  boundary so Release builds cannot emit binder or controller stack traces.

The complete patched tree is integrity-locked by
`gradle/vendored-dependencies.json`.
