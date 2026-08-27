---
name: fixing-flakes
description: Reproduce, diagnose, and fix flaky stripe-android tests with focused, repeated validation and evidence-driven synchronization or determinism fixes. Use for intermittent unit, coroutine, Robolectric, Compose, screenshot, instrumentation, Gradle Managed Device, PaymentSheet E2E, Chrome, WebView, UiAutomator, or Espresso failures, while ensuring temporary ShampooRule edits never enter commits or pull requests.
---

# Fixing Flakes

Invoke `delegate-low-reasoning-work` before any Gradle command and delegate the exact command when an executor is available.

Treat every `ShampooRule` swap or iteration-count edit as uncommittable diagnostic instrumentation. Use it only in the local working tree to reproduce and validate a fix. Never stage, commit, push, or submit those edits in a pull request.

## Establish the scope

1. Locate the test and its rules with `rg`. Classify it as unit, coroutine, Robolectric/Compose, screenshot, instrumentation, managed-device, or external-surface E2E. Do not use an emulator for a non-instrumentation test.
2. Write one sentence stating the behavior the test proves. A synchronization step may observe readiness, but must not directly perform the behavior under test. If a proposed wait or helper makes the assertion tautological, reject it and fix the dependency or replace the invalid test instead.
3. Record the original exception, first relevant stack frame, execution task, device/API/browser when applicable, and whether the failure occurred before, during, or after the tested behavior. Distinguish a test failure from emulator boot, Gradle, network, or other infrastructure failures.
4. Audit the boundaries that can vary between executions: wall versus virtual time, animation/frame time, process-global state, retained caches or selections, external activities, IME/browser state, asynchronous resource identity, and teardown completion.

## Choose the repetition mechanism

Use one focused class or method and expose the first failure. Do not combine inner retries, outer command retries, and repetition; they destroy the failing iteration's evidence.

For instrumentation tests, use the diagnostic-only `ShampooRule` workflow below. For unit, coroutine, Robolectric/Compose, and screenshot tests, keep the normal rule chain and repeat the focused Gradle task uncached. Start with 2 executions, then require 50 clean executions of the final candidate. For example:

```bash
for iteration in {1..2}; do
  echo "Executing iteration: $iteration"
  ./gradlew :MODULE:testDebugUnitTest \
    --tests 'com.example.TestClass' \
    --rerun-tasks || break
done
```

Change `2` to `50` only after the smoke run passes. Select the module's actual unit, Robolectric, or screenshot task; invoke the matching repository test skill before changing test code. Report both Gradle invocations and actual test-method executions when a filtered class contains multiple tests.

## Apply ShampooRule to instrumentation tests

Replace the `RetryRule` in the shared rule chain actually used by the flaky test. Do not add `ShampooRule` alongside `RetryRule`, because retries hide the first failing iteration.

Before editing the rule, inspect and preserve its baseline diff:

```bash
git diff -- paymentsheet-example/src/androidTest/java/com/stripe/android/utils/TestRules.kt
```

Restore only the diagnostic edits after soaking so any pre-existing user changes remain untouched.

For PaymentSheet example E2E tests, edit:

`paymentsheet-example/src/androidTest/java/com/stripe/android/utils/TestRules.kt`

Use this shape and preserve the rule's existing position relative to timeout and cleanup rules:

```kotlin
import com.stripe.android.testing.ShampooRule

fun create(
    disableAnimations: Boolean = true,
    shampooIterations: Int = 2,
    block: RuleChain.() -> RuleChain = { this },
): TestRules {
    // ...
    if (BuildConfig.IS_RUNNING_IN_CI && !BuildConfig.RUN_LATENCY_TESTS_IN_CI) {
        chain.around(ShampooRule(shampooIterations))
    } else {
        chain
    }
}
```

`ShampooRule` is defined at:

`payments-core-testing/src/main/java/com/stripe/android/testing/ShampooRule.kt`

Apply it to an individual test only when that test does not use the shared rule chain. Verify that every iteration includes the test's setup, teardown, timeout, and external-app cleanup.

The shared `TestRules` default affects every consumer in an unfiltered CI run. The Shampoo swap is always temporary, including when the task asks to add Shampoo or increase its iterations. Such requests authorize local diagnosis and validation, not committing the rule change. Restore the original rule immediately after collecting soak evidence.

## Run a focused emulator test

Use the Chrome-enabled Pixel 2/API 33 managed device for browser redirects:

```bash
CI=true ./gradlew \
  -Pandroid.experimental.androidTest.numManagedDeviceShards=1 \
  -Pandroid.experimental.testOptions.managedDevices.maxConcurrentDevices=1 \
  '-Pandroid.testInstrumentationRunnerArguments.class=com.example.TestClass#testMethod' \
  :paymentsheet-example:pixel2api33chromeBaseDebugAndroidTest \
  --init-script build-configuration/instrumentation-test-init.gradle
```

Important details:

- Set `CI=true`; the PaymentSheet example rule chain gates ShampooRule on `BuildConfig.IS_RUNNING_IN_CI`.
- Quote the class filter. An unquoted `#` begins a shell comment.
- Use the fully qualified test class and one method while reproducing.
- Keep one device and one shard so iterations remain serial and attributable.
- Use `pixel2api33chrome` for Chrome flows. For tests that do not need Chrome, select the module's appropriate managed device and confirm the exact task with `./gradlew :MODULE:tasks --all`.
- Use the module's IME-enabled managed device, such as `pixel2api33ime`, for tests annotated with `RequiresIme`; do not substitute the Chrome image or a generic emulator.
- Do not add an outer command retry or resurrect `scripts/retry_with_emulator_cleanup.sh`; outer retries obscure the first failure.

## Escalate Shampoo iterations

1. Start with `ShampooRule(2)`.
2. Run only the reported test. If it fails, diagnose and fix the first failure, then rerun two iterations.
3. After two iterations pass, change the count to 50.
4. Run the same focused command again. A fix is not validated until all 50 pass.
5. If iteration N fails, fix the underlying cause, smoke-test with two iterations, then repeat the full 50. Do not lower the final threshold to obtain a green result.
6. After all 50 pass, restore the exact pre-soak `RetryRule` configuration before staging any files, then run the focused test once in the normal configuration.

## Read the evidence

Gradle reports one JUnit test because ShampooRule repeats inside one test method. Confirm the actual iteration count in the managed-device logcat rather than inferring it from `tests="1"`.

Find artifacts with `rg --files`, then search them:

```bash
rg -n 'Executing iteration|Failed on iteration|UiObjectNotFoundException|AssertionError' \
  paymentsheet-example/build/outputs/androidTest-results
```

For strict `NetworkRule` tests, interpret dispatcher failures before changing expectations:

- `Production code made extra requests` identifies a production branch that ran without a matching enqueue. Record the request event/path and the nearest matcher; do not enqueue the variable request merely to make both orderings pass.
- `Mock responses is not empty` identifies expected production behavior that never ran. Record the remaining matcher and determine which lifecycle, layout, or coroutine boundary was supposed to trigger it.
- When fixing the first mismatch exposes a different failure at a later Shampoo iteration, treat it as a second race. Diagnose it independently, return to 2 iterations, then repeat all 50 on the combined candidate.

Useful outputs include:

- `*/build/outputs/androidTest-results/managedDevice/.../shard_0/logcat-<class>-<method>.txt`
- `*/build/outputs/androidTest-results/managedDevice/.../test-result.textproto`
- `*/build/reports/androidTests/managedDevice/debug/flavors/base/allDevices/index.html`

Iteration numbers are zero-based: `0` through `49` proves 50 executions. Require zero `Failed on iteration` lines and a passing XML/textproto result. Inspect the first exception, screenshots, window hierarchy, and surrounding logcat before editing code.

For non-instrumentation repetition, confirm that every invocation executed the focused test rather than returning `UP-TO-DATE` or from cache. Record the first failing invocation and its test report. A 50-run class soak with 12 methods is 600 test-method executions; report both numbers rather than calling it 50 tests.

## Fix causes, not symptoms

Require one deterministic outcome for a given test setup. Do not make a flaky test pass by accepting whichever result wins a race. If the nondeterminism originates in production code, change the production code or its boundary to make the intended behavior deterministic, then assert that behavior. When variability is intentional, control its inputs in the test instead of weakening the assertion.

Prefer bounded state-based synchronization over retries or arbitrary sleeps:

- Time: keep all timing decisions in one domain. When coroutine delays use a test scheduler, inject a typed clock backed by that scheduler instead of reading wall time. Match test animation delays to production delays; use frame buffers only for frame scheduling tolerance, not to cover incorrect constants.
- Sampled asynchronous work: identify code that observes whether work has completed without synchronizing with its completion. Even immediately returning collaborators can be scheduler-dependent. Preserve the production contract and make observable results deterministic at the appropriate boundary rather than masking the race in the test.
- Execution ordering: change coroutine start or dispatch behavior only when the production contract requires work to begin or complete before it is observed. Verify that any inline work is lightweight, account for ordering, cancellation, error handling, and latency changes, and do not force genuinely asynchronous work to finish synchronously.
- Repeated-process state: assume Shampoo iterations share process state. Establish a known baseline for companion/static flags, analytics sent-once markers, customer/session caches, payment selections, IME visibility, and browser state. Reset only state the test owns, using a narrow test seam when necessary.
- Compose: wait for the exact semantics state, combining tag/text with enabled or click action when relevant. Allow `atLeastOneRootRequired = false` only during a known activity transition, and add `conditionDescription` to every nontrivial wait.
- Event-driven stabilization: do not infer stability from a particular callback count or repeated value. When production needs a stable snapshot, define completeness and a quiet period explicitly, restart stabilization when relevant state changes, and cover incomplete state, cancellation, reset, and one-time delivery with deterministic tests.
- Ordering: wait for processing, screen visibility, or event delivery before asserting the next state. Do not assume an activity finishing, a semantics click, or one analytics event implies that downstream work is complete.
- Espresso: synchronize with app idleness and explicit activity/window transitions.
- UiAutomator and Chrome: wait for the requested package/activity and target object. Prefer stable resource IDs to coordinate taps, handle every valid onboarding screen, and wait for the transient or first-run activity to exit. Do not call `UiScrollable.scrollIntoView` unless a scrollable container exists.
- WebView and server-driven UI: first determine whether the variant is test-controlled. Pin a controllable fixture; when the server legitimately owns the order, poll all valid selectors within one shared timeout and include the attempted selectors and final exception in the failure.
- Async UI resources: preserve stable identity for asynchronously loading drawables, painters, or other remembered resources so recomposition does not restart work indefinitely.
- External activities: restore window focus and clean up Chrome between iterations.
- Test lifecycle: unregister callbacks, clear retained activity/state references, and make teardown safe after partial failures.

Use path-specific timeouts only after evidence shows legitimate latency, keep them below the overall test timeout, and wait on a precise state. Do not catch and suppress the failure, increase `RetryRule` attempts, add a broad delay, merely lengthen an imprecise wait, or weaken assertions. If the failure is infrastructure-only, report that evidence separately instead of changing product or test behavior.

## Minimize and isolate the fix

Treat a passing soak as validation of a candidate fix, not proof that every change in the candidate is necessary. Before considering the task complete:

1. Review the permanent diff without the Shampoo instrumentation. Map each behavior-changing line to the observed failure mode it is intended to address.
2. Attempt to remove, narrow, or consolidate speculative and unrelated changes one at a time. Remove incidental cleanup, redundant waits or guards, and defensive code that is not supported by the failure evidence.
3. Revalidate after each material simplification. Run the focused test in its normal configuration, then validate the exact final candidate with the selected repetition mechanism: 2 and 50 Shampoo iterations for instrumentation, or 2 and 50 uncached Gradle executions for non-instrumentation.
4. Keep the smallest explainable change that addresses the demonstrated race, lifecycle bug, or synchronization gap. Do not claim causality solely because a broad diff passed the soak.
5. If multiple permanent changes cannot be isolated safely, document why they are coupled and what evidence shows each is required.

Do not consider the task complete until this simplification attempt is complete and the final PR diff contains the isolated causal fix rather than the larger diagnostic candidate.

## Keep Shampoo out of commits and PRs

Before any `git add`, `git commit`, `gh stack push`, `gh stack submit`, or PR creation:

1. Restore the rule file to its pre-soak `RetryRule` configuration using `apply_patch`. Do not discard unrelated or pre-existing changes.
2. Inspect both unstaged and staged rule-file diffs:

   ```bash
   git diff -- paymentsheet-example/src/androidTest/java/com/stripe/android/utils/TestRules.kt
   git diff --cached -- paymentsheet-example/src/androidTest/java/com/stripe/android/utils/TestRules.kt
   ```

3. Before pushing or submitting, inspect the committed PR diff against its base:

   ```bash
   git diff <base>...HEAD -- paymentsheet-example/src/androidTest/java/com/stripe/android/utils/TestRules.kt
   ```

4. Require the pre-soak rule state: no diagnostic `RetryRule`-to-`ShampooRule` swap and no diagnostic iteration-count change may appear in any of those diffs. If one is present, stop and remove it before committing or submitting.

Document the 2- and 50-iteration Shampoo results in the PR's Testing section, but do not include the Shampoo instrumentation itself in the PR diff.

When a pull request is opened for the flake fix, add the `flake-fix` label and verify it is present before treating PR submission as complete:

```bash
GH_HOST=github.com gh pr edit <number> --add-label flake-fix
GH_HOST=github.com gh pr view <number> --json labels
```

## Finish cleanly

1. For instrumentation, confirm the artifact contains all 50 iteration lines, no failed iteration, and a passing test result. For non-instrumentation, confirm all 50 uncached invocations and report the actual test-method execution count.
2. Restore the original retry/normal rule when applicable and run the focused test once more in its normal configuration.
3. Run `git diff --check`, inspect the complete diff, and apply the commit/PR guards above.
4. Report the exact Gradle command, repetition mechanism, iteration and method-execution counts, elapsed time, failure iteration if any, files changed, causal fix, preserved test contract, and how the candidate was simplified or isolated. For Shampoo runs, state that Shampoo was diagnostic-only and is absent from the submitted diff.
