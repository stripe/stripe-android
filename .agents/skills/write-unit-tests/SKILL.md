---
name: write-unit-tests
description: Use when writing or structuring unit tests or fake test implementations in stripe-android — covers scenarios, Turbine interaction tracking and Flow testing, and Truth assertions
---

# Writing Unit Tests

Use fakes, scenario helpers, and behavior-focused assertions to keep tests reliable and readable.

## Core Principles

1. Prefer fakes over mocks. Name them `FakeClassName`, keep them `internal`, and place them in the test source set.
2. Track every observed fake interaction with Turbine. Mutable properties are only for configuring behavior.
3. Put shared setup in a `runScenario` helper and validate every fake after the test block.
4. Use Google Truth assertions and prefer field-level assertions when exact object equality is not the contract.
5. Cover one scenario or configuration per `@Test`.
6. Test Flow emissions with Turbine's `.test { }` API.

## Creating Fakes

Give fake constructors sensible defaults so tests only specify behavior relevant to the case. Use a companion `create()` factory when initialization is complex.

All interaction history—including calls, callbacks, and parameterless lifecycle events—must use Turbine. Do not use mutable lists, counters, `wasCalled` flags, or callback shortcuts. Retrieve a registered callback from its recorded call so the test also proves registration occurred.

```kotlin
internal class FakeLauncher(
    var launchResult: Boolean = false,
) : Launcher {
    val registerCalls = Turbine<RegisterCall>()
    val launchCalls = Turbine<LaunchCall>()
    val unregisterCalls = Turbine<Unit>()

    override fun register(callback: () -> Unit) {
        registerCalls.add(RegisterCall(callback))
    }

    override fun launch(id: String): Boolean {
        launchCalls.add(LaunchCall(id))
        return launchResult
    }

    override fun unregister() {
        unregisterCalls.add(Unit)
    }

    fun ensureAllEventsConsumed() {
        registerCalls.ensureAllEventsConsumed()
        launchCalls.ensureAllEventsConsumed()
        unregisterCalls.ensureAllEventsConsumed()
    }

    data class RegisterCall(val callback: () -> Unit)
    data class LaunchCall(val id: String)
}
```

Use data classes for calls with parameters and `Turbine<Unit>` for parameterless calls. Consume expected calls with `awaitItem()`, use `expectNoEvents()` when no call is expected, and include every owned Turbine in `ensureAllEventsConsumed()`.

For view actions, use `ViewActionRecorder<ViewAction>` instead of another recording mechanism. Accept it as a constructor or factory parameter with a default when useful:

```kotlin
internal class FakeInteractor(
    private val viewActionRecorder: ViewActionRecorder<ViewAction> = ViewActionRecorder(),
) : Interactor {
    override fun handleViewAction(viewAction: ViewAction) {
        viewActionRecorder.record(viewAction)
    }
}
```

Good codebase references are:

- `paymentsheet/src/test/java/com/stripe/android/paymentsheet/analytics/FakeEventReporter.kt`
- `paymentsheet/src/test/java/com/stripe/android/utils/FakeCustomerRepository.kt`
- `paymentsheet/src/test/java/com/stripe/android/paymentsheet/verticalmode/FakePaymentMethodVerticalLayoutInteractor.kt`

## Scenario Pattern

Create `runScenario` and `Scenario` at the bottom of the test file. Give setup parameters defaults, expose the system under test and fakes through `Scenario`, and call each fake's `ensureAllEventsConsumed()` after the block.

```kotlin
class MyFeatureTest {
    @Test
    fun `fetching data returns success`() = runScenario {
        fakeRepository.result = Result.success(testData)

        val result = systemUnderTest.fetchData()

        assertThat(result.isSuccess).isTrue()
        assertThat(fakeRepository.fetchCalls.awaitItem()).isEqualTo(
            FetchCall(userId = "123")
        )
    }

    private fun runScenario(
        config: Config = defaultConfig,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakeRepository = FakeRepository()
        val systemUnderTest = MyFeature(
            repository = fakeRepository,
            config = config,
        )

        Scenario(
            systemUnderTest = systemUnderTest,
            fakeRepository = fakeRepository,
        ).apply { block() }

        fakeRepository.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val systemUnderTest: MyFeature,
        val fakeRepository: FakeRepository,
    )
}
```

Use `runTest` directly when shared scenario setup would not help. In that case, validate fakes explicitly.

## Assertions and Test Scope

Use `assertThat(actual).isEqualTo(expected)` from Google Truth. Prefer assertions on meaningful fields because their failures are easier to diagnose; use whole-object equality when exact equality is the behavior under test.

Keep each test focused on one case. Split related configurations into separate tests so one failure does not hide another. Test observable behavior rather than implementation details, and include relevant null, empty, error, and boundary cases. Avoid vacuous assertions by establishing the precondition before testing its removal.

## Testing Flows

Use Turbine's `.test { }` API for Flow and StateFlow emissions:

```kotlin
systemUnderTest.state.test {
    assertThat(awaitItem()).isEqualTo(State.Loading)

    fakeRepository.emit(newData)

    assertThat(awaitItem()).isEqualTo(State.Loaded(newData))
    ensureAllEventsConsumed()
}
```

Common operations are `awaitItem()`, `expectNoEvents()`, `skipItems(n)`, and `ensureAllEventsConsumed()`.

## Concurrency with Real I/O

For coroutine tests involving real NetworkRule or OkHttp I/O:

- Use `runTest`, `async`, and `await()` so failures propagate.
- Use `testScheduler.advanceUntilIdle()` to advance to a suspension point; never use `Thread.sleep`.
- After releasing a latch, use `await()` because OkHttp resumes asynchronously.
- Give every `CountDownLatch.await()` in a mock handler a timeout.
- Use Turbine for StateFlow emission sequences and latches for request ordering.
- Assert both that a queued request did not arrive while held and that it arrived after release.

## Related Skills

- Invoke [`parameterized-tests`](../parameterized-tests/SKILL.md) for parameterized cases.
- Invoke [`compose-tests`](../compose-tests/SKILL.md) for Compose UI tests.
- Invoke [`network-tests`](../network-tests/SKILL.md) for NetworkRule integration tests.
