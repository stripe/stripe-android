# PaymentSheet remaining first-load jank

## Standalone release follow-up

The user also reproduced the first-transition stall in the release example app.
The following captures use the minified example app outside Compose tests, on a
dedicated API 33 Google APIs Pixel 2 emulator. The user's running emulator was
not used. The playground uses its normal backend and horizontal PaymentSheet.
These observations supersede the earlier debug-test timing estimates as evidence
for the release path; they are single captures, not a benchmark distribution.

- [SQL] `/tmp/paymentsheet-release-first.perfetto-trace`: the example process
  (UPID 292, main UTID 295) has real FrameTimeline records. Its first loaded-form
  frame takes 239.94 ms, including 90.36 ms of composition, 38.71 ms of remember
  effects (including a 32.81 ms `FullSuspendCheck`), and 52.43 ms of measurement.
  The next presentation's loaded-form frame takes 172.77 ms. These frame totals
  also include rendering waits on this emulator's software graphics backend.
- [SQL] A separate cold cost follows form creation: the passive CAPTCHA warmer
  activity's `performCreate` takes 227.74 ms, including 132.08 ms in
  `WebViewChromium.init`. The second warmer creation takes 55.80 ms. Chromium's
  first initialization runs synchronously on the app's main thread.
- [INSPECTED] The sideloaded release was initially `status=verify, reason=install`.
  Installing its bundled profile with ProfileInstaller and compiling with
  `cmd package compile -m speed-profile -f` produced a verified
  `status=speed-profile, reason=cmdline` state before a fresh-process capture.
- [SQL] `/tmp/paymentsheet-release-profile-before.perfetto-trace`: the compiled
  build still has a 241.79 ms warmer `performCreate` (slice 17790, main UTID 307),
  including 170.44 ms of `WebViewChromium.init` (slice 18821). Form composition
  is 22.13 ms in this run. Compilation does not remove the Chromium startup
  operation. Differences between runs also include JIT, caches, and host load.

The production change awaits `WebViewCompat.startUpWebView` before CAPTCHA setup.
Its worker executor moves supported startup work off Main; cancellation ignores
late callbacks, and worker initialization exceptions enter the existing CAPTCHA
failure path. Both warmup and confirmation use this path. Token caching and the
passive challenge remain enabled. Payments-core now uses the existing WebKit
1.15 dependency. The isolated wrapper opts into its experimental startup API;
WebKit 1.16's stable replacement raises the minimum Android SDK to 24, while this
SDK supports 23. No WebView feature probe runs before asynchronous startup.

- [SQL] `/tmp/paymentsheet-release-profile-after.perfetto-trace`: after installing
  the changed APK and again verifying `status=speed-profile`, warmer creation
  takes 2.70 ms (slice 19617, main UTID 270). Provider initialization takes
  21.31 ms on a `DefaultDispatch` worker (slice 21005, UTID 1782). The subsequent
  `WebViewChromium.init` takes 20.16 ms (slice 24953).
- [SQL] The emulator's older WebView still runs
  `WebViewChromiumAwInit.startChromiumLocked` for 119.68 ms on Main (slice 23179).
  The change does not eliminate this provider-dependent native startup stall.
  This is not evidence that the opening transition is now entirely smooth.
- [VALIDATED] All 34 selected CAPTCHA-service and warmer unit tests pass,
  including delayed startup, confirmation during warmup, token reuse, and startup
  failure. Payments-core detekt and the minified example release build pass.

- [SQL] In the changed release capture, the first 1.2 seconds after
  PaymentSheet activity creation contain 45 submitted PaymentSheet surface-frame
  records; 10 have `App Deadline Missed` in their FrameTimeline classification.
  There is a 124.70 ms gap between surface-frame start timestamps around the
  119.68 ms Chromium main-thread task. These are emulator observations, not a
  count of every skipped display refresh or a hardware benchmark. Software
  graphics also produces prediction errors and buffer stuffing.
- [VALIDATED WITH LIMITATION] The focused managed-device CAPTCHA confirmation
  check passes `ApiConfigurationOnly` but fails `PaymentConfigurationOnly`:
  `js.hcaptcha.com` returns `net::ERR_NAME_NOT_RESOLVED`, leaving no token for
  the expected confirmation request. Two async runs and a controlled run with
  the startup hook temporarily bypassed have the same result. The async source
  was restored exactly after comparison. This DNS failure also occurs on the
  previous synchronous startup path; test infrastructure was left unchanged.
  Logs: `/tmp/paymentsheet-async-webview-token-device-rerun.log` and
  `/tmp/paymentsheet-webview-baseline-token-device.log`.

Reproduce the release attribution query in a named trace-processor session:

```sql
INCLUDE PERFETTO MODULE slices.with_context;
SELECT s.utid, s.thread_name, s.id, s.ts, s.dur / 1e6 AS ms, s.name
FROM thread_or_process_slice s
WHERE s.process_name = 'com.stripe.android.paymentsheet.example'
  AND (s.name GLOB '*performCreate*' OR s.name GLOB '*WebView*'
       OR s.name = 'Compose:recompose' OR s.name = 'AndroidOwner:onMeasure')
  AND s.dur > 3000000
ORDER BY s.ts;
```

FrameTimeline query for the stated 1.2-second window (same after trace):

```sql
SELECT COUNT(*) AS submitted_surface_frames,
       SUM(CASE WHEN f.jank_type GLOB '*App Deadline Missed*' THEN 1 ELSE 0 END)
           AS app_deadline_missed_frames
FROM actual_frame_timeline_slice f
JOIN process p ON p.upid = f.upid
WHERE p.name = 'com.stripe.android.paymentsheet.example'
  AND f.layer_name GLOB '*PaymentSheetActivity#*'
  AND f.ts BETWEEN 754460334984 AND 755660334984;
```

Further measurement should use the user's device/configuration and current
WebView provider. A complete removal of the old provider's remaining native UI
task would require a different warmup policy or provider behavior; this change
does not disable the challenge or move WebView construction to a worker thread.

## Earlier debug instrumentation investigation

The earlier controller and dependency changes did not establish smooth first-load rendering. The follow-up capture confirms a large remaining main-thread cold-start stall.

## Evidence

Captured with the production fixes applied, the existing Terminal/Mockito infrastructure unchanged, and temporary trace markers. All recording hooks were restored afterward. All four card-payment test variants passed.

Trace: `/tmp/paymentsheet-current-system.perfetto-trace`

Method samples (separate run): `/tmp/paymentsheet-current-methods-output/managed_device_android_test_additional_output/debug/pixel2api33ime/current-1792681916817.trace`

- [SQL] Target process `com.stripe.android.paymentsheet.test`, UPID 250, main UTID 251. First Choreographer frame: timestamp 56645162317 ns, duration 126.666 ms. Later presentations: 25.98, 24.40, and 14.09 ms.
- [SQL] 1,526 `VerifyClass` slices cover 93.71 ms within that first frame, approximately 74% of it. Main runs on CPU for 104.87 ms. The class verification intervals overlap containing composition scopes and must not be added to those scopes.
- [SQL] First activity creation: 62.52 ms, including 43.96 ms of class verification. Post-load initialization: 23.56 ms, including 14.21 ms of verification. Initial card-form creation: 14.30 ms.
- [SQL] Loaded-form recomposition: 133.05 ms, including a 113.39 ms Compose composition; layout afterward: 42.35 ms. These are driven by the Compose test clock outside Choreographer and cannot establish production frame deadlines.
- [INSPECTED] The debug instrumentation APK contains no baseline profile assets. Compose UI 1.10.6 ships a 9,695-byte baseline-prof.txt in its AAR. Before this investigation, this repository had no Stripe-specific baseline profiles.
- [SAMPLED] A separate current ART trace records one initial formElementsForCode call (14.64 ms) and one address-registry get (2.64 ms), consistent with removal of duplicate form creation. Three StripeApiRepository constructions total about 6.86 ms inclusive during activity setup; their DI scopes need review before changing them. These sampled timings are attribution evidence, not frame measurements.
- [INFERRED] Cold runtime verification and UI initialization are the strongest remaining explanation for this test first-load stall. The previous changes reduce form work but leave this separate cold UI path.

## Follow-up production changes

The user confirmed the visible symptom is in the debug example application, with release performance the priority. The trace above is from debug instrumentation, not that application, so its timings establish attribution without measuring the reported example-app frames.

Further changes defer the saved-payment repository backends until an operation needs them and defer the Link analytics subcomponent until its first event. Targeted, manually curated library baseline profiles now cover the initial activity, form, and field UI paths in PaymentSheet and its UI modules. These are release compilation inputs, not debug warmup. Maintenance and release-validation guidance is in `paymentsheet/PERFORMANCE.md`.

The added lazy-dependency changes pass 186 focused unit tests and PaymentSheet detekt. The three release AARs and the minified example release APK build successfully. All 44 profile class families match their owning module's classes, and each AAR contains its source profile. The release APK contains compiled `baseline.prof` (10,467 bytes) and `baseline.profm` (1,225 bytes). There is no measured release speedup yet.

Final managed-device checks pass all four card-payment variants and all four Link-signup variants on `pixel2api33`. Test infrastructure remains unchanged. R8's output profile retains the activity and ViewModel rules through minification.

## Loading-to-horizontal-screen follow-up

Source inspection found additional work on initial form presentation:

- A normal dropdown used a disabled `BasicTextField` through `CompatTextField` to
  display its value. It now uses static text inside the same decoration layout,
  avoiding an embedded editor. The dropdown also subscribed to its selected-index
  flow twice; it now uses one subscription.
- Row-field size callbacks wrote a height read during composition, invalidating
  the row after its first measurement. Divider height is now read during layout,
  keeping that size adjustment out of composition and using local pixel units.
- The debug `collectAsState` helper captured an `AssertionError` stack for every
  new subscription. Text fields each have many subscriptions. Stack capture now
  happens only when the off-main-thread assertion fails. The error includes the
  flow class; it no longer retains the original composition-site stack.

Existing dropdown snapshots (5 cases) and horizontal first-payment-screen
snapshots (6 cases) match without updating baselines. All four card-payment
managed-device variants pass. A new dropdown regression checks the absence of an
embedded editor and selection through the menu, alongside the existing autofill
coverage. All 18 focused dropdown/controller/StateFlow Compose tests and
stripe-ui-core detekt pass. The minified example release build also succeeds.

Fresh system trace: `/tmp/paymentsheet-horizontal-system.perfetto-trace`.
The managed test process is UPID 4, main UTID 52. Initial loaded-form composition
is 125.04 ms (slice 7714, including 57.47 ms of verification), with a 59.16 ms
measure/layout afterward (slice 10102). Subsequent loaded-form compositions are
33.11 and 25.44 ms, with 31.39 and 19.98 ms measure/layout respectively. The
initial Choreographer frame remains 129.13 ms, including 92.14 ms of verification.
These durations do not establish an improvement over the prior run: debug timing
varies, and the test clock drives the loaded-form scopes outside Choreographer.
There are no target FrameTimeline rows and 44 negative-timestamp drops. A release
frame-time comparison is still outstanding.

## Frame-count check and form-subscription follow-up

An explicit FrameTimeline capture, `/tmp/paymentsheet-frame-deadlines.perfetto-trace`,
contains 727 actual and 508 expected frame records for system rendering, but none
for the managed PaymentSheet test process (UPID 250, main UTID 255). The previous
short-option recording configuration also requested FrameTimeline; missing app
rows were not caused by forgetting to enable the data source. This capture
cannot give an exact skipped-frame count for the example application.

The system animation interval is 16.667 ms (60 Hz). The first PaymentSheet
Choreographer callback lasts 117.28 ms (slice 5661), compared with 129.13 ms in
the prior capture. Each occupies about 7–8 refresh periods, equivalent to roughly
7 missed refresh opportunities if evaluated against a one-period budget. This
is a duration-based estimate, not a count of missed presentations. The loaded
form scopes still run under the Compose test clock and must not be converted
into an exact skipped-frame total.

Further source inspection found that FormViewModel collected the complete field
graph twice: once for values and once for save-for-future-use preference. The
preference is now derived from the same unfiltered snapshot as the field values,
before hidden-field filtering and defaults. This removes the duplicate graph
subscription and avoids temporarily pairing values with a preference from a
different update. Regressions cover a single upstream subscription and save
preference updates, including hidden and absent save fields. No frame-time
speedup has been established for this change.

Validation passed: 111 focused unit tests, PaymentSheet detekt, and all four
managed-device card-flow tests. The managed test task was explicitly rerun.

## Measurement limits

The target has no FrameTimeline rows in this ATD capture, so an exact missed-frame count cannot be claimed. The trace reports 69 dropped negative timestamps. Method sampling perturbs timing and reports some stack-depth truncation.

A diagnostic request to compile the already-running managed test package with `cmd package compile -m speed -f` returned Success, but package state remained `status=verify`, `reason=cmdline`. The corresponding second system capture was not retrieved before managed-device shutdown. This is not a valid release/AOT comparison and provides no evidence of improvement.

A production release launch with frame timelines remains necessary to quantify the profile benefit and assess any remaining frame stalls. The debug example can still visibly pause while ART verifies cold classes and executes unoptimized Compose code; the new profiles do not eliminate that debug cost.

## Reproduce the validated SQL

Load with `/tmp/paymentsheet-trace-processor server unix --name ps-current --daemonize /tmp/paymentsheet-current-system.perfetto-trace`, then execute the following through `query --remote ps-current`.

```sql
SELECT s.id, s.ts, s.dur / 1e6 AS ms, s.name, t.utid, p.upid
FROM slice s
JOIN thread_track tt ON tt.id = s.track_id
JOIN thread t ON t.utid = tt.utid
LEFT JOIN process p ON p.upid = t.upid
WHERE s.name GLOB 'PS:*'
ORDER BY s.ts;

SELECT s.id, s.ts, s.dur / 1e6 AS ms, s.name
FROM slice s
JOIN thread_track tt ON tt.id = s.track_id
WHERE tt.utid = 251
  AND s.dur > 16000000
  AND s.ts BETWEEN 56500000000 AND 58000000000
ORDER BY s.ts;

SELECT s.name, COUNT(*) AS n, ROUND(SUM(s.dur) / 1e6, 2) AS ms
FROM descendant_slice(8201) s
WHERE s.dur > 1000000
GROUP BY s.name
ORDER BY SUM(s.dur) DESC;

SELECT f.upid, COUNT(*) AS n
FROM actual_frame_timeline_slice f
GROUP BY f.upid;
```

Full system triage and CPU/verification interval calculations: `/tmp/ps-current-triage.md`.
