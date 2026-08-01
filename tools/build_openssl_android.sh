#!/usr/bin/env bash

set -euo pipefail

readonly OPENSSL_VERSION="3.5.7"
readonly OPENSSL_SHA256="a8c0d28a529ca480f9f36cf5792e2cd21984552a3c8e4aa11a24aa31aeac98e8"
readonly REQUIRED_NDK_REVISION="27.0.12077973"
readonly SOURCE_URL="https://github.com/openssl/openssl/releases/download/openssl-${OPENSSL_VERSION}/openssl-${OPENSSL_VERSION}.tar.gz"
readonly REQUESTED_ABIS="${OPENSSL_ABIS:-armeabi-v7a arm64-v8a x86 x86_64}"

usage() {
  echo "Usage: ANDROID_NDK_HOME=/path/to/ndk/$REQUIRED_NDK_REVISION $0 OUTPUT_DIRECTORY" >&2
}

if [[ $# -ne 1 || -z "${ANDROID_NDK_HOME:-}" ]]; then
  usage
  exit 2
fi

readonly output_dir="$1"
readonly ndk_dir="$(realpath "$ANDROID_NDK_HOME")"
readonly ndk_properties="$ndk_dir/source.properties"

if [[ ! -f "$ndk_properties" ]]; then
  echo "Android NDK metadata is missing: $ndk_properties" >&2
  exit 2
fi

readonly ndk_revision="$(sed -n 's/^Pkg.Revision = //p' "$ndk_properties")"
if [[ "$ndk_revision" != "$REQUIRED_NDK_REVISION" ]]; then
  echo "Expected Android NDK $REQUIRED_NDK_REVISION, got $ndk_revision from $ndk_dir" >&2
  exit 2
fi

if [[ -e "$output_dir" ]] && [[ -n "$(find "$output_dir" -mindepth 1 -print -quit)" ]]; then
  echo "Output directory must be absent or empty: $output_dir" >&2
  exit 2
fi

readonly work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

mkdir -p "$output_dir"
readonly resolved_output_dir="$(realpath "$output_dir")"
case "$resolved_output_dir" in
  /|/home|/mnt|/mnt/*/)
    echo "Refusing unsafe output directory: $resolved_output_dir" >&2
    exit 2
    ;;
esac

readonly archive="$work_dir/openssl-${OPENSSL_VERSION}.tar.gz"
curl --fail --location --silent --show-error "$SOURCE_URL" --output "$archive"
echo "$OPENSSL_SHA256  $archive" | sha256sum --check --status

tar --extract --gzip --file "$archive" --directory "$work_dir"
readonly source_dir="$work_dir/openssl-${OPENSSL_VERSION}"

export PATH="$ndk_dir/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH"
export ANDROID_NDK_ROOT="$ndk_dir"
export SOURCE_DATE_EPOCH=1780963200
export ZERO_AR_DATE=1

readonly common_args=(
  no-apps
  no-docs
  no-engine
  no-module
  no-shared
  no-ssl3
  no-stdio
  no-tests
  no-ui-console
)

build_abi() {
  local abi="$1"
  local target="$2"
  local api="$3"
  local build_dir="$work_dir/build-$abi"
  local stage_dir="$work_dir/stage-$abi"
  local abi_output="$resolved_output_dir/$abi"

  cp -a "$source_dir" "$build_dir"
  mkdir -p "$stage_dir"
  pushd "$build_dir" >/dev/null
  ./Configure \
    "$target" \
    --prefix=/usr \
    --openssldir=/etc/ssl \
    --libdir=lib \
    "${common_args[@]}" \
    -Wno-macro-redefined \
    "-D__ANDROID_API__=$api"
  make --jobs="${BUILD_JOBS:-$(nproc)}" build_libs
  make DESTDIR="$stage_dir" install_dev

  mkdir -p "$abi_output/include"
  cp libcrypto.a libssl.a "$abi_output/"
  cp -a "$stage_dir/usr/include/openssl" "$abi_output/include/"
  popd >/dev/null
}

if [[ -z "${REQUESTED_ABIS// }" ]]; then
  echo "OPENSSL_ABIS must contain at least one supported ABI" >&2
  exit 2
fi

for abi in $REQUESTED_ABIS; do
  case "$abi" in
    armeabi-v7a)
      build_abi "$abi" android-arm 21
      ;;
    arm64-v8a)
      build_abi "$abi" android-arm64 21
      ;;
    x86)
      build_abi "$abi" android-x86 21
      ;;
    x86_64)
      build_abi "$abi" android-x86_64 21
      ;;
    *)
      echo "Unsupported ABI in OPENSSL_ABIS: $abi" >&2
      exit 2
      ;;
  esac
done

cp "$source_dir/LICENSE.txt" "$resolved_output_dir/LICENSE.txt"
printf '%s\n' \
  "name=OpenSSL" \
  "version=$OPENSSL_VERSION" \
  "source=$SOURCE_URL" \
  "source_sha256=$OPENSSL_SHA256" \
  "ndk=$REQUIRED_NDK_REVISION" \
  "min_api=21" \
  "abis=${REQUESTED_ABIS// /,}" \
  > "$resolved_output_dir/BUILD-METADATA.txt"

echo "OpenSSL $OPENSSL_VERSION Android artifacts written to $resolved_output_dir"
