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

Lint uses a checked-in baseline for reviewed legacy findings and treats every finding
outside that baseline as an error. Do not regenerate the baseline merely to make a build
pass: fix new findings, or document why a baseline change is necessary and review the
baseline diff with the code change.

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
