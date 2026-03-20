---
name: compose-tests
description: Use when writing Compose UI tests in stripe-android — covers composeRule setup, Robolectric annotations, node assertions, and test tag patterns
---

# Compose UI Tests

Patterns for testing Jetpack Compose UI in the Stripe Android SDK.

## Test Class Setup

Every Compose test needs three rules and Robolectric:

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class MyComposableTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())
}
```

- `createComposeRule()` — provides the Compose test harness
- `createComposeCleanupRule()` — workaround for Robolectric resource leak (import from `com.stripe.android.testing`)
- `CoroutineTestRule` — controls coroutine dispatching in tests

## runScenario for Compose Tests

Use the same `runScenario` pattern from `write-tests`, wrapping `composeRule.setContent`:

```kotlin
@Test
fun `required fields are visible`() = runScenario {
    page.country.assertIsDisplayed()
    page.zipCode.assertIsDisplayed()
    page.line1.assertDoesNotExist()
}

@Test
fun `input changes update state`() = runScenario(
    addressCollectionMode = AddressCollectionMode.Full,
) {
    page.zipCode.performTextReplacement("94100")
    assertThat(formState?.postalCode?.value).isEqualTo("94100")
}

private fun runScenario(
    addressCollectionMode: AddressCollectionMode = AddressCollectionMode.Automatic,
    block: TestScenario.() -> Unit,
) {
    composeRule.setContent {
        MyComposableUI(addressCollectionMode = addressCollectionMode)
    }
    block(TestScenario(MyPage(composeRule)))
}

private data class TestScenario(val page: MyPage)
```

## Finding Nodes

| Finder | Use when |
|--------|----------|
| `onNodeWithTag(TAG)` | Element has a test tag (preferred) |
| `onNodeWithText("text")` | Finding by visible text |
| `onNodeWithContentDescription("desc")` | Finding by accessibility label |
| `onAllNodesWithTag(TAG)` | Multiple elements share a tag |
| `onNode(hasTestTag(TAG).and(hasText("x")))` | Combining matchers |

## Assertions

| Assertion | Purpose |
|-----------|---------|
| `assertIsDisplayed()` | Element is visible |
| `assertExists()` | Element exists (may not be visible) |
| `assertDoesNotExist()` | Element not in hierarchy |
| `assertIsNotDisplayed()` | Exists but not visible |
| `assertIsEnabled()` / `assertIsNotEnabled()` | Enabled state |
| `assertTextContains("text")` | Text field contains value |
| `assertContentDescriptionContains("desc")` | Accessibility description |
| `assert(hasText("text"))` | General matcher assertion |

## Actions

| Action | Purpose |
|--------|---------|
| `performClick()` | Tap element |
| `performTextReplacement("text")` | Set text field value |
| `performScrollTo()` | Scroll element into view |

## Test Tags

Define tags as constants in production code, use `Modifier.testTag()`:

```kotlin
// In production code:
internal const val SAVE_BUTTON_TEST_TAG = "save_button"

Box(modifier = Modifier.testTag(SAVE_BUTTON_TEST_TAG)) { ... }

// In test code:
composeRule.onNodeWithTag(SAVE_BUTTON_TEST_TAG).assertIsDisplayed()
```

## Waiting for Recomposition

```kotlin
// Wait for pending recompositions
composeRule.waitForIdle()

// Wait for async content with timeout
composeRule.waitUntil(timeoutMillis = 5000L) {
    composeRule.onAllNodesWithTag(MY_TAG).fetchSemanticsNodes().isNotEmpty()
}
```

## Common Mistakes

- **Missing `composeCleanupRule`** — causes resource leaks with Robolectric, flaky tests
- **Missing `@RunWith(RobolectricTestRunner::class)`** — Compose tests need Android framework
- **Using `onNodeWithText` for dynamic content** — prefer test tags for stability
- **Not calling `waitForIdle()` after state changes** — assertions may run before recomposition
