---
name: write-unit-tests
description: Use when writing or structuring unit tests in stripe-android — covers runScenario pattern, fakes, Turbine Flow testing, and Truth assertions
---
# Setting Up Tests

This skill describes how to structure tests in the Stripe Android SDK using fakes, scenarios, and proper verification patterns.

## Core Principles

1. **Use fakes over mocks** - Leverage fake implementations for dependencies (see `create-fake` skill)
2. **Create test scenarios** - Use Scenario classes with `runScenario` functions to organize test setup
3. **Verify all events consumed** - Call `ensureAllEventsConsumed()` on fakes after test block
4. **Use Truth assertions** - Always use `assertThat(actual).isEqualTo(expected)` from Google Truth
5. **Use Turbine for Flow testing** - Test Flow emissions with Turbine's `.test { }` syntax

## Basic Test Structure

Every test should follow this pattern:

```kotlin
@Test
fun `test description`() = runScenario(
    // Test-specific parameters
    config = testConfig
) {
    // 1. Configure: Set up fake behaviors (optional)
    fakeService.result = expectedResult

    // 2. Execute: Call the code under test
    val result = systemUnderTest.doSomething()

    // 3. Verify: Assert results and check fake calls
    assertThat(result).isEqualTo(expected)
    assertThat(fakeService.calls.awaitItem()).isEqualTo(expectedCall)
}
// 4. Validation: ensureAllEventsConsumed called automatically by runScenario
```

## Scenario Pattern with runScenario

### Basic Structure

Create a `runScenario` function and a `Scenario` class at the bottom of your test file:

```kotlin
class MyFeatureTest {
    @Test
    fun `test case`() = runScenario {
        // Test code using scenario fields
        assertThat(systemUnderTest.getValue()).isEqualTo(expectedValue)
    }

    private fun runScenario(
        config: Config = defaultConfig,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        // Setup fakes
        val fakeRepository = FakeRepository()
        val fakeAnalytics = FakeAnalytics()

        // Create system under test
        val systemUnderTest = MyFeature(
            repository = fakeRepository,
            analytics = fakeAnalytics,
            config = config,
        )

        // Run test block with scenario context
        Scenario(
            systemUnderTest = systemUnderTest,
            fakeRepository = fakeRepository,
            fakeAnalytics = fakeAnalytics,
        ).apply { block() }

        // Validate all fakes
        fakeRepository.ensureAllEventsConsumed()
        fakeAnalytics.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val systemUnderTest: MyFeature,
        val fakeRepository: FakeRepository,
        val fakeAnalytics: FakeAnalytics,
    )
}
```

**Key Features:**
- `runScenario` replaces `runTest` as the test entry point
- Default parameters for all configuration make tests concise
- Trailing lambda provides DSL-like syntax with scenario fields
- `ensureAllEventsConsumed()` called automatically after test block
- Scenario class holds system under test and all fakes

### Using runScenario in Tests

```kotlin
@Test
fun `fetching data returns success when repository succeeds`() = runScenario {
    // Configure fake behavior
    fakeRepository.dataResult = Result.success(testData)

    // Execute
    val result = systemUnderTest.fetchData()

    // Verify
    assertThat(result.isSuccess).isTrue()
    assertThat(fakeRepository.fetchCalls.awaitItem()).isEqualTo(FetchCall(userId = "123"))
}
// ensureAllEventsConsumed called automatically
```

## Turbine Flow Testing

Use Turbine's `.test { }` to assert Flow emissions:

```kotlin
@Test
fun `state updates when data changes`() = runScenario {
    systemUnderTest.state.test {
        // Initial state
        assertThat(awaitItem()).isEqualTo(State.Loading)

        // Trigger change
        fakeRepository.emit(newData)
        assertThat(awaitItem()).isEqualTo(State.Loaded(newData))

        // No more events expected
        ensureAllEventsConsumed()
    }
}
```

**Common Turbine operations:**
- `awaitItem()` — wait for next emission
- `expectNoEvents()` — assert no emissions occurred
- `ensureAllEventsConsumed()` — verify no unconsumed events remain
- `skipItems(n)` — skip past emissions you don't care about

## Quick Reference

| What | Pattern |
|------|---------|
| Test entry point | `fun \`test name\`() = runScenario { }` |
| Assertions | `assertThat(actual).isEqualTo(expected)` |
| Flow testing | `flow.test { assertThat(awaitItem()).isEqualTo(x) }` |
| Fake call tracking | `assertThat(fake.calls.awaitItem()).isEqualTo(call)` |
| Fake validation | `ensureAllEventsConsumed()` — automatic in runScenario |
| Compose UI tests | Invoke `compose-tests` skill |
| Creating fakes | Invoke `create-fake` skill |
| NetworkRule integration tests | Invoke `network-tests` skill |

## Concurrency Testing with Real I/O

When testing coroutines that involve real network I/O (NetworkRule/OkHttp), use `runTest` + `testScheduler.advanceUntilIdle()` + `async`/`await()`.

### Asserting StateFlow emission sequences (use Turbine)

When you need to verify a StateFlow's transitions during an async operation, use Turbine's `.test { }` to assert the full sequence:

```kotlin
@Test
fun `loading state transitions during mutation`() = runTest {
    val holdResponse = CountDownLatch(1)
    networkRule.enqueue(host("api.stripe.com"), method("POST"), path("/v1/...")) { response ->
        holdResponse.await(10, TimeUnit.SECONDS)
        response.setBody("{}")
    }

    systemUnderTest.isLoading.test {
        assertThat(awaitItem()).isFalse()

        val job = async { systemUnderTest.mutate() }
        testScheduler.advanceUntilIdle()

        assertThat(awaitItem()).isTrue()

        holdResponse.countDown()
        job.await()

        assertThat(awaitItem()).isFalse()
    }
}
```

### Asserting operations are queued (use CountDownLatch)

When you need to verify that concurrent operations are serialized by a mutex, use CountDownLatch to observe request ordering:

```kotlin
@Test
fun `concurrent operations are serialized by mutex`() = runTest {
    val holdFirstResponse = CountDownLatch(1)
    val secondRequestArrived = CountDownLatch(1)

    networkRule.enqueue(host("api.stripe.com"), method("POST"), path("/v1/...")) { response ->
        holdFirstResponse.await(10, TimeUnit.SECONDS)
        response.setBody("{}")
    }
    networkRule.enqueue(host("api.stripe.com"), method("POST"), path("/v1/...")) { response ->
        secondRequestArrived.countDown()
        response.setBody("{}")
    }

    val jobB = async { systemUnderTest.operationB() }
    testScheduler.advanceUntilIdle()

    val jobA = async { systemUnderTest.operationA() }
    testScheduler.advanceUntilIdle()

    assertThat(secondRequestArrived.count).isEqualTo(1) // A hasn't fired

    holdFirstResponse.countDown()
    jobB.await()
    jobA.await()

    assertThat(secondRequestArrived.count).isEqualTo(0) // A did fire
}
```

### Rules

- `async` + `await()` over `launch` + `join()` — `await()` propagates exceptions
- `testScheduler.advanceUntilIdle()` over `Thread.sleep` — deterministic, advances to suspension point
- After releasing a latch, use `await()` (not `advanceUntilIdle()` alone) — OkHttp delivers continuations asynchronously
- Always pass a timeout to `CountDownLatch.await()` inside mock handlers — prevents hangs
- Assert both negative (didn't happen during hold) AND positive (did happen after release)
- Turbine `.test { }` for StateFlow emission sequences; `CountDownLatch` for request ordering

## Common Mistakes

- **Using mocks instead of fakes** — always create `FakeClassName` implementations
- **Forgetting `ensureAllEventsConsumed()`** — runScenario handles this, but if using `runTest` directly, call it manually
- **Testing implementation details** — test behavior (inputs → outputs), not internal method calls
- **Missing edge cases** — null values, empty lists, blank strings, error paths
- **Using `Thread.sleep` in tests** — use `testScheduler.advanceUntilIdle()` instead
- **Testing stdlib behavior** — don't test that `HashMap.clear()` works
- **Vacuous assertions** — assert the pre-condition exists before testing its removal
