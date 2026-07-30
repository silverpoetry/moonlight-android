# Testing

The project has two verification levels. A successful APK build alone does not run the
complete test suite.

## Local verification

```powershell
.\gradlew.bat verifyLocal
```

This runs:

- all JVM tests for every root/non-root and debug/release variant;
- Android Lint for every variant;
- both root and non-root Release builds, including release-critical Lint checks.

Lint has no baseline and treats every finding as an error. Intentional exceptions are
scoped to the exact manifest element or resource that needs them and include an adjacent
rationale. `MissingTranslation` remains the sole project-level disabled check because
community translations are intentionally incomplete.

## Connected-device verification

Connect exactly one Android device or set `ANDROID_SERIAL`, then run:

```powershell
$env:ANDROID_SERIAL = "device-address:5555"
.\gradlew.bat verifyConnected
```

This includes local verification and runs the complete instrumentation suite for both
the root and non-root debug flavors. Gradle fails when any test fails or no target device
is available.

To run a single instrumentation class while developing, use the AndroidJUnitRunner
directly. A class-only run is not release verification and must be followed by
`verifyConnected`.

## Native protocol tests

`moonlight-common-c` is a Git submodule with its own CMake/CTest suite. Android's
`ndk-build` compiles the library but does not execute those host-side native tests. Run
the common-c CTest suite in that repository whenever the shared protocol library changes.
On MinGW/UCRT builds, the compiler runtime `bin` directory must be on `PATH` while running
CTest; Windows exit code `0xc0000135` means a runtime DLL was not found, not that a test
assertion failed.
