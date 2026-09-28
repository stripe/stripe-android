---
name: screenshot-tests
description: Use when writing or running Paparazzi screenshot tests in stripe-android — covers PaparazziRule setup, recording/verifying commands, and test structure
---

# Screenshot Tests (Paparazzi)

We use [Paparazzi](https://github.com/cashapp/paparazzi) for screenshot testing via the `screenshot-testing` module.

## Recording Screenshots

Record (update) screenshots after UI changes:

```bash
./gradlew :<module>:recordPaparazziDebug
```

To record a specific test class only:

```bash
./gradlew :<module>:recordPaparazziDebug --tests "com.stripe.android.package.MyScreenshotTest"
```

Wildcard form also works:

```bash
./gradlew :<module>:recordPaparazziDebug --tests "*.MyScreenshotTest"
```

New or updated screenshots are written to `<module>/src/test/snapshots/images/` and added to the Git staging area.

## Verifying Screenshots

Verify screenshots match recorded baselines (used on CI):

```bash
./gradlew :<module>:verifyPaparazziDebug
```

Failure diffs are written to `<module>/out/failures` (git-ignored).

## Test Class Structure

```kotlin
internal class MyScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,        // Light/Dark
        PaymentSheetAppearance.entries,  // Default/Custom/Crazy appearances
        FontSize.entries,                // Default/Large font
        boxModifier = Modifier.padding(horizontal = 16.dp),
    )

    @Test
    fun testDefault() {
        paparazziRule.snapshot {
            MyComposable(/* ... */)
        }
    }
}
```

- `PaparazziRule` generates all permutations of the config options automatically.
- Each test produces one snapshot per permutation (e.g., 12 images for 2 themes x 3 appearances x 2 font sizes).
- Snapshots are named `<package>_<class>_<method>[<permutation>].png`.

## Available Config Options

| Option | Values | Import |
|--------|--------|--------|
| `SystemAppearance` | `LightTheme`, `DarkTheme` | `com.stripe.android.screenshottesting` |
| `FontSize` | `DefaultFont`, `LargeFont` | `com.stripe.android.screenshottesting` |
| `PaymentSheetAppearance` | `DefaultAppearance`, `CustomAppearance`, `CrazyAppearance` | `com.stripe.android.utils.screenshots` |

## Common Mistakes

- **Using `-Precord` or other flags** — Paparazzi uses a separate `recordPaparazziDebug` task, not a project property.
- **Running `testDebugUnitTest` to record** — this only _verifies_ against existing snapshots; use `recordPaparazziDebug` to update them.
- **Forgetting to commit updated snapshots** — recorded images go into `src/test/snapshots/images/` and must be committed.
