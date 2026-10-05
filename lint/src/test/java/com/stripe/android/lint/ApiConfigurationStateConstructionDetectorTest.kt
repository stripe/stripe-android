package com.stripe.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class ApiConfigurationStateConstructionDetectorTest {

    @Test
    fun `reports direct ApiConfiguration State construction in production source`() {
        lint().files(
            apiConfiguration,
            kotlin(
                """
                    package com.stripe.android.example

                    import com.stripe.android.core.ApiConfiguration

                    fun createConfiguration() = ApiConfiguration.State("pk_test_123", "acct_123")
                """
            ).indented().to("src/main/java/com/stripe/android/example/Example.kt")
        )
            .issues(ApiConfigurationStateConstructionDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expectMatches("ApiConfigurationStateConstruction")
    }

    @Test
    fun `reports a constructor call inside a lambda only once`() {
        lint().files(
            apiConfiguration,
            kotlin(
                """
                    package com.stripe.android.example

                    import com.stripe.android.core.ApiConfiguration

                    val configurationProvider = {
                        ApiConfiguration.State("pk_test_123", null)
                    }
                """
            ).indented().to("src/main/java/com/stripe/android/example/Example.kt")
        )
            .issues(ApiConfigurationStateConstructionDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun `does not report ApiConfiguration State construction in test source sets`() {
        lint().files(
            apiConfiguration,
            kotlin(
                """
                    package com.stripe.android.example

                    import com.stripe.android.core.ApiConfiguration

                    fun createConfiguration() = ApiConfiguration.State("pk_test_123", "acct_123")
                """
            ).indented().to("src/test/java/com/stripe/android/example/ExampleTest.kt"),
            kotlin(
                """
                    package com.stripe.android.example

                    import com.stripe.android.core.ApiConfiguration

                    fun createConfiguration() = ApiConfiguration.State("pk_test_123", "acct_123")
                """
            ).indented().to("src/androidTest/java/com/stripe/android/example/ExampleAndroidTest.kt")
        )
            .issues(ApiConfigurationStateConstructionDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `does not report construction of unrelated State classes`() {
        lint().files(
            kotlin(
                """
                    package com.stripe.android.example

                    data class State(val value: String)

                    fun createState() = State("value")
                """
            ).indented().to("src/main/java/com/stripe/android/example/Example.kt")
        )
            .issues(ApiConfigurationStateConstructionDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `does not report use or implementation of the ApiConfiguration builder`() {
        lint().files(
            apiConfiguration,
            kotlin(
                """
                    package com.stripe.android.example

                    import com.stripe.android.core.ApiConfiguration

                    fun createConfiguration() = ApiConfiguration("pk_test_123")
                        .stripeAccountId("acct_123")
                        .build()
                """
            ).indented().to("src/main/java/com/stripe/android/example/Example.kt")
        )
            .issues(ApiConfigurationStateConstructionDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    private companion object {
        val apiConfiguration = kotlin(
            """
                package com.stripe.android.core

                class ApiConfiguration(private val publishableKey: String) {
                    private var stripeAccountId: String? = null

                    fun stripeAccountId(value: String?) = apply { stripeAccountId = value }

                    fun build() = State(publishableKey, stripeAccountId)

                    data class State(val publishableKey: String, val stripeAccountId: String?)
                }
            """
        ).indented().to("src/main/java/com/stripe/android/core/ApiConfiguration.kt")
    }
}
