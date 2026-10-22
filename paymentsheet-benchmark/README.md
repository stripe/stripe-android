# PaymentSheet first-presentation benchmark

This Jetpack Macrobenchmark installs a dedicated, minified, non-debuggable release
app (`com.stripe.android.paymentsheet.benchmarkapp`) and a separate instrumentation
APK. It measures the first horizontal PaymentSheet presentation in each fresh
process. It does not use the interactive example app or its saved configuration.

The target app presents a card-only PaymentIntent with full billing address,
name, email, and phone collection. SDK HTTP requests use an in-memory connection
backed by the app's own elements-session fixture; unexpected endpoints fail.
The production PaymentSheet loader, controllers, and Compose UI still execute.
The default workload excludes saved methods, Link, Google Pay, and CAPTCHA/WebView.
The `hidden` and `captcha` variants are described below.

## Run on the managed device

```sh
./gradlew :paymentsheet-benchmark:pixel2api33DebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

The `Debug` task refers to the instrumentation runner. Its target is the release
variant of `paymentsheet-benchmark-app`; that app has no debug variant.
The managed Pixel 2 API 33 uses the Google APIs image for hardware-rendered
FrameTimeline data (the ATD image uses software UI rendering). It runs separately
from any developer emulator. The
explicit emulator suppression permits a functional smoke check of measurement
and trace extraction. Emulator timings are not representative hardware results.
Do not suppress other benchmark errors or use debug target builds.

Both tests run ten cold-process iterations:

- `coldWithBaselineProfile` requires installation of the APK's packaged baseline
  profile (including dependency profiles).
- `coldWithoutCompilation` resets compilation without applying a baseline profile.

Macrobenchmark controls compilation state and kills the target process between
iterations. A cold iteration is not a new installation or cleared application
data. The dedicated package is installed for the managed test run. Keep device,
OS/WebView versions, animations, thermal state, fixture, and compilation mode
consistent when comparing changes. Use repeated runs on a representative physical
device for performance conclusions; the managed device validates the harness.

To run only one case, add, for example:

```sh
-Pandroid.testInstrumentationRunnerArguments.class=com.stripe.android.paymentsheet.benchmark.FirstPresentationBenchmark#coldWithoutCompilation
```

## What the measurements mean

A trace-only SDK marker starts immediately after `PaymentElementLoader.load`
returns successfully on its worker, before loaded-state handling on Main. It ends
after the first draw of the loaded PaymentSheet content's root, containing the
form and primary button (which can be below the viewport in this full-address
workload).
It excludes network/loading time and includes the work that turns the loader
result into UI. It does not wait for the sheet's entrance animation to settle.
Failed loads or missing frame data fail the benchmark rather than reporting zero.
The benchmark records distributions without imposing a device-specific latency threshold.
No production analytics event is sent for these measurements.

| Metric | Boundary |
| --- | --- |
| `loadToFirstDrawMs` | Successful loader return to completion of the loaded content's Compose draw callback; CPU draw recording, not display presentation. |
| `loadToFirstPresentedFrameMs` | Loader return to the end of the actual FrameTimeline surface frame containing that draw. If that submission is dropped, use the first subsequent non-dropped frame on the same PaymentSheet layer. |
| `firstPresentedFrameOverrunMs` | Actual end of that presented surface frame minus its expected deadline; positive means late, negative means deadline headroom. |
| `frameDurationCpuMs`, `frameOverrunMs` | Standard `FrameTimingMetric` distributions for the whole measured launch/presentation journey, including loading and animation frames. |

The custom metric links the draw callback to its main-thread `Choreographer#doFrame`
token and then to PaymentSheetActivity's FrameTimeline layer. It requires API 31+
FrameTimeline data. It deliberately fails if the marker or matching frame cannot
be found. These metrics distinguish CPU draw completion, surface presentation,
and frame deadline misses; they are not interchangeable.

Macrobenchmark writes JSON distributions and per-iteration `.perfetto-trace`
files into Gradle's additional managed-device test output under
`paymentsheet-benchmark/build/outputs/`. Open a trace in Perfetto to inspect
`PaymentSheet#loadToFirstDraw`, the containing main-thread frame, and its
FrameTimeline record. Preserve the JSON and traces with any performance report.
Report distributions and the workload/compilation mode, not a single iteration
or an inferred total number of skipped refreshes.

See [VALIDATION.md](VALIDATION.md) for the initial managed-device smoke run and
an independently checked trace-to-frame match.

## Isolated fix comparisons

Pass `-Pandroid.testInstrumentationRunnerArguments.paymentsheet.workload=full`,
`hidden`, or `captcha` to select the workload. `full` is the default. `hidden`
retains full address collection while hiding name/email/phone. `captcha` uses
full billing collection and enables real passive CAPTCHA warmup with hCaptcha's
public test site key. The CAPTCHA JavaScript is not fixture-backed; this workload
measures startup/presentation only, not token readiness or confirmation success.
Record the installed WebView provider version when reporting it.

`presentToLoadMs`, `presentToFirstDrawMs`, and
`presentToFirstPresentedFrameMs` start immediately before building/presenting
PaymentSheet in the benchmark activity. They complement the loader-return
metrics: moving work before loader return must not be counted as eliminated work.
These intervals exclude initial benchmark Activity/application startup.

For each independent fix, build the unchanged harness with baseline production
sources and then with only that fix. Use `coldWithBaselineProfile` in both arms,
so the profile-packaging fix is compared with profiles enabled in both apps.
Preserve each arm's JSON, traces, APK hash, source patch, compilation mode, and
workload before running the next arm. Use a fresh baseline for each fix to reveal
host/device drift. Do not add the individual deltas: the fixes overlap.

See [the independent 16-fix comparison report](comparisons/2026-10-22/README.md)
for measured samples, source patches, APK identities, and uncertainty.
