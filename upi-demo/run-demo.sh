#!/usr/bin/env bash
set -euo pipefail

if [[ $# -gt 1 ]]; then
    echo "Usage: $0 [DEVICE_SERIAL]" >&2
    exit 1
fi

demo_sdk_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
demo_banks_root="${UPI_DEMO_BANKS_DIR:-/Users/tjclawson/stripe/upi-demo-banks}"
demo_adb=("${UPI_DEMO_ADB:-/Users/tjclawson/Library/Android/sdk/platform-tools/adb}")
if [[ $# -eq 1 ]]; then
    demo_adb+=(-s "$1")
fi

if [[ ! -x "$demo_banks_root/gradlew" ]]; then
    echo "Demo bank sources not found at $demo_banks_root. Set UPI_DEMO_BANKS_DIR to their location." >&2
    exit 1
fi

"${demo_adb[@]}" get-state
(cd "$demo_sdk_root" && ./gradlew :upi-demo:assembleDebug)
UPI_DEMO_SDK_WORKTREE="$demo_sdk_root" "$demo_banks_root/gradlew" \
    :app:assembleBlueDebug :app:assembleGreenDebug :app:assembleOrangeDebug

"${demo_adb[@]}" install -r "$demo_sdk_root/upi-demo/build/outputs/apk/debug/upi-demo-debug.apk"
for demo_bank in blue green orange; do
    "${demo_adb[@]}" install -r "$demo_banks_root/app/build/outputs/apk/$demo_bank/debug/app-$demo_bank-debug.apk"
done
"${demo_adb[@]}" shell am start -n com.stripe.android.upidemo/.DemoActivity
