package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
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

    private val verificationPageLiveData =
        MutableLiveData<Resource<VerificationPage>>(Resource.idle())

    private val mockVerificationArgs = mock<IdentityVerificationSheetContract.Args> {
        on { brandLogo } doReturn mock()
    }
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationPage } doReturn verificationPageLiveData
        on { verificationArgs } doReturn mockVerificationArgs
    }

    private val mockNavController = mock<NavController>()

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
                missing = listOf(Requirement.BIOMETRICCONSENT)
            )
        )
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
                missing = listOf(Requirement.BIOMETRICCONSENT)
            )
        )
    }

    @Test
    fun `when VerificationPage with time and policy UI is bound correctly`() {
        setComposeTestRuleWith(Resource.success(verificationPageWithTimeAndPolicy)) {
            onNodeWithTag(LOADING_SCREEN_TAG).assertDoesNotExist()

            onNodeWithTag(TITLE_TAG).assertTextEquals(CONSENT_TITLE)
            onNodeWithTag(TIME_ESTIMATE_TAG).assertTextEquals(CONSENT_TIME_ESTIMATE)
            onNodeWithTag(PRIVACY_POLICY_TAG).assertTextEquals(CONSENT_PRIVACY_POLICY)
            onNodeWithTag(DIVIDER_TAG).assertExists()
            onNodeWithTag(BODY_TAG).assertTextEquals(CONSENT_BODY)

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

            onNodeWithTag(TITLE_TAG).assertTextEquals(CONSENT_TITLE)
            onNodeWithTag(TIME_ESTIMATE_TAG).assertDoesNotExist()
            onNodeWithTag(PRIVACY_POLICY_TAG).assertDoesNotExist()
            onNodeWithTag(DIVIDER_TAG).assertDoesNotExist()
            onNodeWithTag(BODY_TAG).assertTextEquals(CONSENT_BODY)

            onNodeWithTag(ACCEPT_BUTTON_TAG).onChildAt(0)
                .assertTextEquals(SCROLL_TO_CONTINUE_TEXT.uppercase())
            onNodeWithTag(ACCEPT_BUTTON_TAG).onChildAt(1).assertDoesNotExist()

            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(0)
                .assertTextEquals(CONSENT_DECLINE_TEXT.uppercase())
            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(1).assertDoesNotExist()
        }
    }

    @Test
    fun `when agreed button is clicked correctly navigates`() {
        setComposeTestRuleWith(Resource.success(verificationPageWithTimeAndPolicy)) {
            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(0).performClick()
            runBlocking {
                verify(mockIdentityViewModel).postVerificationPageDataAndMaybeNavigate(
                    same(mockNavController),
                    argThat {
                        biometricConsent == false
                    },
                    eq(ConsentDestination.ROUTE.route),
                    any(),
                    any(),
                    any()
                )
            }

            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
            onNodeWithTag(DECLINE_BUTTON_TAG).onChildAt(1)
                .assertExists() // CircularProgressIndicator

            onNodeWithTag(ACCEPT_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
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
        verificationPageLiveData.postValue(verificationState)
        composeTestRule.setContent {
            ConsentScreen(
                mockNavController,
                mockIdentityViewModel
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
    }
}
