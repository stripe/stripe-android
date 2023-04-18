# stripe-ui-core screenshot tests

We use [Paparazzi](https://github.com/cashapp/paparazzi) to run screenshot tests.

## Creating screenshot tests

To create a screenshot test, create a new test class and add the `PaparazziRule`. This rule takes any number of configuration enums and runs each test with all possible permutations.

```kotlin
class MyNewScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        // Add others…
    )

    @Test
    fun testDefault() {
        paparazziRule.snapshot {
            // My UI goes here…
        }
    }
}
```

## Recording screenshots

To record screenshots, run `./gradlew <module-name>:recordPaparazziDebug`. This will record all screenshots of the specified module. New or updated screenshots will be added to your Git staging area.

If you only want to run a specific test class, add `--tests com.stripe.android.uicore.MyNewScreenshotTest`.

## Verifying screenshots

To verify screenshots, run `./gradlew <module-name>:verifyPaparazziDebug`. This is the command we use on CI.

In case of failures, this command will generate failure images that show the diff in `<module-name>/out/failures`. However, we exclude these from Git. 
