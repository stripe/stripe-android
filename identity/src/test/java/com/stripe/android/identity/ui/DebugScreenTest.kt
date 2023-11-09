package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.DebugDestination
import com.stripe.android.identity.navigation.IndividualWelcomeDestination
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
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
class DebugScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val verificationPageData = MutableLiveData<Resource<VerificationPage>>()
    private val mockNavController = mock<NavController>()
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationPage } doReturn verificationPageData
    }
    private val mockVerificationFlowFinishable = mock<VerificationFlowFinishable>()

    private val verificationPageForDoc = mock<VerificationPage>().also {
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf(
                    Requirement.BIOMETRICCONSENT,
                    Requirement.IDDOCUMENTFRONT,
                    Requirement.IDDOCUMENTBACK,
                    Requirement.IDNUMBER
                )
            )
        )
    }

    private val verificationPageForIDNumber = mock<VerificationPage>().also {
        whenever(it.livemode).thenReturn(true)
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf(
                    Requirement.DOB,
                    Requirement.NAME,
                    Requirement.IDNUMBER
                )
            )
        )
    }

    @Test
    fun testSubmitWithSuccess() {
        setComposeTestRuleWith(verificationPageForDoc) {
            runBlocking {
                onNodeWithTag(TEST_TAG_SUBMIT_BUTTON).performScrollTo()
                onNodeWithTag(TEST_TAG_SUCCESS).performClick()
                onNodeWithTag(TEST_TAG_SUBMIT_BUTTON).performClick()

                verify(mockIdentityViewModel).verifySessionAndTransition(
                    fromRoute = eq(DebugDestination.ROUTE.route),
                    simulateDelay = eq(false),
                    navController = same(mockNavController)
                )
            }
        }
    }

    @Test
    fun testSubmitWithSuccessAsync() {
        setComposeTestRuleWith(verificationPageForDoc) {
            runBlocking {
                onNodeWithTag(TEST_TAG_SUBMIT_BUTTON).performScrollTo()
                onNodeWithTag(TEST_TAG_SUCCESS_ASYNC).performClick()
                onNodeWithTag(TEST_TAG_SUBMIT_BUTTON).performClick()

                verify(mockIdentityViewModel).verifySessionAndTransition(
                    fromRoute = eq(DebugDestination.ROUTE.route),
                    simulateDelay = eq(true),
                    navController = same(mockNavController)
                )
            }
        }
    }

    @Test
    fun testSubmitWithFailure() {
        setComposeTestRuleWith(verificationPageForDoc) {
            runBlocking {
                onNodeWithTag(TEST_TAG_SUBMIT_BUTTON).performScrollTo()
                onNodeWithTag(TEST_TAG_FAILURE).performClick()
                onNodeWithTag(TEST_TAG_SUBMIT_BUTTON).performClick()

                verify(mockIdentityViewModel).unverifySessionAndTransition(
                    fromRoute = eq(DebugDestination.ROUTE.route),
                    simulateDelay = eq(false),
                    navController = same(mockNavController)
                )
            }
        }
    }

    @Test
    fun testSubmitWithFailureAsync() {
        setComposeTestRuleWith(verificationPageForDoc) {
            runBlocking {
                onNodeWithTag(TEST_TAG_SUBMIT_BUTTON).performScrollTo()
                onNodeWithTag(TEST_TAG_FAILURE_ASYNC).performClick()
                onNodeWithTag(TEST_TAG_SUBMIT_BUTTON).performClick()

                verify(mockIdentityViewModel).unverifySessionAndTransition(
                    fromRoute = eq(DebugDestination.ROUTE.route),
                    simulateDelay = eq(true),
                    navController = same(mockNavController)
                )
            }
        }
    }

    @Test
    fun testCancelledButton() {
        setComposeTestRuleWith(verificationPageForDoc) {
            onNodeWithTag(TEST_TAG_CANCELLED_BUTTON).performScrollTo()
            onNodeWithTag(TEST_TAG_CANCELLED_BUTTON).performClick()
            verify(mockVerificationFlowFinishable).finishWithResult(
                eq(IdentityVerificationSheet.VerificationFlowResult.Canceled)
            )
        }
    }

    @Test
    fun testFailedButton() {
        setComposeTestRuleWith(verificationPageForDoc) {
            onNodeWithTag(TEST_TAG_FAILED_BUTTON).performScrollTo()
            onNodeWithTag(TEST_TAG_FAILED_BUTTON).performClick()
            verify(mockVerificationFlowFinishable).finishWithResult(
                argWhere {
                    it is IdentityVerificationSheet.VerificationFlowResult.Failed
                }
            )
        }
    }

    @Test
    fun testProceedButtonWithDoc() {
        setComposeTestRuleWith(verificationPageForDoc) {
            onNodeWithTag(TEST_TAG_PROCEED_BUTTON).performScrollTo()
            onNodeWithTag(TEST_TAG_PROCEED_BUTTON).assertExists()
            onNodeWithTag(TEST_TAG_PROCEED_BUTTON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_PROCEED_BUTTON).performClick()

            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(ConsentDestination.CONSENT)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun testProceedButtonWithIDNumber() {
        setComposeTestRuleWith(verificationPageForIDNumber) {
            onNodeWithTag(TEST_TAG_PROCEED_BUTTON).performScrollTo()
            onNodeWithTag(TEST_TAG_PROCEED_BUTTON).assertExists()
            onNodeWithTag(TEST_TAG_PROCEED_BUTTON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_PROCEED_BUTTON).performClick()

            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(IndividualWelcomeDestination.INDIVIDUAL_WELCOME)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    private fun setComposeTestRuleWith(
        verificationPage: VerificationPage,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        verificationPageData.postValue(Resource.success(verificationPage))
        composeTestRule.setContent {
            DebugScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel,
                verificationFlowFinishable = mockVerificationFlowFinishable
            )
        }

        with(composeTestRule, testBlock)
    }
}
