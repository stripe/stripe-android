---
name: network-tests
description: Use when writing NetworkRule integration tests in stripe-android — covers testBodyFromFile, inline JSON modification, request matchers, and fixture patterns
---
# NetworkRule Integration Tests

For instrumentation tests that need mocked network responses, use `NetworkRule` (from `network-testing` module) with `testBodyFromFile`. JSON fixture files live in the module's `src/androidTest/resources/` directory (e.g., `paymentsheet/src/androidTest/resources/checkout-session-init.json`) and are resolved by filename from the resources root.

## Basic Structure

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

## Prefer Inline JSON Modification Over New Fixture Files

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

## Composing Multiple Modifications

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

## Extracting Shared Modifiers

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

## testBodyFromFile Variants

| Signature | Use when |
|-----------|----------|
| `testBodyFromFile("file.json")` | No modifications needed |
| `testBodyFromFile("file.json") { json -> ... }` | Modifying JSON fields inline |
| `testBodyFromFile("file.json", replacements)` | String-level find/replace with `ResponseReplacement` |

## Request Matchers

Use `RequestMatchers` (from `com.stripe.android.networktesting.RequestMatchers`) to validate request body parameters. Import the matchers you need statically:

```kotlin
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.not

networkRule.checkoutConfirm(
    bodyPart("expected_amount", "5099"),
    not(hasBodyPart("save_payment_method")),
) { response ->
    response.testBodyFromFile("checkout-session-confirm.json")
}
```
