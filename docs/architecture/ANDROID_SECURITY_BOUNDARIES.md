# Android security boundaries

This document records the Android attack surface that is intentional and
release-gated. It is a product contract, not a list of optional hardening
ideas. Changes to any boundary below require a threat review, focused tests,
and an update to `verifyAndroidSecurityPolicy`.

## Component exposure

Every activity, service, provider, and receiver declares `android:exported`
explicitly. Only these production components are externally reachable:

- `PcView`: launcher entry point;
- `ShortcutTrampoline`: explicit launcher-shortcut target;
- `FilePushActivity`: Android `SEND`/`SEND_MULTIPLE` share target;
- `PosterContentProvider`: validated, read-only Android TV poster cache.

The stream activity, settings activities, host services, USB service,
accessibility service, and `FileProvider` are internal. The debug manifest may
export `Game` only as a test entry point; that override must never enter a
Release manifest.

## Private files and sharing

Persistent app files are not direct `FileProvider` roots. Outbound shares are
copied atomically into a unique directory below
`cache/outbound-shares/`, and clipboard images must already be regular files
below `cache/clipboard/`. Both APIs validate canonical containment before
creating a URI.

The provider exposes exactly those two cache subdirectories. It exposes no
device root, external storage, entire cache directory, or entire files
directory. Read-only URI grants are issued only with the matching share or
clipboard item. Expired outbound sessions are removed on later shares.

## Backup and device transfer

Standard Android backup remains available for benign, user-authored files.
The same device-bound state is excluded from legacy backup, encrypted cloud
backup, and device-to-device transfer:

- all shared preferences;
- all host databases and pinned host certificates;
- `client.key` and `client.crt`;
- the generated `uniqueid`.

Restoring a host database without the matching client identity would create a
misleading, partially paired state. Users who intentionally move pairing data
must use the bounded settings import/export actions for hosts, certificate,
and private key. Cache and no-backup directories remain excluded by Android.

## Network trust

GameStream/Sunshine discovery and pairing require HTTP to user-selected LAN
hosts and numeric addresses, so a finite domain allowlist cannot model the
cleartext exception. The network-security configuration therefore permits
cleartext at base scope but accepts only Android system trust anchors. HTTPS
uses either system trust or the exact host certificate pinned during pairing;
no user CA, debug CA, trust-all manager, or hostname bypass belongs in the
release policy.

## Credentials and signing

Client identity material is stored only in the app sandbox and excluded from
automatic backup. Explicit export is a user action and stages a temporary,
read-only cache copy. Local keystores, private-key formats, signing property
files, and exported client credentials are ignored by Git; no signing secret
is tracked in the repository.

Release uses full R8 optimization, shrinking, and obfuscation. Every published
APK must retain its exact mapping file as release evidence so production stack
traces can be retraced. Debug remains unminified for interactive diagnostics.
Signing compatibility with installed builds is separate from code hardening:
any future CI/release-key change must inject credentials outside source control
and rehearse upgrades on both product flavors before replacing the current
workspace signing identity.

## Enforcement

`verifyAndroidSecurityPolicy`, included in `verifyLocal`, parses the source
manifest, both fully merged Release manifests, and security XML. It fails when:

- a component omits an explicit exported state or the exported allowlist
  changes;
- a Debug-only stream/provider entry point, debuggable/test-only flag, or
  shell-profileable declaration enters either Release flavor;
- a broad or unknown `FileProvider` path appears;
- any backup mode can move settings, host databases, or client identity;
- the network trust-anchor surface changes;
- a signing artifact, private-key format, or exported client credential is
  placed anywhere inside the repository.

Connected instrumentation additionally proves that staged files are readable,
direct persistent-file URIs are rejected, clipboard cache siblings are
rejected, and external files cannot enter the staging path.
