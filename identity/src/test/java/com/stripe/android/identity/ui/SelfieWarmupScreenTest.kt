package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_SELFIE_WARMUP
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.navigation.SelfieDestination
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class SelfieWarmupScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockNavController = mock<NavController>()
    private val mockScreenTracker = mock<ScreenTracker>()
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { screenTracker } doReturn mockScreenTracker
        on { verificationPage } doReturn MediatorLiveData(Resource.success(mock<VerificationPage>()))
    }

    @Test
    fun verifyContentVisibleAndButtonClick() {
        composeTestRule.setContent {
            SelfieWarmupScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel,
            )
        }

        with(composeTestRule) {
            onNodeWithTag(SELFIE_WARMUP_CONTENT_TAG).assertExists()

            onNodeWithTag(SELFIE_CONTINUE_BUTTON_TAG).onChildAt(0).performClick()
            verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_SELFIE_WARMUP), any())
            verify(mockIdentityViewModel).setSelfieTrainingConsent(eq(false))
            verify(mockNavController).navigate(
                eq(SelfieDestination.routeWithArgs),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun verifyTrainingConsentButtons() {
        val identityViewModel = mockIdentityViewModelWithTrainingConsent()

        composeTestRule.setContent {
            SelfieWarmupScreen(
                navController = mockNavController,
                identityViewModel = identityViewModel,
            )
        }

        with(composeTestRule) {
            onNodeWithTag(SELFIE_CONTINUE_BUTTON_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_ALLOW_BUTTON_TAG).onChildAt(0).performClick()
            verify(identityViewModel).setSelfieTrainingConsent(eq(true))
            verify(mockNavController).navigate(
                eq(SelfieDestination.routeWithArgs),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun verifyDeclineTrainingConsent() {
        val identityViewModel = mockIdentityViewModelWithTrainingConsent()

        composeTestRule.setContent {
            SelfieWarmupScreen(
                navController = mockNavController,
                identityViewModel = identityViewModel,
            )
        }

        with(composeTestRule) {
            onNodeWithTag(SELFIE_DECLINE_BUTTON_TAG).onChildAt(0).performClick()
            verify(identityViewModel).setSelfieTrainingConsent(eq(false))
            verify(mockNavController).navigate(
                eq(SelfieDestination.routeWithArgs),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    private fun mockIdentityViewModelWithTrainingConsent(): IdentityViewModel {
        val selfieCapturePage = mock<VerificationPageStaticContentSelfieCapturePage> {
            on { consentText } doReturn "training consent"
        }
        val verificationPage = mock<VerificationPage> {
            on { selfieCapture } doReturn selfieCapturePage
        }
        return mock {
            on { screenTracker } doReturn mockScreenTracker
            on { this.verificationPage } doReturn MediatorLiveData(
                Resource.success(verificationPage)
            )
        }
    }
}
