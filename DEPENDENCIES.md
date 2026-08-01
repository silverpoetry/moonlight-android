# Dependency policy

Moonlight Android resolves Java dependencies only from repositories declared in
`settings.gradle`. Versions are centralized in `gradle/libs.versions.toml`, and
Gradle dependency verification pins downloaded artifacts in
`gradle/verification-metadata.xml`.

The complete CycloneDX 1.6 application SBOM is generated at
`build/reports/sbom/moonlight-android.cdx.json`. It combines repository-resolved
Gradle components with checked-in native and reviewed source dependencies.

Checked-in native code is permitted only when all of the following are true:

- the upstream project, exact version, HTTPS source URL, source SHA-256, license,
  NDK revision, minimum API, ABI set, and expected files are recorded in
  `gradle/native-dependencies.json`;
- the source build is automated by a reviewed script in `tools/`;
- independent builds are byte-for-byte reproducible for a representative ABI;
- the checked-in tree digest passes `verifyNativeDependencies`;
- the upstream license is packaged verbatim in the APK; and
- all four supported ABIs link and pass the native runtime self-tests.

Local AAR and JAR files are forbidden. If a Java dependency cannot be obtained
from a verifiable repository or replaced with maintainable source in this
project, it must not be shipped.

Checked-in third-party source additionally requires an immutable upstream Git
commit, an in-tree content digest, upstream metadata, and a verbatim packaged
license in `gradle/vendored-dependencies.json`. Local modifications remain
visible in the content digest and in the SBOM's `modified` field.

## Checked-in native dependencies

| Dependency | Version | Purpose | Rebuild command |
| --- | --- | --- | --- |
| OpenSSL | 3.5.7 LTS | TLS and protocol cryptography | `ANDROID_NDK_HOME=/path/to/android-ndk-r27 tools/build_openssl_android.sh OUTPUT` |
| libopus | 1.6.1 | Stream audio decoding and microphone encoding | `ANDROID_NDK_HOME=/path/to/android-ndk-r27 tools/build_opus_android.sh OUTPUT` |

## Checked-in source dependencies

| Dependency | Version | Upstream commit | Purpose |
| --- | --- | --- | --- |
| ShieldControllerExtensions | 1.0.1 | `48356a2263839949fdfdf0795b3d8cc38edeef9b` | NVIDIA SHIELD controller accessory-service integration |

Both scripts reject a toolchain other than Android NDK `27.0.12077973`, verify
the downloaded source archive before extraction, use API 21 for every ABI, and
refuse a non-empty output directory. Set `OPENSSL_ABIS` or `OPUS_ABIS` to a
space-separated subset only for reproducibility checks; production updates must
build all four ABIs.

After updating a native dependency, copy only the generated public headers,
static archives, license, and build metadata into its integration directory.
Update the tree digest in `gradle/native-dependencies.json`, then run:

```powershell
.\gradlew.bat verifyNativeDependencies verifyVendoredDependencies verifyLocal --no-configuration-cache
```

The full release gate also builds both application flavors. Native runtime
cryptography is checked at library load and by instrumentation tests so an ABI
that links incorrectly fails before streaming starts.
