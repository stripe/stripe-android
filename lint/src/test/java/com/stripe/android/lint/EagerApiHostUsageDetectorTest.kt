package com.stripe.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class EagerApiHostUsageDetectorTest {

    @Test
    fun `should flag val without get() that uses API_HOST`() {
        lint().files(
            kotlin(
                """
                    package com.stripe.android.example

                    object ApiRequest {
                        var API_HOST_OVERRIDE: String? = null
                        val API_HOST: String get() = API_HOST_OVERRIDE ?: "https://api.stripe.com"
                    }

                    class MyRepository {
                        companion object {
                            private val MY_URL = "${'$'}{ApiRequest.API_HOST}/v1/foo"
                        }
                    }
                """
            ).indented()
        )
            .issues(EagerApiHostUsageDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expectMatches("EagerApiHostUsage")
    }

    @Test
    fun `should not flag val with get() that uses API_HOST`() {
        lint().files(
            kotlin(
                """
                    package com.stripe.android.example

                    object ApiRequest {
                        var API_HOST_OVERRIDE: String? = null
                        val API_HOST: String get() = API_HOST_OVERRIDE ?: "https://api.stripe.com"
                    }

                    class MyRepository {
                        companion object {
                            private val MY_URL: String
                                get() = "${'$'}{ApiRequest.API_HOST}/v1/foo"
                        }
                    }
                """
            ).indented()
        )
            .issues(EagerApiHostUsageDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should not flag function that uses API_HOST`() {
        lint().files(
            kotlin(
                """
                    package com.stripe.android.example

                    object ApiRequest {
                        var API_HOST_OVERRIDE: String? = null
                        val API_HOST: String get() = API_HOST_OVERRIDE ?: "https://api.stripe.com"
                    }

                    class MyRepository {
                        companion object {
                            private fun getApiUrl(path: String): String {
                                return "${'$'}{ApiRequest.API_HOST}/v1/${'$'}path"
                            }
                        }
                    }
                """
            ).indented()
        )
            .issues(EagerApiHostUsageDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should not flag val that does not use API_HOST`() {
        lint().files(
            kotlin(
                """
                    package com.stripe.android.example

                    class MyRepository {
                        companion object {
                            private val MY_URL = "https://example.com/v1/foo"
                        }
                    }
                """
            ).indented()
        )
            .issues(EagerApiHostUsageDetector.ISSUE)
            .allowCompilationErrors()
            .allowMissingSdk()
            .run()
            .expectClean()
    }
}
