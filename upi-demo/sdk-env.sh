#!/usr/bin/env bash
# Sourced by the demo scripts after demo_sdk_root is set. Keep the standalone
# bank project on the same Android SDK as the checkout without copying local.properties.
demo_android_sdk=""
if [[ -f "$demo_sdk_root/local.properties" ]]; then
    demo_android_sdk="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*//p' \
        "$demo_sdk_root/local.properties" | sed 's/\\:/:/g; s/\\ / /g; s/\\\\/\\/g; s/\r$//')"
fi
demo_android_sdk="${demo_android_sdk:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}}"
if [[ -z "$demo_android_sdk" || ! -d "$demo_android_sdk" ]]; then
    echo "Android SDK not found. Set ANDROID_HOME or sdk.dir in $demo_sdk_root/local.properties." >&2
    exit 1
fi
export ANDROID_HOME="$demo_android_sdk"
export ANDROID_SDK_ROOT="$demo_android_sdk"
