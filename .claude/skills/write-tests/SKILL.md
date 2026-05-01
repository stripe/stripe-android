---
name: write-tests
description: Use when writing or structuring tests in stripe-android — covers runScenario pattern, fakes, Turbine Flow testing, Truth assertions, and NetworkRule integration tests with testBodyFromFile
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

## NetworkRule Integration Tests

For instrumentation tests that need mocked network responses, use `NetworkRule` with `testBodyFromFile`. JSON fixture files live alongside the test code in `src/androidTest/resources/`.

### Basic Structure

```kotlin
@RunWith(AndroidJUnit4::class)
internal class MyFeatureTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val networkRule = testRules.networkRule

    @Test
    fun testSomething() = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(requestMatcher) { response ->
            response.testBodyFromFile("my-fixture.json")
        }
        // ... test actions ...
    }
}
```

### Prefer Inline JSON Modification Over New Fixture Files

When a test needs a modified JSON response, use the `testBodyFromFile` lambda to modify the base fixture inline — do NOT create a separate JSON file for each variation.

```kotlin
// GOOD: Modify the base fixture inline
networkRule.checkoutInit { response ->
    response.testBodyFromFile("checkout-session-init.json") { json ->
        json.put("customer_email", "session@example.com")
    }
}

// BAD: Creating checkout-session-init-with-email.json with one field different
networkRule.checkoutInit { response ->
    response.testBodyFromFile("checkout-session-init-with-email.json")
}
```

This keeps the fixture set minimal and makes the test-specific modifications explicit at the call site.

### Composing Multiple Modifications

The lambda receives a `JSONObject` — use standard `org.json` methods to add or modify fields:

```kotlin
networkRule.checkoutInit { response ->
    response.testBodyFromFile("checkout-session-init.json") { json ->
        json.put("customer", JSONObject("""
            {
                "id": "cus_12345",
                "payment_methods": [],
                "can_detach_payment_method": true
            }
        """.trimIndent()))
        json.put("customer_managed_saved_payment_methods_offer_save", JSONObject("""
            {"enabled": true, "status": "not_accepted"}
        """.trimIndent()))
    }
}
```

For nested modifications, chain `getJSONObject()`:

```kotlin
response.testBodyFromFile("checkout-session-init.json") { json ->
    json.getJSONObject("server_built_elements_session_params")
        .getJSONObject("deferred_intent")
        .put("setup_future_usage", "off_session")
}
```

### Extracting Shared Modifiers

When multiple tests share the same JSON modification, extract the lambda as a parameter:

```kotlin
private fun runMyTest(
    jsonModifier: (JSONObject) -> Unit = {},
) = runPaymentSheetTest(networkRule = networkRule, resultCallback = ::assertCompleted) { testContext ->
    networkRule.checkoutInit { response ->
        response.testBodyFromFile("checkout-session-init.json", jsonModifier)
    }
    // ... shared test logic ...
}

@Test
fun testWithSfu() = runMyTest { json ->
    json.getJSONObject("server_built_elements_session_params")
        .getJSONObject("deferred_intent")
        .put("setup_future_usage", "off_session")
}
```

### testBodyFromFile Variants

| Signature | Use when |
|-----------|----------|
| `testBodyFromFile("file.json")` | No modifications needed |
| `testBodyFromFile("file.json") { json -> ... }` | Modifying JSON fields inline |
| `testBodyFromFile("file.json", replacements)` | String-level find/replace with `ResponseReplacement` |

### Request Matchers

Use `RequestMatchers` to validate request body parameters:

```kotlin
networkRule.checkoutConfirm(
    bodyPart("expected_amount", "5099"),
    not(hasBodyPart("save_payment_method")),
) { response ->
    response.testBodyFromFile("checkout-session-confirm.json")
}
```

## Common Mistakes

- **Using mocks instead of fakes** — always create `FakeClassName` implementations
- **Forgetting `ensureAllEventsConsumed()`** — runScenario handles this, but if using `runTest` directly, call it manually
- **Testing implementation details** — test behavior (inputs → outputs), not internal method calls
- **Missing edge cases** — null values, empty lists, blank strings, error paths
