package com.stripe.android.identity.navigation

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONFIRMATION
import com.stripe.android.identity.databinding.ConfirmationFragmentBinding
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentTextPage
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfirmationFragmentTest {

    private val mockVerificationFlowFinishable = mock<VerificationFlowFinishable>()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { identityAnalyticsRequestFactory }.thenReturn(
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            )
        )
    }

    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.success).thenReturn(
            VerificationPageStaticContentTextPage(
                body = CONFIRMATION_BODY,
                buttonText = CONFIRMATION_BUTTON_TEXT,
                title = CONFIRMATION_TITLE
            )
        )
    }

    private fun setUpSuccessVerificationPage() {
        val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
        verify(
            mockIdentityViewModel,
            times(1)
        ).observeForVerificationPage(
            any(),
            successCaptor.capture(),
            any()
        )
        successCaptor.lastValue(verificationPage)
    }

    private fun setUpErrorVerificationPage() {
        val failureCaptor: KArgumentCaptor<(Throwable?) -> Unit> = argumentCaptor()
        verify(
            mockIdentityViewModel
        ).observeForVerificationPage(
            any(),
            any(),
            failureCaptor.capture()
        )
        failureCaptor.firstValue(null)
    }

    @Test
    fun `when verification page is available UI is bound correctly`() {
        launchConfirmationFragment { binding, _ ->
            setUpSuccessVerificationPage()

            verify(mockIdentityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == EVENT_SCREEN_PRESENTED &&
                        params[PARAM_SCREEN_NAME] == SCREEN_NAME_CONFIRMATION
                }
            )
            assertThat(binding.titleText.text).isEqualTo(CONFIRMATION_TITLE)
            assertThat(binding.contentText.text.toString()).isEqualTo(CONFIRMATION_BODY)
            assertThat(binding.kontinue.text).isEqualTo(CONFIRMATION_BUTTON_TEXT)
        }
    }

    @Test
    fun `when finish button clicked then finish with completed`() {
        launchConfirmationFragment { binding, _ ->
            setUpSuccessVerificationPage()
            binding.kontinue.callOnClick()

            verify(mockVerificationFlowFinishable).finishWithResult(
                eq(IdentityVerificationSheet.VerificationFlowResult.Completed)
            )
        }
    }

    @Test
    fun `when verification page is not available navigates to error`() {
        launchConfirmationFragment { _, navController ->
            setUpErrorVerificationPage()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    private fun launchConfirmationFragment(
        testBlock: (binding: ConfirmationFragmentBinding, navController: TestNavHostController) -> Unit
    ) = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        ConfirmationFragment(
            viewModelFactoryFor(mockIdentityViewModel),
            mockVerificationFlowFinishable
        )
    }.onFragment {
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )
        navController.setGraph(
            R.navigation.identity_nav_graph
        )
        navController.setCurrentDestination(R.id.consentFragment)
        Navigation.setViewNavController(
            it.requireView(),
            navController
        )

        testBlock(ConfirmationFragmentBinding.bind(it.requireView()), navController)
    }

    private companion object {
        const val CONFIRMATION_TITLE = "title"
        const val CONFIRMATION_BODY = "this is the confirmation body"
        const val CONFIRMATION_BUTTON_TEXT = "confirmation"
    }
}
