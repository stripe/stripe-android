package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.navigation.INDIVIDUAL
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageIconType
import com.stripe.android.identity.networking.models.VerificationPageStaticConsentLineContent
import com.stripe.android.identity.networking.models.VerificationPageStaticContentIndividualWelcomePage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class IndividualWelcomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockVerificationArgs = mock<IdentityVerificationSheetContract.Args> {
        on { brandLogo } doReturn mock()
    }

    private val verificationPageLiveData =
        MutableLiveData(
            Resource.success(
                mock<VerificationPage>().also {
                    whenever(it.individualWelcome).thenReturn(
                        VerificationPageStaticContentIndividualWelcomePage(
                            getStartedButtonText = INDIVIDUAL_WELCOME_GET_STARTED_TEXT,
                            title = INDIVIDUAL_WELCOME_TITLE,
                            privacyPolicy = INDIVIDUAL_WELCOME_PRIVACY_POLICY,
                            lines = listOf(
                                VerificationPageStaticConsentLineContent(
                                    icon = VerificationPageIconType.CAMERA,
                                    content = INDIVIDUAL_WELCOME_CAMERA_LINE
                                ),
                                VerificationPageStaticConsentLineContent(
                                    icon = VerificationPageIconType.CLOUD,
                                    content = INDIVIDUAL_WELCOME_CLOUD_LINE
                                )
                            )
                        )
                    )
                }
            )
        )
    private val mockNavController = mock<NavController>()
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationPage } doReturn verificationPageLiveData
        on { verificationArgs } doReturn mockVerificationArgs
    }

    @Test
    fun verifyUIIsBoundCorrectly() {
        testIndividualWelcome {
            onNodeWithTag(INDIVIDUAL_WELCOME_TITLE_TAG).assertTextEquals(INDIVIDUAL_WELCOME_TITLE)
            onNodeWithTag(INDIVIDUAL_WELCOME_GET_STARTED_BUTTON_TAG).onChildAt(0).assertTextEquals(
                INDIVIDUAL_WELCOME_GET_STARTED_TEXT.uppercase()
            )
            onAllNodesWithTag(CONSENT_LINE_TAG).assertCountEquals(2)
            onNodeWithTag(INDIVIDUAL_WELCOME_PRIVACY_POLICY_TAG).assertTextEquals(
                INDIVIDUAL_WELCOME_PRIVACY_POLICY
            )
        }
    }

    @Test
    fun whenButtonClickedNavigateToIndividual() {
        testIndividualWelcome {
            onNodeWithTag(INDIVIDUAL_WELCOME_GET_STARTED_BUTTON_TAG).performClick()
            verify(mockNavController).navigate(
                eq(INDIVIDUAL),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    private fun testIndividualWelcome(
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        composeTestRule.setContent {
            IndividualWelcomeScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel
            )
        }

        with(composeTestRule, testBlock)
    }

    private companion object {
        const val INDIVIDUAL_WELCOME_TITLE = "title"
        const val INDIVIDUAL_WELCOME_PRIVACY_POLICY = "privacy policy"
        const val INDIVIDUAL_WELCOME_GET_STARTED_TEXT = "get started"
        const val INDIVIDUAL_WELCOME_CAMERA_LINE = "content for camera line"
        const val INDIVIDUAL_WELCOME_CLOUD_LINE = "content for cloud line"
    }
}
