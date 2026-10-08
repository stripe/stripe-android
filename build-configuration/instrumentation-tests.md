# Instrumentation tests

Instrumentation suites run on [emulator.wtf](https://docs.emulator.wtf/integrations/gradle-plugin/)
through its Gradle plugin and the Gradle Managed Devices API. The shared configuration is in
the root [`build.gradle`](../build.gradle), applied to Android application and library modules.

## Authentication

Set `EW_API_TOKEN` in your shell environment before running remote tests. In Bitrise, add it as
a Secret available to the instrumentation workflow. The plugin reads it automatically; keep
the token out of Gradle files and command-line arguments. No token is needed to assemble APKs
or inspect task graphs with `--dry-run`.

## Devices and commands

Both managed devices use Android API 33:

| Device | Image | Suites |
| --- | --- | --- |
| `ewPixel2api33Atd` | Pixel 2 Automated Test Device | General instrumentation; focused PaymentSheet runs without a software keyboard |
| `ewPixel2api33` | Pixel 2 full image | All PaymentSheet tests in CI, browser E2E, Financial Connections, Connect, Crypto, and CardScan tests |

Run PaymentSheet's entire suite, including software-keyboard tests, with:

```sh
./gradlew :paymentsheet:ewPixel2api33DebugAndroidTest \
  --init-script build-configuration/instrumentation-test-init.gradle
```

For a focused run, select PaymentSheet's regular tests on ATD with:

```sh
./gradlew :paymentsheet:ewPixel2api33AtdDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.notAnnotation=com.stripe.android.paymentsheet.RequiresIme \
  --init-script build-configuration/instrumentation-test-init.gradle
```

Select only its software-keyboard tests with:

```sh
./gradlew :paymentsheet:ewPixel2api33DebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.annotation=com.stripe.android.paymentsheet.RequiresIme \
  --init-script build-configuration/instrumentation-test-init.gradle
```

Run the PaymentSheet example E2E suite with:

```sh
./gradlew :paymentsheet-example:ewPixel2api33BaseDebugAndroidTest \
  --init-script build-configuration/instrumentation-test-init.gradle
```

The example app's `emulatorwtf.targets` configuration excludes Google Pay, which runs on Firebase.
The init script adds quarantined methods to the plugin's native test targets for emulator.wtf
tasks, while local connected tests retain the runner's `notClass` exclusions. Use the plugin's
`targets` DSL for class and method lists: ew-cli's `--environment-variables` cannot encode
comma-separated `class` or `notClass` runner arguments. Other instrumentation runner arguments
and Android Test Orchestrator configuration pass through to emulator.wtf.
The plugin also provides `testDebugWithEmulatorWtf` (or `testBaseDebugWithEmulatorWtf` for
the example app) for running tests without the managed-device task names.

## Migrated end-to-end tests

Financial Connections and Connect end-to-end scenarios previously run by Maestro now live in
their example apps' `src/androidTest` source sets. Android UI tests drive the native UI and Chrome
against the real example backends, with Compose actions for annotated links. The seven active
Financial Connections scenarios cover OAuth, connected accounts, PaymentIntent, downtime recovery,
manual entry, returning Link users, and Instant Debits. Connect's scenario checks that account onboarding loads.

Run both suites, including Connect's existing instrumentation tests, with:

```sh
./gradlew :financial-connections-example:ewPixel2api33DebugAndroidTest \
  :connect-example:ewPixel2api33DebugAndroidTest \
  --init-script build-configuration/instrumentation-test-init.gradle
```

Financial Connections requires `STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL` in the
environment or Gradle properties. Connect uses its existing demo backend. Both suites record
videos and disable emulator.wtf result caching because they create real backend sessions.
Browser state is cleared between scenarios, and returning-user tests generate a unique email.

The four previously disabled Financial Connections flows remain `@Ignore`: networking
(BANKCON-14726), web OAuth (RUN_BANKCON_AUX-1237), livemode FinBank, and livemode MX Bank.
Setting `test_environment=edge` keeps the five previously edge-tagged scenarios and skips
the other Financial Connections and Connect end-to-end scenarios.

The init script maps existing Maestro quarantine names to Android class/method identifiers using
[`migrated-maestro-tests.json`](migrated-maestro-tests.json). New quarantines use the Android
test names directly. JUnit reports, logcat, and videos use the shared instrumentation artifact export.

## Sharding and results

`shardTargetRuntime = 13` enables smart sharding with a 13-minute execution target. emulator.wtf
chooses the shard count using historical test durations. On the first run, it estimates
10 seconds per test until history is available. This target excludes APK assembly and does
not guarantee a 13-minute CI job. Each shard has a 30-minute timeout, and failed tests can
be retried twice by emulator.wtf.

Do not combine this with `android.experimental.androidTest.numManagedDeviceShards` or
`maxConcurrentDevices`; emulator.wtf manages shard allocation remotely.

Managed-device tasks store merged JUnit XML and shard logcat under
`MODULE/build/outputs/androidTest-results/managedDevice/`. The plugin's convenience tasks
store results under `MODULE/build/outputs/androidTest-results/emulatorWtf/`. Bitrise exports
per-class JUnit reports using `splitInstrumentationTestReportsByClass` and uploads the raw
results and logcat as artifacts.

## Bitrise workflows

The push and pull-request pipeline runs all emulator.wtf suites in `run-instrumentation-tests`
using one Gradle invocation with `--parallel --continue`. Independent module suites can overlap
as their APKs become ready, subject to Gradle's available workers and emulator.wtf's account
concurrency limit. All PaymentSheet tests use the full image, including software-keyboard tests,
so no separate runner-filtered invocations are needed. The workflow shares checkout, Gradle
caches, and result export across the general, Financial Connections, Connect, Crypto, CardScan,
PaymentSheet, and PaymentSheet example E2E suites. Independent suites continue after a failure,
and report export always runs.

Google Pay E2E runs separately in `run-paymentsheet-google-pay-e2e` on Firebase Test Lab because
it requires automatic Google account login. This workflow is excluded from push and pull-request
builds. Configure its nightly schedule in Bitrise's **Scheduled builds**: branch `master`, workflow
`run-paymentsheet-google-pay-e2e`, cron `0 8 * * *`, timezone UTC. Bitrise stores schedules in the
service, outside `bitrise.yml`.

Latency benchmarks keep their existing local emulator and direct adb access to
preserve comparable measurements.
Local instrumentation runs remain available through `connectedAndroidTest` with a
connected emulator or device.
