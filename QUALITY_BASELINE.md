# Android Release Quality Baseline

Captured on 2026-07-30 before the API 36 quality migration.

> Historical baseline: the current application contract has since moved to
> `compileSdk`/`targetSdk` 37 and `minSdk` 23 as part of the Compose Material 3
> presentation migration. Values below describe the recorded pre-migration
> release and are intentionally not rewritten.

## Source and Toolchain

- Source commit: `bb9dac548dc7c9864a9b076c836e33ab1c9a5f9d`
- Branch: `ui-polished-dialogs`
- Gradle: 8.7
- Android Gradle Plugin: 8.5.1
- Java: Eclipse Temurin 17.0.19
- NDK: 27.0.12077973
- `compileSdk`: 34
- `targetSdk`: 34
- `minSdk`: 21
- `moonlight-common-c`: `7d1e37b8d81537926bea15b6fd817f301039352a`

## Local Verification

Command:

```powershell
.\gradlew.bat verifyLocal --console=plain
```

Result: passed.

- JVM test report files: 20
- JVM tests: 80
- Failures: 0
- Errors: 0
- Skipped: 0
- All four root/non-root debug/release Lint variants passed.
- Both root and non-root Release APKs built successfully.

## Release Artifacts

| Flavor | Size | SHA-256 |
| --- | ---: | --- |
| `nonRootRelease` | 14,747,115 bytes | `46EC70E06CE162C37147E17B7188C652C27D23572EA140D6731B57A740F82EF8` |
| `rootRelease` | 14,768,068 bytes | `E6BCA9DE072D95763A01EA1D916B5EA15544DB0CA39F461D7BD1FACF858921AF` |

Release minification is disabled for both artifacts.

## Lint Inventory

The reviewed baseline contains 67 entries:

| Lint ID | Count |
| --- | ---: |
| `UnusedAttribute` | 31 |
| `VectorPath` | 19 |
| `GradleDependency` | 6 |
| `SourceLockedOrientationActivity` | 3 |
| `Overdraw` | 2 |
| `VectorRaster` | 2 |
| `DiscouragedApi` | 1 |
| `IconLauncherShape` | 1 |
| `IconMissingDensityFolder` | 1 |
| `OldTargetApi` | 1 |

## Connected Verification

The previously configured device `192.168.3.125:5555` was offline and refused an ADB
connection during this capture. Connected tests are therefore explicitly **not
recorded as passed** in this baseline. They remain a required release gate and must run
when exactly one target device is online.
