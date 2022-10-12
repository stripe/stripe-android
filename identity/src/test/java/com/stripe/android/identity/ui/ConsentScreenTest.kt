package com.stripe.android.identity.ui

import android.os.Build
import android.widget.ImageView
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class ConsentScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val onMerchantViewCreatedMock = mock<(ImageView) -> Unit>()
    private val onSuccessMock = mock<(VerificationPage) -> Unit>()
    private val onFallbackMock = mock<(String) -> Unit>()
    private val onErrorMock = mock<(Throwable) -> Unit>()
    private val onConsentAgreedMock = mock<(Boolean) -> Unit>()
    private val onConsentDeclinedMock = mock<(Boolean) -> Unit>()

    private val verificationPageWithTimeAndPolicy = mock<VerificationPage>().also {
        whenever(it.biometricConsent).thenReturn(
            VerificationPageStaticContentConsentPage(
                acceptButtonText = CONSENT_ACCEPT_TEXT,
                title = CONSENT_TITLE,
                privacyPolicy = CONSENT_PRIVACY_POLICY,
                timeEstimate = CONSENT_TIME_ESTIMATE,
                body = CONSENT_BODY,
                declineButtonText = CONSENT_DECLINE_TEXT,
                scrollToContinueButtonText = SCROLL_TO_CONTINUE_TEXT
            )
        )
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf(
                    VerificationPageRequirements.Missing.BIOMETRICCONSENT
                )
            )
        )
        if (CONSENT_REQUIRE_SELFIE) {
            whenever(it.selfieCapture).thenReturn(mock())
        }
    }

    private val verificationPageWithOutTimeAndPolicy = mock<VerificationPage>().also {
        whenever(it.biometricConsent).thenReturn(
            VerificationPageStaticContentConsentPage(
                acceptButtonText = CONSENT_ACCEPT_TEXT,
                title = CONSENT_TITLE,
                privacyPolicy = null,
                timeEstimate = null,
                body = CONSENT_BODY,
                declineButtonText = CONSENT_DECLINE_TEXT,
                scrollToContinueButtonText = SCROLL_TO_CONTINUE_TEXT
            )
        )
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf(
                    VerificationPageRequirements.Missing.BIOMETRICCONSENT
                )
            )
        )
        if (CONSENT_REQUIRE_SELFIE) {
            whenever(it.selfieCapture).thenReturn(mock())
        }
    }

    private val verificationPageWithUnsupportedClient = mock<VerificationPage>().also {
        whenever(it.unsupportedClient).thenReturn(true)
        whenever(it.fallbackUrl).thenReturn(CONSENT_FALLBACK_URL)
    }

    @Test
    fun `when VerificationPage with time and policy UI is bound correctly`() {
        setComposeTestRuleWith(Resource.success(verificationPageWithTimeAndPolicy)) {
            onNodeWithTag(LOADING_SCREEN_TAG).assertDoesNotExist()
            verify(onSuccessMock).invoke(same(verificationPageWithTimeAndPolicy))

            verify(onMerchantViewCreatedMock).invoke(any())
            onNodeWithTag(TITLE_TAG).assertTextEquals(CONSENT_TITLE)
            onNodeWithTag(TIME_ESTIMATE_TAG).assertExists() // TODO: assert text after migrating to compose Text
            onNodeWithTag(PRIVACY_POLICY_TAG).assertExists() // TODO: assert text after migrating to compose Text
            onNodeWithTag(DIVIDER_TAG).assertExists()
            onNodeWithTag(BODY_TAG).assertExists() // TODO: assert text after migrating to compose Text

            onNodeWithTag(ACCEPT_BUTTON_TAG).onChildAt(0)
                .assertTextEquals(SCROLL_TO_CONTINUE_TEXT.uppercase())
            onNodeWithTag(ACCEPT_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()

            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(0)
                .assertTextEquals(CONSENT_DECLINE_TEXT.uppercase())
            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(1).assertDoesNotExist()
        }
    }

    @Test
    fun `when VerificationPage without time and policy UI is bound correctly`() {
        setComposeTestRuleWith(Resource.success(verificationPageWithOutTimeAndPolicy)) {
            onNodeWithTag(LOADING_SCREEN_TAG).assertDoesNotExist()
            verify(onSuccessMock).invoke(same(verificationPageWithOutTimeAndPolicy))

            verify(onMerchantViewCreatedMock).invoke(any())
            onNodeWithTag(TITLE_TAG).assertTextEquals(CONSENT_TITLE)
            onNodeWithTag(TIME_ESTIMATE_TAG).assertDoesNotExist()
            onNodeWithTag(PRIVACY_POLICY_TAG).assertDoesNotExist()
            onNodeWithTag(DIVIDER_TAG).assertDoesNotExist()
            onNodeWithTag(BODY_TAG).assertExists() // TODO: assert text after migrating to compose Text

            onNodeWithTag(ACCEPT_BUTTON_TAG).onChildAt(0)
                .assertTextEquals(SCROLL_TO_CONTINUE_TEXT.uppercase())
            onNodeWithTag(ACCEPT_BUTTON_TAG).onChildAt(1).assertDoesNotExist()

            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(0)
                .assertTextEquals(CONSENT_DECLINE_TEXT.uppercase())
            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(1).assertDoesNotExist()
        }
    }

    @Test
    fun `when agreed button is clicked onConsentDeclined is called`() {
        setComposeTestRuleWith(Resource.success(verificationPageWithTimeAndPolicy)) {
            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(0).performClick()
            verify(onConsentDeclinedMock).invoke(CONSENT_REQUIRE_SELFIE)

            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(1)
                .assertExists() // CircularProgressIndicator

            onNodeWithTag(ACCEPT_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun `when VerificationPage is unsupported, onFallbackUrl is called`() {
        setComposeTestRuleWith(Resource.success(verificationPageWithUnsupportedClient))
        verify(onFallbackMock).invoke(eq(CONSENT_FALLBACK_URL))
    }

    @Test
    fun `when VerificationPage with error onError is invoked`() {
        val throwable = mock<Throwable>()
        setComposeTestRuleWith(Resource.error(throwable = throwable))
        verify(onErrorMock).invoke(same(throwable))
    }

    @Test
    fun `when VerificationPage is Loading UI is bound correctly`() {
        setComposeTestRuleWith(Resource.loading()) {
            onNodeWithTag(LOADING_SCREEN_TAG).assertExists()
        }
    }

    private fun setComposeTestRuleWith(
        verificationState: Resource<VerificationPage>,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        composeTestRule.setContent {
            ConsentScreen(
                verificationState = verificationState,
                onMerchantViewCreated = onMerchantViewCreatedMock,
                onSuccess = onSuccessMock,
                onFallbackUrl = onFallbackMock,
                onError = onErrorMock,
                onConsentAgreed = onConsentAgreedMock,
                onConsentDeclined = onConsentDeclinedMock
            )
        }

        with(composeTestRule, testBlock)
    }

    private companion object {
        const val CONSENT_TITLE = "title"
        const val CONSENT_PRIVACY_POLICY = "privacy policy"
        const val CONSENT_TIME_ESTIMATE = "time estimate"
        const val CONSENT_BODY = "this is the consent body"
        const val CONSENT_ACCEPT_TEXT = "yes"
        const val CONSENT_DECLINE_TEXT = "no"
        const val SCROLL_TO_CONTINUE_TEXT = "scroll to continue"
        const val CONSENT_FALLBACK_URL = "path/to/fallback"
        const val CONSENT_REQUIRE_SELFIE = true
    }
}
