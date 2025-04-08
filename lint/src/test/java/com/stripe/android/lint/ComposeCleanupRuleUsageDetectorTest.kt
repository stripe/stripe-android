package com.stripe.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class ComposeCleanupRuleUsageDetectorTest {
    @Test
    fun `should not lint in Robolectric test without compose context`() {
        lint().files(
            kotlin(
                """
                    package com.stripe.android.uicore.elements

                    import org.junit.runner.RunWith
                    import org.robolectric.RobolectricTestRunner

                    @RunWith(RobolectricTestRunner::class)
                    class TestingWithCompose {}
                    """
            ).indented(),
        )
            .issues(ComposeCleanupRuleUsageDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should detect 'createComposeCleanupRule' not being used alongside 'createComposeRule' in Robolectric test`() {
        lint().files(
            kotlin(
                """
                    package com.stripe.android.uicore.elements

                    import org.junit.runner.RunWith
                    import org.robolectric.RobolectricTestRunner
                    import androidx.compose.ui.test.junit4.createComposeRule

                    @RunWith(RobolectricTestRunner::class)
                    class TestingWithCompose {}
                    """
            ).indented(),
        )
            .issues(ComposeCleanupRuleUsageDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expect(
                """
                    src/com/stripe/android/uicore/elements/TestingWithCompose.kt:5: Error: No cleanup rule found, please use createComposeCleanupRule alongside createComposeRule! [ComposeCleanupRuleUsageIssue]
                    import androidx.compose.ui.test.junit4.createComposeRule
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun `should not lint when 'createComposeCleanupRule' & 'createComposeRule' in Robolectric test`() {
        lint().files(
            kotlin(
                """
                    package com.stripe.android.uicore.elements

                    import org.junit.runner.RunWith
                    import org.robolectric.RobolectricTestRunner
                    import androidx.compose.ui.test.junit4.createComposeRule
                    import com.stripe.android.testing.createComposeCleanupRule

                    @RunWith(RobolectricTestRunner::class)
                    class TestingWithCompose {}
                    """
            ).indented(),
        )
            .issues(ComposeCleanupRuleUsageDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectClean()
    }
}
