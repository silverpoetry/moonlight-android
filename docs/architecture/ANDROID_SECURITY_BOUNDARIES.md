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

The two concrete stream entries, settings activities, host services, USB
service, accessibility service, and `FileProvider` are internal. `Game` is the
shared abstract stream implementation and is never registered as an Android
component. The landscape and portrait entries provide only the immutable
initial window orientation; runtime rotation remains inside the shared stream
session.

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

After a remote clipboard file pull commits successfully, its completion value
contains only the regular document URIs created by that transaction. The
system Sharesheet receives those exact files through a temporary read-only URI
grant; the selected document tree and unrelated files in the destination
directory are never shared. Nested regular files may be shared together, while
empty directories produce no share action.

## Backup and device transfer

Standard Android backup remains available for benign, user-authored files.
The same device-bound state is excluded from legacy backup, encrypted cloud
backup, and device-to-device transfer:

- all shared preferences;
- all host databases and pinned host certificates;
- `client.key` and `client.crt`;
- the generated `uniqueid`.

Explicit transfer therefore uses one versioned configuration ZIP with three
selectable components: typed App settings, host connection information, and
client identity. Host connection information contains the portable host
database and pinned host certificates and can be imported without the current
client identity; pairing status is resolved by the host during the next
refresh. Client identity contains the client certificate and private key as a
single matching pair and cannot be partially selected. System document-tree
URIs are excluded because their Android permission grants cannot be
transferred. Cache and no-backup directories remain excluded by Android.

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
automatic backup. Explicit export is a user action through Android's
`ACTION_CREATE_DOCUMENT` save flow. The archive is generated after the user
chooses the target file; its manifest fixes the format version, component
membership, uncompressed sizes, and SHA-256 digests. Import bounds both the
archive and every entry, rejects unexpected or nested paths, verifies every
digest, checks that the certificate matches the private key, and validates
every selected component before changing live state. Local keystores,
private-key formats, signing property files, and exported client credentials
are ignored by Git; no signing secret is tracked in the repository.

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
- the abstract stream implementation is registered, either concrete stream
  entry becomes exported or loses its declared initial orientation, or a
  Debug-only provider, debuggable/test-only flag, or shell-profileable
  declaration enters either Release flavor;
- a broad or unknown `FileProvider` path appears;
- any backup mode can move settings, host databases, or client identity;
- the network trust-anchor surface changes;
- a signing artifact, private-key format, or exported client credential is
  placed anywhere inside the repository.

Connected instrumentation additionally proves that staged files are readable,
direct persistent-file URIs are rejected, clipboard cache siblings are
rejected, and external files cannot enter the staging path.
