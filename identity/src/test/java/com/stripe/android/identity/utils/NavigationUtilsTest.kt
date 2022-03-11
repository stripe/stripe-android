package com.stripe.android.identity.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.ERROR_BODY
import com.stripe.android.identity.ERROR_BUTTON_TEXT
import com.stripe.android.identity.ERROR_TITLE
import com.stripe.android.identity.ERROR_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.ErrorFragment
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NavigationUtilsTest {
    @Test
    fun `postVerificationPageDataAndMaybeSubmit navigates to error fragment when has error`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any())).thenReturn(
                    ERROR_VERIFICATION_PAGE_DATA
                )
            }
            val mockCollectedDataParam = mock<CollectedDataParam>()

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mockCollectedDataParam,
                    { true },
                    {}
                )

                requireNotNull(navController.backStack.last().arguments).let { arguments ->
                    assertThat(arguments[ErrorFragment.ARG_ERROR_TITLE])
                        .isEqualTo(ERROR_TITLE)
                    assertThat(arguments[ErrorFragment.ARG_ERROR_CONTENT])
                        .isEqualTo(ERROR_BODY)
                    assertThat(arguments[ErrorFragment.ARG_GO_BACK_BUTTON_DESTINATION])
                        .isEqualTo(R.id.action_errorFragment_to_consentFragment)
                    assertThat(arguments[ErrorFragment.ARG_GO_BACK_BUTTON_TEXT])
                        .isEqualTo(ERROR_BUTTON_TEXT)
                }
                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.errorFragment)
            }
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit navigates to general error fragment when fails`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any())).thenThrow(
                    APIException()
                )
            }
            val mockCollectedDataParam = mock<CollectedDataParam>()

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mockCollectedDataParam,
                    { true },
                    {}
                )

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.errorFragment)
            }
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit executes notSubmitBlock when shouldNotSubmit`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )
            }
            var blockExecuted = false

            launchFragment { _, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    { true },
                    {
                        blockExecuted = true
                    }
                )

                assertThat(blockExecuted).isEqualTo(true)
            }
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit submits when shouldNotSubmit is false`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )
                whenever(it.postVerificationPageSubmit()).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )
            }

            launchFragment { _, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    { false },
                    {}
                )

                verify(mockIdentityViewModel).postVerificationPageSubmit()
            }
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit submits success with error then navigates to errorFragment with error data`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )

                whenever(it.postVerificationPageSubmit()).thenReturn(
                    ERROR_VERIFICATION_PAGE_DATA
                )
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    { false },
                    {}
                )

                requireNotNull(navController.backStack.last().arguments).let { arguments ->
                    assertThat(arguments[ErrorFragment.ARG_ERROR_TITLE])
                        .isEqualTo(ERROR_TITLE)
                    assertThat(arguments[ErrorFragment.ARG_ERROR_CONTENT])
                        .isEqualTo(ERROR_BODY)
                    assertThat(arguments[ErrorFragment.ARG_GO_BACK_BUTTON_DESTINATION])
                        .isEqualTo(R.id.action_errorFragment_to_consentFragment)
                    assertThat(arguments[ErrorFragment.ARG_GO_BACK_BUTTON_TEXT])
                        .isEqualTo(ERROR_BUTTON_TEXT)
                }
                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.errorFragment)
            }
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit submits success with submitted success then navigates to general ErrorFragment`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )

                whenever(it.postVerificationPageSubmit()).thenReturn(
                    CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
                )
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    { false },
                    {}
                )

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.confirmationFragment)
            }
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit submits success with submitted failure then navigates to ConfirmationFragment`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )

                whenever(it.postVerificationPageSubmit()).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    { false },
                    {}
                )

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.errorFragment)
            }
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit submits fails then navigates to general ErrorFragment`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )

                whenever(it.postVerificationPageSubmit()).thenThrow(
                    APIException()
                )
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    { false },
                    {}
                )

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.errorFragment)
            }
        }
    }

    private fun launchFragment(testBlock: suspend (navController: TestNavHostController, fragment: Fragment) -> Unit) {
        launchFragmentInContainer<TestFragment>().onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )
            runBlocking {
                testBlock(navController, it)
            }
        }
    }

    internal class TestFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = View(context)
    }
}
