# Initial harness validation

On 2026-10-22, the managed Pixel 2 API 33 run passed both cases, ten cold-process
iterations each. Repository-wide detekt and the release app/runner builds passed.
The existing `PaymentSheetViewModelTest` and `PaymentSheetActivityTest` also passed
(163 tests, no failures or skips). The app APK contains `assets/dexopt/baseline.prof` and `baseline.profm`, is
non-debuggable, and enables shell profiling. This run precedes the proposed
PaymentSheet performance fixes; it applies no manually curated Stripe profiles.

These are emulator smoke-run observations, not representative device performance
or an estimate of savings from any proposed fix or from baseline profiles.
This initial smoke run did not compare production fixes. The later
[independent comparison report](comparisons/2026-10-22/README.md) records those trials.

Environment: Google APIs ARM64 image,
`google/sdk_gphone64_arm64/emu64a:13/TE1A.240213.009/12342917:userdebug/dev-keys`,
ART mainline version `331711080`, two virtual CPU cores. See [README.md](README.md)
for the fixture workload, compilation modes, metric definitions, and limitations.

Command (the task-scoped `--rerun` forces fresh instrumentation):

```sh
./gradlew detekt :paymentsheet-benchmark:pixel2api33DebugAndroidTest --rerun \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

| Compilation mode | Metric | Min / median / max (ms) |
| --- | --- | --- |
| Packaged baseline profile required | Load → first draw | 112.57 / 161.26 / 260.31 |
| Packaged baseline profile required | Load → first presented frame | 163.03 / 242.08 / 414.94 |
| Packaged baseline profile required | Presented frame deadline overrun | 120.54 / 184.05 / 357.44 |
| No compilation | Load → first draw | 213.51 / 248.56 / 362.49 |
| No compilation | Load → first presented frame | 261.15 / 313.86 / 509.79 |
| No compilation | Presented frame deadline overrun | 215.51 / 275.98 / 488.33 |

The JSON is `com.stripe.android.paymentsheet.benchmark-benchmarkData.json` under
`build/outputs/managed_device_android_test_additional_output/debug/pixel2api33/`,
alongside twenty per-iteration traces. These are generated artifacts and are not
committed; reproduce the run to obtain them.

The final SQL is embedded in
[FirstLoadedFrameMetric.kt](src/main/java/com/stripe/android/paymentsheet/benchmark/FirstLoadedFrameMetric.kt).
It was also run independently with desktop Perfetto v58.2 against
`FirstPresentationBenchmark_coldWithBaselineProfile_iter000_2026-10-22-18-43-35.perfetto-trace`.
It returned 248.689084 ms to draw, 414.940542 ms to presentation, and 357.437685 ms
overrun, matching that iteration's benchmark JSON. The draw callback ended inside
`Choreographer#doFrame 7665`; the metric matched surface token `7665` on
`TX - com.stripe.android.paymentsheet.benchmarkapp/com.stripe.android.paymentsheet.PaymentSheetActivity#122`,
whose present type was `Late Present`. Thus the metric selected the frame
containing the loaded draw rather than an earlier loading frame.

Run this workload repeatedly on representative physical devices before setting
latency budgets or attributing improvements to an individual fix. Saved methods,
Link, Google Pay, and CAPTCHA/WebView require additional workloads.
