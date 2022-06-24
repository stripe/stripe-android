package com.stripe.android.identity.navigation

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EVENT_META_DATA
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_ERROR
import com.stripe.android.identity.databinding.BaseErrorFragmentBinding
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ErrorFragmentTest {

    private val mockVerificationFlowFinishable = mock<VerificationFlowFinishable>()
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { identityAnalyticsRequestFactory } doReturn
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            )
    }

    @Test
    fun `title and content are set correctly`() {
        launchErrorFragment().onFragment {
            verify(mockIdentityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == EVENT_SCREEN_PRESENTED &&
                        (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCREEN_NAME] == SCREEN_NAME_ERROR
                }
            )

            val binding = BaseErrorFragmentBinding.bind(it.requireView())

            assertThat(binding.titleText.text).isEqualTo(TEST_ERROR_TITLE)
            assertThat(binding.message1.text).isEqualTo(TEST_ERROR_CONTENT)
        }
    }

    @Test
    fun `bottom button is hidden correctly when not set`() {
        launchErrorFragment().onFragment {
            val binding = BaseErrorFragmentBinding.bind(it.requireView())

            assertThat(binding.topButton.visibility).isEqualTo(View.GONE)
            assertThat(binding.bottomButton.visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun `bottom button is set correctly when set`() {
        launchErrorFragment(ErrorFragment.UNEXPECTED_DESTINATION).onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.errorFragment)
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )
            val binding = BaseErrorFragmentBinding.bind(it.requireView())

            assertThat(binding.topButton.visibility).isEqualTo(View.GONE)
            assertThat(binding.bottomButton.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.bottomButton.text).isEqualTo(TEST_GO_BACK_BUTTON_TEXT)

            binding.bottomButton.callOnClick()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.consentFragment)
        }
    }

    @Test
    fun `clicking bottom button finishes the flow when failed reason is set`() {
        val mockFailedReason = mock<Throwable>()
        launchErrorFragmentWithFailedReason(mockFailedReason).onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.errorFragment)
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )
            val binding = BaseErrorFragmentBinding.bind(it.requireView())

            assertThat(binding.topButton.visibility).isEqualTo(View.GONE)
            assertThat(binding.bottomButton.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.bottomButton.text).isEqualTo(TEST_GO_BACK_BUTTON_TEXT)

            binding.bottomButton.callOnClick()
            val resultCaptor = argumentCaptor<VerificationFlowResult.Failed>()
            verify(mockVerificationFlowFinishable).finishWithResult(
                resultCaptor.capture()
            )
            assertThat(resultCaptor.firstValue.throwable).isSameInstanceAs(mockFailedReason)
        }
    }

    @Test
    fun `when destination is present in backstack, clicking back keep popping until destination is reached`() {
        val navigationDestination = R.id.consentFragment

        launchErrorFragment(navigationDestination).onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.consentFragment)
            navController.navigate(R.id.action_consentFragment_to_docSelectionFragment)
            navController.navigate(R.id.action_global_errorFragment)

            Navigation.setViewNavController(
                it.requireView(),
                navController
            )

            // back stack: [consentFragment, docSelectionFragment, errorFragment]
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.errorFragment)

            // keep popping until navigationDestination(consentFragment) is reached
            BaseErrorFragmentBinding.bind(it.requireView()).bottomButton.callOnClick()

            assertThat(navController.currentDestination?.id).isEqualTo(navigationDestination)
        }
    }

    @Test
    fun `when destination is not present in backstack, clicking back reaches the first entry`() {
        val navigationDestination = R.id.confirmationFragment
        val firstEntry = R.id.consentFragment
        launchErrorFragment(navigationDestination).onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(firstEntry)
            navController.navigate(R.id.action_consentFragment_to_docSelectionFragment)
            navController.navigate(R.id.action_global_errorFragment)

            Navigation.setViewNavController(
                it.requireView(),
                navController
            )

            // back stack: [consentFragment, docSelectionFragment, errorFragment]
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.errorFragment)

            // navigationDestination(confirmationFragment) is not in backstack,
            // keep popping until firstEntry(consentFragment) is reached
            BaseErrorFragmentBinding.bind(it.requireView()).bottomButton.callOnClick()

            assertThat(navController.currentDestination?.id).isEqualTo(firstEntry)
        }
    }

    private fun launchErrorFragment(
        navigationDestination: Int? = null
    ) = launchFragmentInContainer(
        bundleOf(
            ErrorFragment.ARG_ERROR_TITLE to TEST_ERROR_TITLE,
            ErrorFragment.ARG_ERROR_CONTENT to TEST_ERROR_CONTENT
        ).also { bundle ->
            navigationDestination?.let {
                bundle.putInt(ErrorFragment.ARG_GO_BACK_BUTTON_DESTINATION, navigationDestination)
                bundle.putString(ErrorFragment.ARG_GO_BACK_BUTTON_TEXT, TEST_GO_BACK_BUTTON_TEXT)
            }
        },
        themeResId = R.style.Theme_MaterialComponents
    ) {
        ErrorFragment(mock(), viewModelFactoryFor(mockIdentityViewModel))
    }

    private fun launchErrorFragmentWithFailedReason(
        throwable: Throwable
    ) = launchFragmentInContainer(
        bundleOf(
            ErrorFragment.ARG_ERROR_TITLE to TEST_ERROR_TITLE,
            ErrorFragment.ARG_ERROR_CONTENT to TEST_ERROR_CONTENT,
            ErrorFragment.ARG_GO_BACK_BUTTON_TEXT to TEST_GO_BACK_BUTTON_TEXT,
            ErrorFragment.ARG_FAILED_REASON to throwable
        ),
        themeResId = R.style.Theme_MaterialComponents
    ) {
        ErrorFragment(mockVerificationFlowFinishable, viewModelFactoryFor(mockIdentityViewModel))
    }

    private companion object {
        const val TEST_ERROR_TITLE = "test error title"
        const val TEST_ERROR_CONTENT = "test error content"
        const val TEST_GO_BACK_BUTTON_TEXT = "go back"
    }
}
