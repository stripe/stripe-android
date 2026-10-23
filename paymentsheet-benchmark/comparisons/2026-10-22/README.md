# Independent PaymentSheet first-presentation comparisons

These measurements compare each of the 16 production fixes independently with
unchanged production sources, using the same release Macrobenchmark harness.
They are exploratory **managed-emulator measurements**, not representative device
performance estimates. Positive reductions mean faster; negative reductions mean
slower. An optimization's expected effect is not substituted for its measured
result.

**Result:** none of the 16 initial trials resolves a positive improvement in
load-to-first-draw, load-to-first-presented-frame, or presentation-to-first-presented-frame:
every positive median reduction has a descriptive interval crossing zero.
This does not establish that the fixes have no benefit. The emulator and block
order do not support reliable per-fix millisecond savings or a performance ranking.
ID 14's initial apparent slowdown does not persist in a reverse-order repeat.
For ID 16, all 20 traces place WebView startup after the first loaded draw, so
that first-frame endpoint cannot measure its later startup stall in this workload.

There are **320 initial measured cold launches (16 × 2 × 10), plus 20 launches
for the targeted ID 14 repeat**. All 34 managed benchmark invocations passed.
No production fixes are applied by this report or the harness PR.

**Follow-up:** [Fresh reverse-order verification of IDs 2, 4, and 16](../2026-10-23/README.md)
adds 60 cold launches with build cooldowns and archived APKs. Keep the new
observations separate from these original selected estimates.

## Workload and boundaries

Each arm runs ten fresh target processes of a separately installed, minified,
non-debuggable, profileable app. It presents horizontal PaymentSheet for a
fixture-backed, card-only PaymentIntent with a full billing address. The target
package is `com.stripe.android.paymentsheet.benchmarkapp`. The instrumentation
runner is a different APK. Each arm runs on the managed Pixel 2 / API 33 Google
APIs image; the developer's emulator is not used. A fresh process is not a fresh
installation or cleared app data on every iteration.

- IDs 1 and 3–15: full address plus name, email, and phone collection (`full`).
- ID 2: full address with name, email, and phone hidden (`hidden`).
- ID 16: full contact/address form plus passive CAPTCHA startup (`captcha`). SDK
  requests use fixtures; remote CAPTCHA JavaScript does not. This measures
  presentation during startup, not CAPTCHA token readiness or confirmation.

`loadToFirstDrawMs` starts immediately after successful `PaymentElementLoader.load`
return on the worker and ends at completion of the loaded root Compose draw
callback. It includes the transition from the loader result to form UI. It is
CPU draw recording, not display presentation or completed entrance animation.
For ID 6, the marker remains immediately at loader return, before its enclosing
scope finishes waiting for country warmup.

`loadToFirstPresentedFrameMs` ends at the actual FrameTimeline surface-frame end
associated with that draw's main-thread Choreographer token. A dropped submission
falls forward to the first subsequent presented frame on the same PaymentSheet
layer. `firstPresentedFrameOverrunMs` compares that surface frame with its
expected deadline. Android FrameTimeline is the source; this is not a camera
measurement of emitted pixels. Missing markers/frame matches fail the test.

The three `presentTo…` metrics start immediately before PaymentSheet builder and
presentation in the benchmark activity. They include loader dependency creation
and loading, and exclude initial application/benchmark Activity startup. They
expose work moved before loader return. Differences between medians of separate
phases need not sum to the median difference of a total.

## Comparison protocol

All arms use `coldWithBaselineProfile`, Macrobenchmark 1.5.0,
`CompilationMode.Partial(BaselineProfileMode.Require)`, and ten cold iterations.
Macrobenchmark controls profile installation/compilation and process termination.
The profile fix adds Stripe profiles to the dependency profiles present in the
baseline; both arms use the same compilation mode. The raw JSON context records
`compilationMode: run-from-apk`; the target's
explicit requested policy is recorded separately in each arm's metadata and
set by `FirstPresentationBenchmark`. Both fields are preserved.

Common source revision: `008a103e53` (benchmark harness and trace markers; none of
the 16 production fixes). Production patches were extracted from combined
implementation `17ddb3e1bc28442139dad42a4d513de109bec2ec`. Each trial restores the
common revision, applies only its numbered production patch, and builds the
release target. Constructor/test changes outside production sources are not part
of this timing experiment. ID 6's overlapping ViewModel edit preserves the common
trace-marker boundary. Patches record the exact measured source changes.

Each initial comparison has a fresh adjacent baseline: baseline ten iterations, then isolated
fix ten iterations. Runs/builds are serial. Baseline source and APK identity are
checked using the preserved metadata and patch hashes. Builds precede runs, and
fix builds generally perform more compilation than cached baseline builds. The initial trials
have no standardized host cooldown or randomized/counterbalanced arm order; host
load, build heat, emulator scheduling, OS caches, and drift can confound a delta.
An adjacent baseline reduces neither all confounding nor all variance.

The intervals below are descriptive independent bootstrap percentile intervals
for **baseline median minus fix median**: 10,000 resamples of size ten per arm,
Python `random.Random(160016)`, sorted bootstrap differences at indices 249 and
9749. They do not pair unrelated iterations, adjust for 16 comparisons, model
host drift, or establish causality. A narrow interval on this emulator would
still need repeated, counterbalanced physical-device validation. Do not sum
individual deltas; several fixes remove overlapping work.

## Load complete → first draw

All times are milliseconds. Intervals describe the median reduction, not the
range of individual launches. Individual samples and min/median/max are in
[samples.csv](samples.csv), [results.json](results.json), and [raw/](raw/).

| ID | Isolated change | Baseline median | Fix median | Reduction [95% interval] |
| --- | --- | --- | --- | --- |
| 1 | Lazy country address controllers | 111.06 | 165.44 | -54.38 [-103.87, +0.77] |
| 2 | Hidden contact/autocomplete controllers | 139.87 | 158.02 | -18.15 [-68.05, +27.31] |
| 3 | Skip duplicate initial form | 130.42 | 170.44 | -40.03 [-84.52, +13.86] |
| 4 | Classify without disposable forms | 187.06 | 132.18 | +54.88 [-24.25, +96.55] |
| 5 | Resolve loader on worker | 157.29 | 153.23 | +4.06 [-63.29, +44.15] |
| 6 | Warm countries + atomic cache | 132.37 | 129.91 | +2.46 [-100.73, +25.75] |
| 7 | Defer analytics dependencies/requests | 165.46 | 132.03 | +33.43 [-22.28, +85.55] |
| 8 | Lazy saved-method backends | 169.93 | 163.06 | +6.87 [-25.33, +110.00] |
| 9 | Lazy inline Link analytics | 132.27 | 145.40 | -13.13 [-80.77, +113.33] |
| 10 | Scoped baseline profiles | 132.41 | 127.53 | +4.88 [-58.96, +54.07] |
| 11 | Static dropdown display | 136.81 | 150.53 | -13.72 [-53.30, +76.70] |
| 12 | Single dropdown subscription | 135.67 | 132.96 | +2.72 [-46.37, +40.59] |
| 13 | Divider height in layout | 130.14 | 119.91 | +10.23 [-22.85, +74.31] |
| 14 | Assertion stack on failure | 131.58 | 204.53 | -72.96 [-146.31, -22.95] |
| 15 | Single form-value snapshot | 149.24 | 191.33 | -42.08 [-150.24, +36.18] |
| 16 | Await async WebView startup | 116.00 | 170.09 | -54.09 [-89.95, +9.27] |

## Surface presentation and total presentation latency

The first pair is load return → presented surface frame. The second is
PaymentSheet builder/presentation start → that frame; it includes loading.
Each cell shows baseline / fix medians followed by reduction [95% interval].

| ID | Load → presented frame | Presentation start → presented frame |
| --- | --- | --- |
| 1 | 181.19 / 245.13; -63.94 [-157.98, +46.76] | 640.05 / 555.44; +84.61 [-353.45, +245.66] |
| 2 | 221.95 / 220.69; +1.26 [-67.56, +51.98] | 769.80 / 598.72; +171.08 [-111.29, +332.50] |
| 3 | 191.60 / 233.24; -41.64 [-179.37, +38.11] | 668.85 / 991.42; -322.57 [-544.82, +40.30] |
| 4 | 229.32 / 188.99; +40.33 [-33.79, +76.94] | 617.01 / 628.90; -11.90 [-151.76, +225.60] |
| 5 | 227.69 / 202.62; +25.08 [-51.31, +79.06] | 597.36 / 613.80; -16.44 [-176.29, +113.18] |
| 6 | 205.75 / 208.45; -2.70 [-109.08, +50.53] | 603.69 / 697.34; -93.65 [-220.56, +54.67] |
| 7 | 255.00 / 212.47; +42.52 [-5.73, +131.56] | 711.88 / 694.78; +17.10 [-305.06, +435.24] |
| 8 | 227.22 / 246.35; -19.12 [-108.37, +163.82] | 607.09 / 653.83; -46.74 [-296.91, +301.21] |
| 9 | 216.75 / 257.84; -41.10 [-100.49, +113.59] | 693.44 / 785.68; -92.24 [-536.83, +99.02] |
| 10 | 215.19 / 205.26; +9.93 [-65.36, +45.85] | 633.76 / 648.18; -14.42 [-191.77, +157.12] |
| 11 | 222.93 / 244.62; -21.69 [-95.88, +103.43] | 682.86 / 661.17; +21.69 [-275.10, +197.73] |
| 12 | 205.14 / 192.87; +12.26 [-38.28, +74.11] | 654.09 / 661.58; -7.49 [-183.08, +308.49] |
| 13 | 237.18 / 191.66; +45.52 [-30.24, +101.12] | 621.58 / 625.35; -3.78 [-136.92, +287.05] |
| 14 | 210.64 / 287.22; -76.57 [-198.17, +20.90] | 636.49 / 740.82; -104.34 [-379.77, +115.50] |
| 15 | 241.91 / 277.00; -35.09 [-191.16, +16.45] | 783.07 / 808.58; -25.51 [-762.08, +123.45] |
| 16 | 219.50 / 235.54; -16.04 [-99.27, +64.12] | 664.79 / 766.83; -102.04 [-313.39, +64.34] |

## Frame deadline observations

The first metric is the deadline overrun of the first presented loaded frame
(baseline / fix medians). The second is the standard FrameTimingMetric P90 overrun
across the entire measured journey. Its sample count and duration vary with
loading and animation; it is not a fixed post-load window. These positive
overruns show that late frames remain. They are not counts of skipped refreshes.

| ID | First loaded frame overrun, ms | Journey frame overrun P90, ms |
| --- | --- | --- |
| 1 | 141.02 / 205.29 | 100.32 / 147.08 |
| 2 | 201.38 / 186.59 | 101.65 / 101.30 |
| 3 | 153.41 / 195.88 | 124.87 / 193.15 |
| 4 | 179.13 / 145.57 | 116.47 / 98.86 |
| 5 | 192.25 / 170.28 | 104.56 / 131.46 |
| 6 | 165.01 / 161.63 | 100.55 / 122.86 |
| 7 | 192.06 / 180.22 | 161.47 / 146.02 |
| 8 | 187.09 / 176.51 | 115.34 / 128.20 |
| 9 | 173.82 / 221.42 | 134.75 / 170.23 |
| 10 | 160.79 / 172.71 | 135.23 / 103.40 |
| 11 | 182.89 / 201.69 | 121.04 / 115.57 |
| 12 | 159.87 / 170.00 | 105.76 / 96.48 |
| 13 | 179.12 / 169.71 | 110.66 / 102.86 |
| 14 | 165.34 / 232.66 | 100.88 / 151.98 |
| 15 | 202.76 / 229.68 | 150.99 / 204.94 |
| 16 | 196.37 / 196.60 | 171.50 / 168.97 |

## Targeted reverse-order repeat of ID 14

The initial load-to-draw reduction was −72.96 ms [−146.31, −22.95], the only
initial first-draw interval excluding zero, in the slower direction. To check
that observation, the repeat prebuilt each app/runner, waited 60 seconds, and
then measured **fix first, baseline second**. Both arms used the same workload,
compilation policy, and ten fresh processes. This adds 20 launches.

| Metric | Baseline / fix; reduction [95% interval], ms |
| --- | --- |
| `loadToFirstDrawMs` | 149.41 / 118.40; +31.01 [-30.75, +93.34] |
| `loadToFirstPresentedFrameMs` | 226.50 / 177.15; +49.35 [-46.59, +123.45] |
| `presentToFirstPresentedFrameMs` | 642.60 / 652.94; -10.33 [-256.42, +169.82] |

The same source patch has identical SHA256 in both fix blocks, but fresh builds
produced different APK hashes in both repeat arms. Thus this is a repeat of the
source comparison, **not an exact-binary replication**. Keep it separate from the
initial trial; do not pool it to manufacture a favorable result. It does not
reproduce the initial slowdown, and it does not resolve a positive improvement.
The anticipated assertion-stack benefit is primarily debug-only; no debug
performance run was made. See [repeat-protocol.json](repeat-protocol.json) and
the `14-repeat-*` raw data/manifest entries.

## CAPTCHA startup coverage and remaining main-thread work

Both arms used `com.google.android.webview` **109.0.5414.123**, code `541412334`,
as recorded by WebViewFactory logcat (without a pre-startup feature probe).
Perfetto v58.2 extracted the following scopes from all ten traces per arm.
These are scope durations, not additional independent end-to-end savings.
Some scopes nest or move into separate tasks; do not sum their differences.

| Scope | Baseline min / median / max, ms | Fix min / median / max, ms | Thread observation |
| --- | --- | --- | --- |
| Warmer Activity creation | 161.25 / 169.45 / 299.01 | 1.43 / 4.08 / 97.61 | Main in 10/10 of each arm |
| Provider initialize | 3.85 / 6.18 / 13.98 | 3.75 / 19.25 / 84.08 | Main 10/10 baseline; worker 10/10 fix |
| Native Chromium startup | 60.30 / 79.99 / 144.04 | 82.43 / 132.18 / 156.62 | Main in 10/10 of each arm |
| WebView init | 81.16 / 106.35 / 200.37 | 19.89 / 39.46 / 52.37 | Main in 10/10 of each arm |

All four scopes begin **after the first loaded draw in all 20 traces**. The
Activity creation median falls from 169.45 ms to 4.08 ms, while native startup
still occupies Main in a later task (median 132.18 ms with the fix). This is a
structural/timing change in startup, not evidence that total main-thread work
or first-frame latency improved. The standard journey-frame P90 overrun is
171.50 ms baseline versus 168.97 ms fix; that small aggregate difference has no
isolated confidence estimate here. Presentation remains janky on this emulator.

[SQL](captcha-startup.sql) and [all extracted slices](captcha-startup-slices.csv)
record thread IDs, offsets from loader return, and the first draw offset. The
first traces are `FirstPresentationBenchmark_coldWithBaselineProfile_iter000_2026-10-22-20-03-34.perfetto-trace`
and `FirstPresentationBenchmark_coldWithBaselineProfile_iter000_2026-10-22-20-06-59.perfetto-trace`;
the archive manifest identifies every trace and its hash. CAPTCHA JavaScript
uses the real network; no token-forwarding or confirmation success is asserted.

## Coverage by fix

| IDs | What this trial covers and excludes |
| --- | --- |
| 1 | Initial country address-controller construction in a full address form; country-switch latency is not measured. |
| 2 | Hidden contact fields with full address; visible/optional contact and autocomplete variants are not compared. |
| 3–4 | Initial card form creation/classification; subsequent payment-method changes are not measured. |
| 5–6 | Loading and first form publication; cold in-process country cache, current default locale, no concurrent locale-switch stress test. |
| 7 | Presentation analytics dependency deferral. The fixture has no experiment-exposure events, so V2 request construction is not exercised. |
| 8 | Avoiding unused saved-method backend graphs; no saved-method operation or customer-variant comparison. |
| 9 | Avoiding unused inline Link analytics construction; no Link confirmation branch. |
| 10 | Added curated profiles with profiles required in both arms. Baseline APK profile sizes are 6,869/822 bytes (`prof`/`profm`), versus 8,092/999 with the fix. No generated-profile or representative-hardware speedup claim. |
| 11–13 | Dropdowns and multi-field rows on initial full-form presentation; no controller replacement, density sweep, or allocation/recomposition-count instrumentation. |
| 14 | Release presentation only. The primary debug stack-allocation benefit remains unmeasured. |
| 15 | Initial form-value graph and first draw; repeated field-edit latency is not measured. |
| 16 | Cold in-process WebView/CAPTCHA initialization during presentation, with the important later-than-first-draw limitation above. No token readiness/confirmation test. |

These are performance trials, not full functional regression suites for each
isolated fix. Existing fixtures, clocks, synchronization, and test setup remain
unchanged. The main remaining measured CAPTCHA problem is native Chromium on
Main; changing warmup policy or WebView provider is outside these patches.

## Reproduce and inspect

Environment: Google APIs ARM64 API 33 image,
`google/sdk_gphone64_arm64/emu64a:13/TE1A.240213.009/12342917:userdebug/dev-keys`,
two virtual CPU cores, ART mainline `331711080`. Emulator errors alone are
explicitly suppressed; the target remains release/minified. Device/server
network state is not controlled for CAPTCHA. Validate performance conclusions
on representative physical devices with consistent compilation/installation,
recorded WebView version, standardized cooldown, and counterbalanced order.

Use a dedicated clean worktree at `008a103e5301811cebd29a84a99e95684d62c606`.
For a fix arm, apply only [its production patch](patches/); for baseline, retain
that clean revision. New controllers are not shared between apps or iterations.
Run the following separately for each arm (choose `hidden` for ID 2, `captcha`
for ID 16, otherwise `full`):

```sh
./gradlew :paymentsheet-benchmark:pixel2api33DebugAndroidTest --rerun \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR \
  -Pandroid.testInstrumentationRunnerArguments.class=com.stripe.android.paymentsheet.benchmark.FirstPresentationBenchmark#coldWithBaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.paymentsheet.workload=full
```

Preserve each arm's outputs before the next run overwrites them. [raw/](raw/)
contains the original benchmark JSON and metadata for all 34 arms. Metadata
records source patch/APK SHA256, compilation policy, workload, collection time,
and base commit. The 16 initial baseline APK hashes all match; the rebuilt
repeat hashes are recorded separately. APK binaries are not archived.

[archive-manifest.json](archive-manifest.json) records identities/hashes of
all 340 traces, raw logs, test XML, metadata, and patches. The full archive is
retained outside Git in the session's `paymentsheet-first-load-measurements-20261022`
directory; traces/logcat are not embedded in this PR. These are local artifacts,
not a publicly downloadable archive. Only the preserved JSON/metadata, measured
patches, extracted CAPTCHA slices, and analysis are committed.

Recompute tables, descriptive intervals, min/median/max ranges, journey frame
summaries, and all 2,040 scalar samples using the Python standard library:

```sh
python3 paymentsheet-benchmark/comparisons/2026-10-22/analyze.py
```

`presentToLoadMs` and `presentToFirstDrawMs`, including their distributions and
intervals, are available in `results.json`; all scalar samples are in
`samples.csv`. Full standard FrameTimingMetric frame distributions remain in
the raw benchmark JSON. Keep the first-frame, overall-presentation, and
later-CAPTCHA observations separate when assessing a fix.
