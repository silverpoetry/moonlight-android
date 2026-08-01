#!/usr/bin/env bash

set -euo pipefail

readonly OPUS_VERSION="1.6.1"
readonly OPUS_SHA256="6ffcb593207be92584df15b32466ed64bbec99109f007c82205f0194572411a1"
readonly REQUIRED_NDK_REVISION="27.0.12077973"
readonly SOURCE_URL="https://downloads.xiph.org/releases/opus/opus-${OPUS_VERSION}.tar.gz"
readonly REQUESTED_ABIS="${OPUS_ABIS:-armeabi-v7a arm64-v8a x86 x86_64}"

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

readonly archive="$work_dir/opus-${OPUS_VERSION}.tar.gz"
curl --fail --location --silent --show-error "$SOURCE_URL" --output "$archive"
echo "$OPUS_SHA256  $archive" | sha256sum --check --status

tar --extract --gzip --file "$archive" --directory "$work_dir"
readonly source_dir="$work_dir/opus-${OPUS_VERSION}"
readonly toolchain_dir="$ndk_dir/toolchains/llvm/prebuilt/linux-x86_64/bin"

export SOURCE_DATE_EPOCH=1768435200
export ZERO_AR_DATE=1

build_abi() {
  local abi="$1"
  local target="$2"
  local host="$3"
  local api="$4"
  local build_dir="$work_dir/build-$abi"
  local stage_dir="$work_dir/stage-$abi"
  local abi_output="$resolved_output_dir/$abi"

  mkdir -p "$build_dir" "$stage_dir" "$abi_output/include"
  pushd "$build_dir" >/dev/null
  CC="$toolchain_dir/${target}${api}-clang" \
  CXX="$toolchain_dir/${target}${api}-clang++" \
  AR="$toolchain_dir/llvm-ar" \
  NM="$toolchain_dir/llvm-nm" \
  RANLIB="$toolchain_dir/llvm-ranlib" \
  STRIP="$toolchain_dir/llvm-strip" \
  CFLAGS="-O2 -DNDEBUG -ffile-prefix-map=$work_dir=/usr/src/libopus -fdebug-prefix-map=$work_dir=/usr/src/libopus -fmacro-prefix-map=$work_dir=/usr/src/libopus" \
  "$source_dir/configure" \
    --host="$host" \
    --prefix=/usr \
    --disable-maintainer-mode \
    --disable-shared \
    --enable-static \
    --disable-doc \
    --disable-extra-programs \
    --enable-silent-rules
  make --jobs="${BUILD_JOBS:-$(nproc)}"
  make DESTDIR="$stage_dir" install

  cp "$stage_dir/usr/lib/libopus.a" "$abi_output/"
  cp -a "$stage_dir/usr/include/opus/." "$abi_output/include/"
  popd >/dev/null
}

if [[ -z "${REQUESTED_ABIS// }" ]]; then
  echo "OPUS_ABIS must contain at least one supported ABI" >&2
  exit 2
fi

for abi in $REQUESTED_ABIS; do
  case "$abi" in
    armeabi-v7a)
      build_abi "$abi" armv7a-linux-androideabi arm-linux-androideabi 21
      ;;
    arm64-v8a)
      build_abi "$abi" aarch64-linux-android aarch64-linux-android 21
      ;;
    x86)
      build_abi "$abi" i686-linux-android i686-linux-android 21
      ;;
    x86_64)
      build_abi "$abi" x86_64-linux-android x86_64-linux-android 21
      ;;
    *)
      echo "Unsupported ABI in OPUS_ABIS: $abi" >&2
      exit 2
      ;;
  esac
done

cp "$source_dir/COPYING" "$resolved_output_dir/LICENSE.txt"
printf '%s\n' \
  "name=libopus" \
  "version=$OPUS_VERSION" \
  "source=$SOURCE_URL" \
  "source_sha256=$OPUS_SHA256" \
  "ndk=$REQUIRED_NDK_REVISION" \
  "min_api=21" \
  "abis=${REQUESTED_ABIS// /,}" \
  > "$resolved_output_dir/BUILD-METADATA.txt"

echo "libopus $OPUS_VERSION Android artifacts written to $resolved_output_dir"
