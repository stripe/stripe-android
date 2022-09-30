package com.stripe.android.identity.navigation

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EVENT_META_DATA
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONFIRMATION
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentTextPage
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ConfirmationFragmentTest {
    private val mockVerificationFlowFinishable = mock<VerificationFlowFinishable>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val verificationPageLiveData = MutableLiveData<Resource<VerificationPage>>()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { identityAnalyticsRequestFactory }.thenReturn(
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            )
        )
        on { uiContext } doReturn testDispatcher
        on { workContext } doReturn testDispatcher
        on { screenTracker } doReturn mock()
        on { analyticsState } doReturn mock()

        on { verificationPage } doReturn verificationPageLiveData
    }

    private val verificationPage = mock<VerificationPage>()

    private fun setUpSuccessVerificationPage() {
        whenever(verificationPage.success).thenReturn(
            VerificationPageStaticContentTextPage(
                body = CONFIRMATION_BODY,
                buttonText = CONFIRMATION_BUTTON_TEXT,
                title = CONFIRMATION_TITLE
            )
        )
        verificationPageLiveData.postValue(Resource.success(verificationPage))
    }

    @Test
    fun `when verification page is available analytics is sent and screen is tracked`() {
        setUpSuccessVerificationPage()

        launchConfirmationFragment { _ ->

            verify(mockIdentityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == EVENT_SCREEN_PRESENTED &&
                        (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCREEN_NAME] == SCREEN_NAME_CONFIRMATION
                }
            )
        }
    }

    private fun launchConfirmationFragment(
        testBlock: (navController: TestNavHostController) -> Unit
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

        testBlock(navController)
    }

    private companion object {
        const val CONFIRMATION_TITLE = "title"
        const val CONFIRMATION_BODY = "this is the confirmation body"
        const val CONFIRMATION_BUTTON_TEXT = "confirmation"
    }
}
