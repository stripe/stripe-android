---
name: write-tests
description: Use when writing or structuring unit tests in stripe-android — covers runScenario pattern, fakes, Turbine Flow testing, and Truth assertions
---

# Writing Tests

Structure tests in the Stripe Android SDK using fakes, scenarios, and proper verification patterns.

## Core Principles

1. **Fakes over mocks** — use fake implementations for dependencies (invoke `create-fake` skill)
2. **runScenario pattern** — use Scenario classes with `runScenario` functions to organize setup
3. **Verify all events consumed** — call `ensureAllEventsConsumed()` on fakes after test block
4. **Truth assertions** — always use `assertThat(actual).isEqualTo(expected)` from Google Truth
5. **Turbine for Flows** — test Flow emissions with `.test { }` syntax

## Test Strategy

### Priority by Risk
1. **Critical**: Payment flows, security, API contract correctness
2. **High**: Multi-module integration, backward compatibility, error handling
3. **Medium**: UI components, data processing, standard features
4. **Low**: Utility functions, simple data models — basic happy-path sufficient

### Android-Specific Concerns
- **Process death**: Test state save/restore via `SavedStateHandle`
- **Configuration changes**: Orientation, dark mode, locale changes
- **API level compatibility**: Test on min and target SDK versions

## Test File Conventions

- Name: `<SourceClass>Test.kt`
- Location: `src/test/java/` (unit), `src/androidTest/java/` (instrumentation)
- For Compose UI tests, invoke the `compose-tests` skill

## runScenario Pattern

Every test class should define its own `runScenario` function and `Scenario` class:

```kotlin
class MyFeatureTest {
    @Test
    fun `test case`() = runScenario {
        assertThat(systemUnderTest.getValue()).isEqualTo(expectedValue)
    }

    @Test
    fun `test with custom config`() = runScenario(
        config = customConfig,
    ) {
        fakeRepository.dataResult = Result.success(testData)

        val result = systemUnderTest.fetchData()

        assertThat(result.isSuccess).isTrue()
        assertThat(fakeRepository.fetchCalls.awaitItem()).isEqualTo(FetchCall(userId = "123"))
    }

    // runScenario and Scenario at the bottom of the file
    private fun runScenario(
        config: Config = defaultConfig,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakeRepository = FakeRepository()
        val fakeAnalytics = FakeAnalytics()

        val systemUnderTest = MyFeature(
            repository = fakeRepository,
            analytics = fakeAnalytics,
            config = config,
        )

        Scenario(
            systemUnderTest = systemUnderTest,
            fakeRepository = fakeRepository,
            fakeAnalytics = fakeAnalytics,
        ).apply { block() }

        fakeRepository.ensureAllEventsConsumed()
        fakeAnalytics.ensureAllEventsConsumed()
    }

    private class Scenario(
        val systemUnderTest: MyFeature,
        val fakeRepository: FakeRepository,
        val fakeAnalytics: FakeAnalytics,
    )
}
```

**Key points:**
- `runScenario` wraps `runTest` — it is the test entry point
- Default parameters for all configuration keep tests concise
- `ensureAllEventsConsumed()` called automatically after test block
- Each test class defines its own `runScenario` tailored to its dependencies

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

## Common Mistakes

- **Using mocks instead of fakes** — always create `FakeClassName` implementations
- **Forgetting `ensureAllEventsConsumed()`** — runScenario handles this, but if using `runTest` directly, call it manually
- **Testing implementation details** — test behavior (inputs → outputs), not internal method calls
- **Missing edge cases** — null values, empty lists, blank strings, error paths
