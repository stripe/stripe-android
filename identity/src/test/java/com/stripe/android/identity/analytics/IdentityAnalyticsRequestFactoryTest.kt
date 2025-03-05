package com.stripe.android.identity.analytics

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.networking.toMap
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SHEET_CLOSED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EVENT_META_DATA
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_LIVE_MODE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SESSION_RESULT
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentExperiment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdentityAnalyticsRequestFactoryTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()
    private val mockIdentityRepository = mock<IdentityRepository>()
    private val mockArgs = mock<IdentityVerificationSheetContract.Args> {
        on { verificationSessionId }.thenReturn(VERIFICATION_SESSION)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val factory = IdentityAnalyticsRequestFactory(
        ApplicationProvider.getApplicationContext(),
        mockArgs,
        mockIdentityRepository,
        CoroutineScope(UnconfinedTestDispatcher())
    )

    private val mockPageScreenPresented = mock<VerificationPage> {
        on { userSessionId }.thenReturn(USER_SESSION_ID)
        on { experiments }.thenReturn(
            listOf(
                VerificationPageStaticContentExperiment(
                    experimentName = EXP1,
                    eventName = EVENT_SCREEN_PRESENTED,
                    eventMetadata = mapOf(
                        PARAM_SCREEN_NAME to TEST_SCREEN_NAME
                    )
                )
            )
        )
        on { livemode }.thenReturn(true)
    }

    private val mockPageUnmatchedEvent = mock<VerificationPage> {
        on { userSessionId }.thenReturn(USER_SESSION_ID)
        on { experiments }.thenReturn(
            listOf(
                VerificationPageStaticContentExperiment(
                    experimentName = EXP1,
                    eventName = "testEvent",
                    eventMetadata = mapOf(
                        "metadata" to "value"
                    )
                ),
            )
        )
        on { livemode }.thenReturn(true)
    }

    private val liveModePage = mock<VerificationPage> {
        on { livemode }.thenReturn(true)
    }

    @After
    fun clearVerificationPage() {
        factory.verificationPage = null
    }

    @Test
    fun testNoExperiment() = runBlocking {
        factory.verificationPage = liveModePage
        val sessionResult = "sessionResult"
        factory.sheetClosed(sessionResult)

        verify(mockIdentityRepository).sendAnalyticsRequest(
            argWhere {
                it.eventName == EVENT_SHEET_CLOSED &&
                    (it.params.toMap()[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SESSION_RESULT] == sessionResult &&
                    (it.params.toMap()[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_LIVE_MODE] == "true"
            }
        )
    }

    private companion object {
        const val USER_SESSION_ID = "userSessionId"
        const val EXP1 = "EXP1"
        const val TEST_SCREEN_NAME = "testScreenName"
        const val VERIFICATION_SESSION = "session1"
    }
}
