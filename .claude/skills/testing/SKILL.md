---
name: testing
description: Use when writing tests, running tests, modifying test code, or choosing test patterns for this project
---

# Testing

## Running Tests

- `./gradlew test` — Run unit tests for all modules
- `./gradlew testDebugUnitTest` — Run debug unit tests only
- `./gradlew connectedAndroidTest` — Run instrumentation tests (requires device)
- `./gradlew :<module>:test` — Run all tests for a specific module
- `./gradlew :<module>:testDebugUnitTest` — Run debug unit tests for a specific module
  - Module names from settings.gradle: `:payments-core`, `:paymentsheet`, `:financial-connections`, `:identity`, `:connect`, etc.
  - Example: `./gradlew :payments-core:testDebugUnitTest`

## Testing Stack

- Unit tests with JUnit, **fakes** (preferred over mocks), and Truth assertions
- **Turbine** for Flow testing and for call tracking/verification in fakes
- Instrumentation tests using Espresso and AndroidX Test
- Robolectric for Android unit tests
- Screenshot testing with Paparazzi and custom screenshot-testing module
- Compose UI tests with `createComposeRule()`

## Testing Patterns

**Fakes over mocks** — Create `FakeClassName` implementations when possible. Use mocks or indirect testing only when necessary.

**`runScenario` pattern** — Define a private `runScenario` function per test class that encapsulates fixture setup, exposing dependencies via lambda parameters or a `Scenario` data class receiver:
- Simple tests: lambda parameters `runScenario { sut, fakeDep1, fakeDep2 -> ... }`
- Stateful/coroutine tests: `Scenario` data class + `runTest` integration
- Configurable tests: add parameters with defaults for behavioral variations

**Flow testing:** `flow.test { assertThat(awaitItem()).isEqualTo(expected) }`

**Compose testing:** `composeTestRule.onNodeWithText("text").assertIsDisplayed()`

**Truth assertions:** `assertThat(actual).isEqualTo(expected)`
