package com.stripe.android.identity.analytics

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.networking.toMap
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.CameraSource
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.CAMERA_ACCESS_STATE_DENIED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.DOC_BACK
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_CAMERA_PERMISSION_DENIED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_EXPERIMENT_EXPOSURE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_GENERIC_ERROR
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SHEET_CLOSED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_ARB_ID
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_CAMERA_ACCESS_STATE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_CAMERA_EVENT_KIND
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_CAMERA_SOURCE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_ERROR_CONTEXT
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_ERROR_DETAILS
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EVENT_META_DATA
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EXCEPTION
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EXPERIMENT_RETRIEVED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_LAST_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_LIVE_MODE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCAN_TYPE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SESSION_RESULT
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentExperiment
import com.stripe.android.identity.states.IdentityScanState
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
        factory.sheetClosed(sessionResult, lastScreenName = TEST_SCREEN_NAME)

        verify(mockIdentityRepository).sendAnalyticsRequest(
            argWhere {
                val metadata = it.params.toMap()[PARAM_EVENT_META_DATA] as Map<*, *>
                it.eventName == EVENT_SHEET_CLOSED &&
                    metadata[PARAM_SESSION_RESULT] == sessionResult &&
                    metadata[PARAM_LAST_SCREEN_NAME] == TEST_SCREEN_NAME &&
                    metadata[PARAM_LIVE_MODE] == "true"
            }
        )
    }

    @Test
    fun testCameraPermissionDeniedIncludesMetadata() = runBlocking {
        factory.verificationPage = liveModePage

        factory.cameraPermissionDenied(
            screenName = TEST_SCREEN_NAME,
            cameraSource = CameraSource.CAMERA_SESSION,
            isGranted = false
        )

        verify(mockIdentityRepository).sendAnalyticsRequest(
            argWhere {
                val metadata = it.params.toMap()[PARAM_EVENT_META_DATA] as Map<*, *>
                it.eventName == EVENT_CAMERA_PERMISSION_DENIED &&
                    metadata[PARAM_SCREEN_NAME] == TEST_SCREEN_NAME &&
                    metadata[PARAM_CAMERA_SOURCE] == CameraSource.CAMERA_SESSION.analyticsValue &&
                    metadata[PARAM_CAMERA_EVENT_KIND] ==
                    IdentityAnalyticsRequestFactory.CameraEventKind.PERMISSION.analyticsValue &&
                    metadata[PARAM_CAMERA_ACCESS_STATE] == CAMERA_ACCESS_STATE_DENIED
            }
        )
    }

    @Test
    fun testGenericErrorIncludesAdditionalMetadataAndErrorDetails() = runBlocking {
        factory.verificationPage = liveModePage
        val error = IllegalStateException("boom")

        factory.genericError(
            throwable = error,
            additionalMetadata = mapOf(
                PARAM_ERROR_CONTEXT to IdentityAnalyticsRequestFactory.ERROR_CONTEXT_ERROR_SCREEN,
                PARAM_SCREEN_NAME to TEST_SCREEN_NAME
            )
        )

        verify(mockIdentityRepository).sendAnalyticsRequest(
            argWhere {
                val metadata = it.params.toMap()[PARAM_EVENT_META_DATA] as Map<*, *>
                val errorDetails = metadata[PARAM_ERROR_DETAILS] as Map<*, *>
                it.eventName == EVENT_GENERIC_ERROR &&
                    metadata[PARAM_ERROR_CONTEXT] ==
                    IdentityAnalyticsRequestFactory.ERROR_CONTEXT_ERROR_SCREEN &&
                    metadata[PARAM_SCREEN_NAME] == TEST_SCREEN_NAME &&
                    errorDetails[PARAM_EXCEPTION] == IllegalStateException::class.java.name
            }
        )
    }

    @Test
    fun testDocBackUsesDocBackAnalyticsValue() = runBlocking {
        factory.verificationPage = liveModePage

        factory.screenPresented(
            IdentityScanState.ScanType.DOC_BACK,
            TEST_SCREEN_NAME
        )

        verify(mockIdentityRepository).sendAnalyticsRequest(
            argWhere {
                val metadata = it.params.toMap()[PARAM_EVENT_META_DATA] as Map<*, *>
                it.eventName == EVENT_SCREEN_PRESENTED &&
                    metadata[PARAM_SCAN_TYPE] == DOC_BACK
            }
        )
    }

    @Test
    fun testExperimentWithEventMatchedLogged() = runBlocking {
        factory.verificationPage = mockPageScreenPresented
        factory.screenPresented(
            IdentityScanState.ScanType.DOC_FRONT,
            TEST_SCREEN_NAME
        )

        verify(mockIdentityRepository).sendAnalyticsRequest(
            argWhere {
                it.eventName == EVENT_SCREEN_PRESENTED
            }
        )

        verify(mockIdentityRepository).sendAnalyticsRequest(
            argWhere {
                it.eventName == EVENT_EXPERIMENT_EXPOSURE &&
                    it.params.toMap()[PARAM_ARB_ID] == USER_SESSION_ID &&
                    it.params.toMap()[PARAM_EXPERIMENT_RETRIEVED] == EXP1
            }
        )
    }

    @Test
    fun testExperimentWithoutEventMatchedNotLogged() = runBlocking {
        factory.verificationPage = mockPageUnmatchedEvent
        factory.screenPresented(
            IdentityScanState.ScanType.DOC_FRONT,
            TEST_SCREEN_NAME
        )

        verify(mockIdentityRepository).sendAnalyticsRequest(
            argWhere {
                it.eventName == EVENT_SCREEN_PRESENTED
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
