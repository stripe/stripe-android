package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertTextContains
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
import com.stripe.android.identity.networking.models.VerificationPage.Companion.IDPROD_3D_FACE_CAPTURE_MOBILE_EXPERIMENT
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentExperiment
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.testing.createComposeCleanupRule
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

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val mockNavController = mock<NavController>()
    private val mockScreenTracker = mock<ScreenTracker>()
    private val defaultBiometricConsentPage = biometricConsentPage()
    private val mockVerificationPage = mock<VerificationPage> {
        on { biometricConsent } doReturn defaultBiometricConsentPage
        on { experiments } doReturn emptyList()
    }
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { screenTracker } doReturn mockScreenTracker
        on { verificationPage } doReturn MediatorLiveData(
            Resource.success(mockVerificationPage)
        )
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
            onNodeWithTag(SELFIE_WARMUP_TITLE_TAG)
                .assertTextContains(TRAINING_CONSENT_TITLE)
            onNodeWithTag(SELFIE_WARMUP_BODY_TAG)
                .assertTextContains(TRAINING_CONSENT_BODY)
            onNodeWithTag(SELFIE_TRAINING_CONSENT_FOOTER_TAG)
                .assertExists()

            onNodeWithTag(SELFIE_ALLOW_BUTTON_TAG).onChildAt(0).performClick()
            verify(identityViewModel).setSelfieTrainingConsent(eq(true))
            verify(mockNavController).navigate(
                eq(SelfieDestination.routeWithArgs),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun verifyLegacyTrainingConsentContent() {
        val identityViewModel = mockIdentityViewModelWithTrainingConsent(
            has3DFaceCaptureExperiment = false,
            trainingConsentText = LEGACY_TRAINING_CONSENT_TEXT
        )

        composeTestRule.setContent {
            SelfieWarmupScreen(
                navController = mockNavController,
                identityViewModel = identityViewModel,
            )
        }

        composeTestRule
            .onNodeWithTag(SELFIE_WARMUP_TITLE_TAG)
            .assertTextContains(LEGACY_WARMUP_TITLE)
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
            onNodeWithTag(SELFIE_DECLINE_BUTTON_TAG)
                .onChildAt(0)
                .assertTextContains(CONSENT_DECLINE_TEXT)
            onNodeWithTag(SELFIE_DECLINE_BUTTON_TAG).onChildAt(0).performClick()
            verify(identityViewModel).setSelfieTrainingConsent(eq(false))
            verify(mockNavController).navigate(
                eq(SelfieDestination.routeWithArgs),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun verifyDeclineAndContinueTrainingConsentText() {
        val identityViewModel = mockIdentityViewModelWithTrainingConsent(
            declineAndContinueButtonText = DECLINE_AND_CONTINUE_TEXT
        )

        composeTestRule.setContent {
            SelfieWarmupScreen(
                navController = mockNavController,
                identityViewModel = identityViewModel,
            )
        }

        composeTestRule
            .onNodeWithTag(SELFIE_DECLINE_BUTTON_TAG)
            .onChildAt(0)
            .assertTextContains(DECLINE_AND_CONTINUE_TEXT)
    }

    private fun mockIdentityViewModelWithTrainingConsent(
        declineAndContinueButtonText: String? = null,
        has3DFaceCaptureExperiment: Boolean = true,
        trainingConsentText: String = TRAINING_CONSENT_HTML
    ): IdentityViewModel {
        val selfieCapturePage = mock<VerificationPageStaticContentSelfieCapturePage> {
            on { consentText } doReturn trainingConsentText
            on { this.declineAndContinueButtonText } doReturn declineAndContinueButtonText
        }
        val biometricConsentPage = biometricConsentPage()
        val verificationPage = mock<VerificationPage> {
            on { selfieCapture } doReturn selfieCapturePage
            on { biometricConsent } doReturn biometricConsentPage
            on { experiments } doReturn if (has3DFaceCaptureExperiment) {
                listOf(faceCaptureExperiment())
            } else {
                emptyList()
            }
        }
        return mock {
            on { screenTracker } doReturn mockScreenTracker
            on { this.verificationPage } doReturn MediatorLiveData(
                Resource.success(verificationPage)
            )
        }
    }

    private fun biometricConsentPage() = mock<VerificationPageStaticContentConsentPage> {
        on { declineButtonText } doReturn CONSENT_DECLINE_TEXT
    }

    private fun faceCaptureExperiment() = VerificationPageStaticContentExperiment(
        experimentName = IDPROD_3D_FACE_CAPTURE_MOBILE_EXPERIMENT,
        eventName = "screen_presented",
        eventMetadata = mapOf("screen_name" to "selfie")
    )

    private companion object {
        const val CONSENT_DECLINE_TEXT = "Decline"
        const val DECLINE_AND_CONTINUE_TEXT = "Decline and continue"
        const val LEGACY_TRAINING_CONSENT_TEXT = "Allow Stripe to use your images."
        const val LEGACY_WARMUP_TITLE = "Get ready to take a selfie"
        const val TRAINING_CONSENT_BODY =
            "With your permission, Stripe may use your images to improve fraud detection. " +
                "Declining doesn't affect your verification."
        const val TRAINING_CONSENT_LINK_TEXT = "Learn how Stripe uses your data"
        const val TRAINING_CONSENT_HTML =
            "<a href='https://stripe.com'>$TRAINING_CONSENT_LINK_TEXT</a>"
        const val TRAINING_CONSENT_TITLE = "Verify with a selfie"
    }
}
