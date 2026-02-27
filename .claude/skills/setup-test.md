# Setting Up Tests

This skill describes how to structure tests in the Stripe Android SDK using fakes, scenarios, and proper verification patterns.

## Core Principles

1. **Use fakes over mocks** - Leverage fake implementations for dependencies (see `create-fake.md`)
2. **Create test scenarios** - Use Scenario classes with `runScenario` functions to organize test setup
3. **Verify all events consumed** - Call `validate()` or `ensureAllEventsConsumed()` on fakes after test block
4. **Use Turbine for Flow testing** - Test Flow emissions with Turbine's `.test { }` syntax

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
            config = config
        )

        // Run test block with scenario context
        block(
            Scenario(
                systemUnderTest = systemUnderTest,
                fakeRepository = fakeRepository,
                fakeAnalytics = fakeAnalytics,
            )
        )

        // Validate all fakes
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
