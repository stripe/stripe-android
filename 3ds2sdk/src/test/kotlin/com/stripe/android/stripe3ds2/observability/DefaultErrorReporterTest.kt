package com.stripe.android.stripe3ds2.observability

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.SdkVersion.VERSION_NAME
import com.stripe.android.stripe3ds2.transaction.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.json.JSONObject
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultErrorReporterTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val reporter = DefaultErrorReporter(
        ApplicationProvider.getApplicationContext(),
        config = Stripe3ds2ErrorReporterConfig(ChallengeMessageFixtures.SDK_TRANS_ID),
        workContext = testDispatcher,
        sentryConfig = FakeSentryConfig(),
        environment = "debug",
        localeCountry = "US",
        osVersion = Build.VERSION_CODES.R
    )

    @Test
    fun `createRequestBody should return expected value`() {
        val requestBody = reporter.createRequestBody(
            RuntimeException("testing 1 2 3")
        )
        assertThat(requestBody.keys().asSequence().toSet())
            .containsExactly("tags", "exception", "contexts", "release")

        assertThat(
            requestBody.getJSONObject("tags").toString()
        ).isEqualTo(
            JSONObject(
                """
                {
                    "locale": "US",
                    "environment": "debug",
                    "android_os_version": 30,
                    "sdk_transaction_id": "${ChallengeMessageFixtures.SDK_TRANS_ID}"
                }
                """.trimIndent()
            ).toString()
        )
    }

    @Test
    fun `createRequestStacktrace should return expected value`() {
        val frames = reporter.createRequestStacktrace(
            RuntimeException("testing 1 2 3")
        ).getJSONArray("frames")

        assertThat(frames.length())
            .isGreaterThan(10)

        val lastFrame = frames.getJSONObject(frames.length() - 1)
        assertThat(lastFrame.getString("filename"))
            .isEqualTo(this::class.java.name)
    }

    @Test
    fun `createSentryAuthHeader should return expected value`() {
        assertThat(
            reporter.createSentryAuthHeader()
        ).isEqualTo(
            "Sentry sentry_key=abc, sentry_version=7, sentry_timestamp=1600103536.978037, " +
                "sentry_client=Android3ds2Sdk $VERSION_NAME, sentry_secret=def"
        )
    }

    @Test
    fun `createRequestContexts should return expected value`() {
        val contexts = reporter.createRequestContexts()
        assertThat(contexts.keys().asSequence().toSet())
            .containsExactly("app", "os", "device")
    }

    @Test
    fun `exception should be logged`() {
        val logger = mock<Logger>()
        val failingReporter = DefaultErrorReporter(
            ApplicationProvider.getApplicationContext(),
            config = object : DefaultErrorReporter.Config {
                override val customTags: Map<String, String>
                    get() = fail("Exception while creating tags.")
            },
            workContext = testDispatcher,
            sentryConfig = FakeSentryConfig(),
            logger = logger
        )

        failingReporter.reportError(RuntimeException("test"))
        verify(logger).error(
            eq("Failed to send error report."),
            any<AssertionError>()
        )
    }

    private class FakeSentryConfig : SentryConfig {
        override val projectId: String = "123"
        override val key: String = "abc"
        override val version: String = "7"
    }
}
