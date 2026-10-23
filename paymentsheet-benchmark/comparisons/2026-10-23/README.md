# Verification of fixes 2, 4, and 16

This repeats the three selected comparisons from the
[16-fix experiment](../2026-10-22/README.md) using fresh baselines, reverse arm
order, and a 60-second cooldown after each build. It adds **60 measured cold
launches**, ten per arm. All six managed benchmark invocations passed. No test,
fixture, synchronization, harness, or production implementation was changed for
this verification.

These are exploratory managed-emulator measurements. A passing benchmark means
its measurement boundaries and frame matching succeeded; it does not by itself
establish an improvement. Positive reduction means faster. Original and new
samples remain separate.

## Protocol and evidence

Each comparison measures **fix first, baseline second** on the managed Pixel 2
API 33 Google APIs image. Each arm prebuilds the target and runner with
`:paymentsheet-benchmark:assembleDebug :paymentsheet-benchmark-app:assembleRelease`,
waits 60 seconds after successful build completion, then runs:

```sh
./gradlew :paymentsheet-benchmark:pixel2api33DebugAndroidTest --rerun \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR \
  -Pandroid.testInstrumentationRunnerArguments.class=com.stripe.android.paymentsheet.benchmark.FirstPresentationBenchmark#coldWithBaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.paymentsheet.workload=full
```

Use `hidden` for ID 2, `full` for ID 4, and `captcha` for ID 16. Macrobenchmark
requires packaged profiles in both arms, controls compilation, and kills the
target between iterations. Each arm installs the dedicated minified release
target; each iteration starts a fresh process, not a fresh installation or
cleared app data. The developer's emulator is not used.

All arms use common harness revision `008a103e5301811cebd29a84a99e95684d62c606`.
Each fix's exact production patch SHA256 matches its original experiment, and
each baseline production patch is empty. Rebuilt APKs differ from the original
experiment: this repeats the source comparison, not the original binary.
The six actual APKs are now archived with their SHA256 hashes alongside all
60 traces, build/run logs, logcat, test XML, and patches. The local archive is
`/Users/jaynewstrom/stripe/paymentsheet-first-load-verification-20261023`;
[archive-manifest.json](archive-manifest.json) records file identities. These
large artifacts are local, not publicly downloadable from the PR.

Committed [raw JSON and metadata](raw/), [samples.csv](samples.csv), and
[results.json](results.json) preserve all observations. Recompute medians,
ranges, descriptive intervals, and journey-frame summaries with:

```sh
python3 paymentsheet-benchmark/comparisons/2026-10-23/analyze.py
```

Intervals use independent bootstrap median differences (10,000 resamples,
seed 160016, percentile indices 249 and 9749), as in the original report. They
are not adjusted for selecting these three comparisons or multiple endpoints,
and do not correct host drift, emulator scheduling, block order, or OS cache
state. Reverse order and cooldown address some original protocol concerns;
one reversed block is not repeated randomized counterbalancing. Confirm on
representative physical devices before assigning production millisecond savings.

## Load completion → first draw

Times are medians in milliseconds. The interval describes baseline median minus
fix median. Draw means completion of the root Compose draw callback; it is CPU
draw recording, not the display's surface presentation.

| Fix | Baseline | Fix | Reduction [95% interval] | Original reduction |
| --- | --- | --- | --- | --- |
| 2 | 154.64 | 132.41 | +22.23 [-65.33, +55.60] | −18.15 [−68.05, +27.31] |
| 4 | 173.57 | 128.14 | +45.43 [+1.18, +71.85] | +54.88 [−24.25, +96.55] |
| 16 | 162.08 | 164.42 | -2.34 [-85.99, +53.96] | −54.09 [−89.95, +9.27] |

**ID 4 has the clearest first-frame evidence of these three.** Its post-load
draw reduction repeats in direction and similar magnitude (original 55 ms,
rerun 45 ms), and this rerun's descriptive interval excludes zero narrowly.
The selected comparisons and emulator limitations still prevent assigning a
production saving or a reliable ranking across all 16 fixes. ID 2's original
171 ms overall-presentation estimate did not reproduce at that size. ID 16's
first-draw difference remains unresolved.

## Surface presentation, overall latency, and deadlines

Each cell gives baseline / fix medians; reduction [95% interval], in ms.
The surface endpoint uses the actual FrameTimeline frame associated with the
loaded draw (falling forward if its submission was dropped). Overall latency
starts immediately before PaymentSheet builder/presentation and includes loading.
It excludes initial application and benchmark Activity startup.

| Fix | Load → presented frame | Presentation start → presented frame |
| --- | --- | --- |
| 2 | 234.35 / 218.43; +15.91 [-57.90, +61.19] | 669.42 / 630.68; +38.74 [-227.89, +316.39] |
| 4 | 264.30 / 192.50; +71.80 [+3.49, +159.34] | 758.53 / 592.82; +165.72 [-75.18, +323.06] |
| 16 | 245.07 / 230.69; +14.37 [-117.23, +125.12] | 695.63 / 644.26; +51.37 [-342.34, +264.99] |

ID 4's load-to-surface reduction also excludes zero in this block. No overall
presentation interval excludes zero. Original overall reductions for IDs 2, 4,
and 16 were +171.08, −11.90, and −102.04 ms respectively; the new ones are
+38.74, +165.72, and +51.37 ms. These unstable totals cannot establish which
fix saves the most overall presentation time.

Late frames remain. First-loaded-frame deadline overruns and whole-journey
FrameTimingMetric P90 overruns are below. Journey duration/frame counts vary;
P90 is not a fixed post-load window and has no separate interval here. Positive
overrun is lateness against a deadline, not a skipped-refresh count.

| Fix | First frame overrun baseline / fix, ms | Journey P90 overrun baseline / fix, ms |
| --- | --- | --- |
| 2 | 193.92 / 188.12 | 95.47 / 97.35 |
| 4 | 187.66 / 167.65 | 140.59 / 89.07 |
| 16 | 203.48 / 197.93 | 202.63 / 207.36 |

## CAPTCHA startup scopes

Both arms again used `com.google.android.webview` **109.0.5414.123**, code
`541412334`, confirmed from preserved WebViewFactory logcat without a feature
probe. Perfetto v58.2 queried all 20 traces using the unchanged
[scope query](captcha-startup.sql). [Extracted slices](captcha-startup-slices.csv)
include thread IDs, offsets from loader return, and each trace's first draw.

| Scope | Baseline min / median / max, ms | Fix min / median / max, ms | Observed thread |
| --- | --- | --- | --- |
| Warmer Activity creation | 136.97 / 229.49 / 611.28 (10/10) | 1.33 / 4.34 / 12.48 (10/10) | Main both arms |
| Provider initialize | 3.53 / 10.06 / 65.79 (10/10) | 5.61 / 27.26 / 138.44 (10/10) | Main baseline; worker fix |
| Native Chromium startup | 52.05 / 94.25 / 311.38 (10/10) | 93.04 / 115.66 / 201.33 (10/10) | Main both arms |
| WebView init | 74.98 / 147.75 / 344.88 (10/10) | 19.62 / 42.71 / 155.03 (9/10) | Main both arms |

Warmer Activity creation falls from **229.49 to 4.34 ms median** (about 98%),
reproducing the original large local reduction (169.45 to 4.08 ms). The ranges
are separated in both experiments. Provider initialization moves to a worker
in all ten fix traces. Native Chromium startup remains on Main in all twenty
traces, with a fix median of 115.66 ms. These scopes can nest or shift to later
tasks; do not sum their differences or equate the Activity reduction to total
main-thread work eliminated.

Every extracted scope starts **after its first loaded draw** (79/79 completed
slices). Thus the first-draw metric does not capture this later cold-WebView
stall. This rerun reproduces the local startup change, not an overall latency
improvement or a smooth transition. The workload does not test token readiness
or confirmation, and remote CAPTCHA JavaScript is not fixture-backed.

Only nine fix traces contain a completed `WebViewChromium.init` slice; do not
interpret the missing sample as zero. The row above explicitly reports 9/10
coverage and is a summary of completed slices only.

The missing scope is fix iteration 0,
`FirstPresentationBenchmark_coldWithBaselineProfile_iter000_2026-10-23-12-45-46.perfetto-trace`.
A [diagnostic extraction](captcha-missing-init.csv) finds no init slice,
including incomplete slices; recording ends about 42.76 ms after native startup
finishes. It does not establish when later WebView creation finished. This query
runs after schema inspection (`slice`, `trace_bounds`, `LIMIT 0`):

```sql
SELECT s.name, s.dur,
    (s.ts - t.start_ts) / 1e6 AS start_after_trace_ms,
    (t.end_ts - s.ts) / 1e6 AS remaining_trace_ms
FROM slice s CROSS JOIN trace_bounds t
WHERE s.name GLOB '*WebViewChromium*'
ORDER BY s.ts;
```

## Validation and interpretation

All six builds and managed benchmark invocations passed. Each produced ten
first-draw/surface matches with all six scalar metrics. Source-patch hashes
match the original experiment, saved APK hashes match metadata, and all 60
trace files are present with hashes. Repository-wide `./gradlew detekt` passed.
These are timing reruns, not new full functional regression suites for each fix.

For the load-completion-to-first-draw endpoint, ID 4 is the strongest candidate from this
verification; ID 2 remains unresolved. ID 16 has the largest reproduced local
scope change, but that scope is after the endpoint and its native startup stall
remains. No overall-presentation improvement is resolved for any of the three.
Run repeated, counterbalanced comparisons on representative physical devices
before treating these exploratory emulator deltas as production savings.
