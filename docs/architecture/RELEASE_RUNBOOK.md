# Android Release Verification Runbook

This runbook is the operational gate for a Moonlight Android release. It is
not a substitute for the domain matrices in `REFACTORING_ROADMAP.md`; it makes
their build, install, upgrade, rollback, and evidence requirements
reproducible.

## 1. Freeze the candidate

Run every command from the repository root. Record the candidate commit and
verify that no source or submodule changes are present:

```powershell
git status --short
git submodule status
git rev-parse HEAD
```

The release is rejected if the worktree is dirty, a submodule is prefixed by
`+` or `-`, or Android, Moonlight Qt, Sunshine, and their submodules do not
resolve to the reviewed common-c revision.

Before invoking Gradle on Windows, reject malformed process environment data.
A quote embedded in `PATH` is interpreted as part of the test JVM command line
and can make Java treat a later path component as its main class:

```powershell
if ($env:Path.Contains('"')) {
    throw 'PATH contains an embedded quote'
}
```

Correct the invoking shell or CI environment. Do not add application or Gradle
workarounds for a malformed machine `PATH`.

## 2. Build and local verification

Use the checked-in wrapper and force execution so stale task outputs cannot
stand in for release evidence:

```powershell
.\gradlew.bat verifyLocal --rerun-tasks --max-workers=1 --no-daemon
```

This gate includes all app and core JVM tests, architecture checks, four lint
variants, four native ABIs, native and vendored dependency verification,
secret/security-policy checks, the CycloneDX application SBOM, and both
unobfuscated Release APKs.

The networknt validator used by the current CycloneDX toolchain can report
`meta:enum` and `deprecated` as unknown schema annotation keywords. These two
messages are upstream diagnostic noise, not ignored validation failures. Any
other warning, an invalid SBOM, or a non-zero task result rejects the release.

## 3. Connected verification

Select one disposable test device explicitly. Never allow a multi-device adb
session to choose the target implicitly:

```powershell
$env:ANDROID_SERIAL = 'emulator-5554'
.\gradlew.bat verifyConnected --rerun-tasks --max-workers=1 --no-daemon
```

Both root and non-root instrumentation suites must report zero failures,
errors, and skips. Preserve their XML reports from
`app/build/outputs/androidTest-results/connected` with the release evidence.

## 4. Artifact identity

For each Release APK, record its SHA-256, application ID, version, and signing
certificate. The expected tools are supplied by the configured Android SDK:

```powershell
$candidateApk = Resolve-Path `
    app\build\outputs\apk\nonRoot\release\app-nonRoot-release.apk
$rootApk = Resolve-Path `
    app\build\outputs\apk\root\release\app-root-release.apk
$apkAnalyzer = Join-Path $env:ANDROID_HOME `
    'cmdline-tools\latest\bin\apkanalyzer.bat'
$apkSigner = Get-ChildItem `
    (Join-Path $env:ANDROID_HOME 'build-tools') `
    -Filter apksigner.bat -Recurse |
    Sort-Object FullName -Descending |
    Select-Object -First 1 -ExpandProperty FullName

Get-FileHash $candidateApk -Algorithm SHA256
Get-FileHash $rootApk -Algorithm SHA256
& $apkAnalyzer manifest application-id $candidateApk
& $apkAnalyzer manifest version-code $candidateApk
& $apkAnalyzer manifest version-name $candidateApk
& $apkSigner verify --print-certs $candidateApk
```

Reject an unexpected package name, version, certificate digest, verification
failure, missing ABI, or missing native-license entry.

## 5. Clean install and cold launch

The uninstall step deletes application data. Run it only on the explicitly
selected disposable device:

```powershell
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
& $adb -s $env:ANDROID_SERIAL uninstall com.silverpoetry.moonlight
& $adb -s $env:ANDROID_SERIAL install $candidateApk
& $adb -s $env:ANDROID_SERIAL logcat -c
& $adb -s $env:ANDROID_SERIAL shell am start -W `
    -n com.silverpoetry.moonlight/com.limelight.PcView
& $adb -s $env:ANDROID_SERIAL shell pidof com.silverpoetry.moonlight
& $adb -s $env:ANDROID_SERIAL logcat -d -v brief AndroidRuntime:E '*:S'
```

The install and launch must succeed, the process must remain alive, and the
filtered crash log must be empty. Exercise settings, host discovery, manual
host entry, pairing, and one stream before accepting the clean-install leg.

## 6. Upgrade and rollback rehearsal

The previous and candidate APKs must use the same application ID and signing
certificate. Install and launch the previous APK, exercise enough UI to create
real settings and host state, then record `dataDir` and `firstInstallTime`:

```powershell
$previousApk = Resolve-Path 'path\to\reviewed-previous.apk'
& $adb -s $env:ANDROID_SERIAL install $previousApk
& $adb -s $env:ANDROID_SERIAL shell am start -W `
    -n com.silverpoetry.moonlight/com.limelight.PcView
& $adb -s $env:ANDROID_SERIAL shell dumpsys package `
    com.silverpoetry.moonlight
& $adb -s $env:ANDROID_SERIAL install -r $candidateApk
```

After upgrading, `dataDir` and `firstInstallTime` must be unchanged. Cold-launch
the candidate and verify migrated settings, paired hosts, credentials, stream
startup, and an empty `AndroidRuntime` crash log.

Where Android permits rollback without clearing data, install the reviewed
previous APK with `-r`, cold-launch it, and then restore the candidate with
another `-r` install. Both directions must be crash-free. Never use downgrade
flags or rollback experiments on a user's production device.

## 7. Cross-client release matrix

Run the current Android, Moonlight Qt, Sunshine, and common-c revisions
together. Record both success and cancellation cases:

- clipboard text, PNG, file, folder, empty file, Unicode, conflict, large tree,
  cancel, retry, and reconnect in Android-to-host and host-to-Android;
- the same clipboard matrix in Qt-to-host and host-to-Qt;
- microphone mono and stereo input, negotiated sample format/rate, mute,
  restart, long capture, and overload behavior;
- input at the target refresh rate, cursor prediction, rotation/cutout/PiP,
  controller reconnect/rumble/sensors, and stream background/foreground.

Copying a file tree must remain metadata-only until the remote side explicitly
pastes or pulls it. Normal Release logging must remain bounded and must not
contain clipboard payloads, paths, credentials, certificates, or microphone
samples.

## 8. Release evidence and rejection

Archive the four repository commit IDs, submodule revisions, Gradle summary,
instrumentation XML, APK hashes and signer digests, SBOM, matrix results, and
known environment limitations. A release is rejected for any ignored failure,
unexplained warning, dirty generated output, hidden compatibility path, or
missing matrix result.
