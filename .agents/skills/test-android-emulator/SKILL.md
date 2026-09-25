---
name: test-android-emulator
description: Manually drive and verify stripe-android app flows on a connected Android emulator or device with adb and UiAutomator hierarchy dumps. Use for interactive playground checks, bounds-based taps, safe test-data entry, navigation, screenshots, and evidence-backed state transitions when an automated Compose or instrumentation test is not the requested proof.
---

# Test Android Emulator

Use `adb` and the rendered UI hierarchy to perform a reproducible manual flow. Treat each hierarchy dump as a snapshot: act on it once, then inspect the new state before continuing.

Do not use this workflow as a substitute for writing or running automated tests. Invoke the matching Compose, instrumentation, screenshot, or flake skill when the task is about test code or repeated automated execution.

## Establish the target

1. State the app package, behavior under test, expected initial state, and expected terminal state.
2. List connected targets:

   ```bash
   adb devices -l
   ```

3. Select one serial explicitly and use `-s` on every command:

   ```bash
   device_serial=emulator-5554
   adb -s "$device_serial" shell getprop ro.build.version.sdk
   ```

4. Stop when multiple targets exist and the intended one cannot be resolved from the task. Never rely on adb's implicit default target.

If adb reports `ADB server didn't ACK`, `could not install *smartsocket* listener`, or `Operation not permitted`, rerun it in the host-capable execution context after requesting approval. Treat this as a sandbox socket failure, not evidence that the emulator is offline.

Do not launch, wipe, restart, or delete an emulator unless the task explicitly requires it. Prefer the already-connected target.

## Inspect before acting

Dump the current hierarchy and make it searchable:

```bash
adb -s "$device_serial" shell uiautomator dump /sdcard/window.xml >/dev/null
adb -s "$device_serial" shell cat /sdcard/window.xml \
  | sed 's/></>\n</g' \
  | rg 'text=|content-desc=|resource-id=|clickable=|enabled=|bounds='
```

Narrow the final `rg` expression to stable text, content descriptions, or resource IDs for the current step. Record the observed state before the first action.

For Compose screens, expect the visible text node to be non-clickable. Locate its nearest clickable parent and use that parent's `bounds`. Prefer targets in this order:

1. Stable `resource-id`.
2. Stable `content-desc`.
3. Exact visible `text`.
4. Bounds only after the semantic target is identified.

Do not use coordinates copied from a different device, orientation, screen, or stale hierarchy.

## Tap from current bounds

For a node with `bounds="[83,1821][604,1851]"`, use the midpoint of the clickable bounds:

```bash
adb -s "$device_serial" shell input tap 343 1836
```

After every tap that can navigate, open a sheet, select a menu item, or submit data:

1. Allow only the short transition time the UI needs.
2. Dump the hierarchy again.
3. Verify the expected new state before the next action.

Use bounded polling for asynchronous transitions. Do not use a long fixed sleep and infer success from elapsed time.

If the target is offscreen, confirm that the hierarchy exposes a scrollable container, then swipe within that container and dump again:

```bash
adb -s "$device_serial" shell input swipe 540 1700 540 700 400
```

Adjust swipe coordinates to the current viewport. Never assume a page is scrollable from visual layout alone.

## Enter only safe test data

Tap an empty field, enter one value, then inspect the field before continuing:

```bash
adb -s "$device_serial" shell input tap 300 760
adb -s "$device_serial" shell input text '4242424242424242'
```

Encode spaces as `%s`, not `%20`:

```bash
adb -s "$device_serial" shell input text '510%sTownsend%sSt'
```

For a formatted value, verify the result on the target device. This expiration example produced `12/34` in the Checkout test flow:

```bash
adb -s "$device_serial" shell input text '12%2F34'
```

Important constraints:

- Use documented test fixtures only. Never type credentials, live card data, access tokens, client secrets, or customer PII through adb.
- `input text` appends at the current cursor. Do not assume it clears an existing field.
- Avoid unreliable bulk clearing with long-press delete. Relaunch the form or clear the field through its UI when the current value is uncertain.
- The IME can resize the viewport and invalidate coordinates. Dump again after focusing a field.
- Compose semantics may expose formatted values in `content-desc` while `text` remains empty. Check both before concluding that entry failed.

Use named key events for navigation and IME actions:

```bash
adb -s "$device_serial" shell input keyevent KEYCODE_BACK
adb -s "$device_serial" shell input keyevent KEYCODE_ENTER
```

Verify the result immediately. `KEYCODE_BACK` can dismiss the IME, close a sheet, or navigate away depending on current focus.

## Launch and reset narrowly

Prefer navigating from the app's real entry point. When a direct component is known and the task requires a clean launch, use the explicit package and activity:

```bash
adb -s "$device_serial" shell am start -n com.example.app/.MainActivity
```

Use `am force-stop <package>` only when restarting the app process is part of the test plan, and record that state reset. Do not guess activity names.

Require explicit user authorization before any of these actions:

- `pm clear`
- uninstalling an app
- deleting app files
- wiping or deleting an AVD
- stopping unrelated processes
- changing global device settings

If storage prevents installation, report the exact failure and resolve only the scoped app or cache after approval. Do not clean unrelated emulator data.

## Capture evidence

Create a disposable evidence directory and capture both the visible screen and semantic state when useful:

```bash
evidence_dir=$(mktemp -d /private/tmp/android-emulator-evidence.XXXXXX)
adb -s "$device_serial" exec-out screencap -p > "$evidence_dir/completed.png"
adb -s "$device_serial" shell uiautomator dump /sdcard/window.xml >/dev/null
adb -s "$device_serial" pull /sdcard/window.xml "$evidence_dir/completed.xml"
```

Keep evidence free of credentials and customer data. Store temporary artifacts outside the repository unless the user explicitly requests committed test assets.

For each manual check, record:

- Device serial, model, API level, and app variant.
- Exact initial state.
- Actions taken and the semantic target for each action.
- Exact terminal UI state.
- Session or request identifier only when it is safe and needed for an independent backend check.
- Any external verification, such as Admin persistence, as a separate evidence source.

UI hierarchy evidence proves only what the app displayed. It does not prove backend persistence, network payload contents, or automated regression coverage. Verify those surfaces independently and keep unchecked any claim that was not directly observed.

## Finish safely

1. Dump and capture the final state.
2. Confirm the app did not exit unexpectedly and required actions are enabled or disabled as intended.
3. Report canceled, failed, or partial results honestly and preserve retryability when that is part of the behavior.
4. Leave the emulator in a stable state. Do not clear data or tear down the target unless requested.
5. Report exact observed evidence, not a broad claim inferred from one intermediate screen.
