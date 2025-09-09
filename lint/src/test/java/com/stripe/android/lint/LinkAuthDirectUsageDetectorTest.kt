package com.stripe.android.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class LinkAuthDirectUsageDetectorTest {

    @Test
    fun `detect LinkAuth import in regular class`() {
        lint()
            .files(
                kotlin(
                    """
                    package com.example
                    
                    import com.stripe.android.link.account.LinkAuth
                    
                    class SomeViewModel {
                        fun doSomething() {
                            // test
                        }
                    }
                    """.trimIndent()
                )
            )
            .issues(LinkAuthDirectUsageDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expect(
                """
                src/com/example/SomeViewModel.kt:3: Error: Do not import LinkAuth directly. Use LinkAccountManager.lookup() or .signup() instead. [LinkAuthDirectUsage]
                import com.stripe.android.link.account.LinkAuth
                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
    }
}
