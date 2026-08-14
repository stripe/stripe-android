---
name: parameterized-tests
description: Use when writing, converting, or reviewing parameterized JVM, Robolectric, instrumentation, or Paparazzi tests in stripe-android. Covers TestParameterInjector runners, source-set dependencies, method versus class parameters, value providers, case matrices, and stable test names.
---

# Parameterized Tests

Use TestParameterInjector to give each case an independent test result while keeping repeated behavior in one test method.

## Select the runner and dependency

Declare TestParameterInjector directly in the module and source set that compiles the test:

```groovy
testImplementation testLibs.testParameterInjector
androidTestImplementation testLibs.testParameterInjector
```

Use only the declaration needed by that source set. Do not rely on another module or source set to provide it transitively.

| Test type | Runner |
| --- | --- |
| JVM, instrumentation, or Paparazzi | `TestParameterInjector` |
| Robolectric JVM | `RobolectricTestParameterInjector` |

```kotlin
@RunWith(TestParameterInjector::class)
internal class FeeCalculatorTest

@RunWith(RobolectricTestParameterInjector::class)
internal class FeeCalculatorRobolectricTest
```

Import `RobolectricTestParameterInjector` from `org.robolectric`. Import the other runner and annotations from `com.google.testing.junit.testparameterinjector`.

## Put focused cases on the test method

Parameterize the method when the cases apply to one test. This keeps unrelated tests from running across the same matrix.

```kotlin
@Test
fun `calculates the expected fee`(
    @TestParameter(valuesProvider = FeeCaseProvider::class)
    testCase: FeeCase,
) {
    assertThat(calculateFee(testCase.input)).isEqualTo(testCase.expected)
}
```

Use a class field or constructor parameter only when every test in the class intentionally shares that parameter matrix.

## Choose the smallest value source

Use enum and boolean shorthand when every value is meaningful:

```kotlin
@Test
fun `updates state`(
    @TestParameter mode: Mode,
    @TestParameter enabled: Boolean,
) {
    // Runs the Cartesian product of every mode and boolean value.
}
```

Use one curated case object when only specific combinations are valid or when inputs and expected outputs belong together. Multiple `@TestParameter` arguments produce a Cartesian product.

Use the modern `TestParameterValuesProvider`, not the deprecated nested provider interface:

```kotlin
internal object FeeCaseProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: TestParameterValuesProvider.Context?,
    ): List<FeeCase> = listOf(
        FeeCase(name = "Standard", input = 100, expected = 3),
        FeeCase(name = "Expedited", input = 100, expected = 8),
    )
}

internal data class FeeCase(
    val name: String,
    val input: Int,
    val expected: Int,
) {
    override fun toString(): String = name
}
```

Give curated cases stable, readable names. Override `toString()` when the framework should derive test identifiers from the case.

## Check the result

1. Run the focused parameterized test class and confirm the expected case count and names.
2. Run the module's required static and API checks.
3. Run `git diff --check`.
