package com.stripe.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class ComposeCollectAsStateUsageDetectorTest {
    @Test
    fun `should detect disallowed usage of 'collectAsState' import from Compose Runtime library`() {
        lint().files(
            kotlin(
                """
                    package com.stripe.android.uicore.elements

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.collectAsState

                    @Composable
                    fun TextField(
                        textFieldController: TextFieldController,
                    ) {
                        val value by textFieldController.fieldValue.collectAsState()
                    }
                    """
            ).indented()
        )
            .issues(ComposeCollectAsStateUsageDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expect(
                """
                    src/com/stripe/android/uicore/elements/test.kt:4: Error: Do not use the [androidx.compose.runtime.collectAsState] composable function [ComposeCollectAsStateUsageIssue]
                    import androidx.compose.runtime.collectAsState
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun `should detect disallowed usage of named 'collectAsState' import from Compose Runtime library`() {
        lint().files(
            kotlin(
                """
                    package com.stripe.android.uicore.elements

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.collectAsState as collectAsNamedState

                    @Composable
                    fun TextField(
                        textFieldController: TextFieldController,
                    ) {
                        val value by textFieldController.fieldValue.collectAsNamedState()
                    }
                    """
            ).indented()
        )
            .issues(ComposeCollectAsStateUsageDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expect(
                """
                    src/com/stripe/android/uicore/elements/test.kt:4: Error: Do not use the [androidx.compose.runtime.collectAsState] composable function [ComposeCollectAsStateUsageIssue]
                    import androidx.compose.runtime.collectAsState as collectAsNamedState
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun `should not detect allowed usage of named 'collectAsState' import from stripe-ui-core library`() {
        lint().files(
            kotlin(
                """
                    package com.stripe.android.uicore.elements

                    import com.stripe.android.uicore.utils.collectAsState
                    import androidx.compose.runtime.Composable

                    @Composable
                    fun TextField(
                        textFieldController: TextFieldController,
                    ) {
                        val value by textFieldController.fieldValue.collectAsState()
                    }
                    """
            ).indented()
        )
            .issues(ComposeCollectAsStateUsageDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectClean()
    }
}
