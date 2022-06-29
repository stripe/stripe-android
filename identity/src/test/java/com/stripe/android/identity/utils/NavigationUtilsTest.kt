package com.stripe.android.identity.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
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
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.navigation.ErrorFragment
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NavigationUtilsTest {
    private val mockScreenTracker = mock<ScreenTracker>()

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from consent navigates to error fragment when has BIOMETRICCONSENT error `() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_BIOMETRICCONSENT,
            R.id.consentFragment,
            R.id.consentFragment
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from passportUpload navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            R.id.passportUploadFragment,
            R.id.passportUploadFragment
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            R.id.passportUploadFragment,
            R.id.passportUploadFragment,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from idUpload navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            R.id.IDUploadFragment,
            R.id.IDUploadFragment
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            R.id.IDUploadFragment,
            R.id.IDUploadFragment,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from driverLicenseUpload navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            R.id.driverLicenseUploadFragment,
            R.id.driverLicenseUploadFragment
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            R.id.driverLicenseUploadFragment,
            R.id.driverLicenseUploadFragment,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from passportScan navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            R.id.passportScanFragment,
            R.id.passportScanFragment
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            R.id.passportScanFragment,
            R.id.passportScanFragment,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from idScan navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            R.id.IDScanFragment,
            R.id.IDScanFragment
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            R.id.IDScanFragment,
            R.id.IDScanFragment,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from driverLicenseScan navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            R.id.driverLicenseScanFragment,
            R.id.driverLicenseScanFragment
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            R.id.driverLicenseScanFragment,
            R.id.driverLicenseScanFragment,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from docSelection navigates to error fragment when has IDDOCUMENTTYPE error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTTYPE,
            R.id.docSelectionFragment,
            R.id.docSelectionFragment
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit navigates to error fragment with default destination when error type doesn't match`() {
        // only uploadFragment and scanFragment could possible have IDDOCUMENTFRONT or IDDOCUMENTBACK errors,
        // all other fragments should return with default destination when error returns with this type.

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            R.id.consentFragment,
            ErrorFragment.UNEXPECTED_DESTINATION
        )
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            R.id.consentFragment,
            ErrorFragment.UNEXPECTED_DESTINATION,
            2
        )
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            R.id.docSelectionFragment,
            ErrorFragment.UNEXPECTED_DESTINATION
        )
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            R.id.docSelectionFragment,
            ErrorFragment.UNEXPECTED_DESTINATION,
            2
        )
    }

    private fun testPostVerificationPageDataAndMaybeSubmitWithError(
        errorResponse: VerificationPageData,
        @IdRes fromFragment: Int,
        @IdRes backButtonDestination: Int,
        times: Int = 1
    ) = runBlocking {
        val mockIdentityViewModel = mock<IdentityViewModel>().also {
            whenever(it.postVerificationPageData(any(), any())).thenReturn(
                errorResponse
            )

            whenever(it.screenTracker).thenReturn(mockScreenTracker)
        }

        launchFragment { navController, fragment ->
            fragment.postVerificationPageDataAndMaybeSubmit(
                mockIdentityViewModel,
                mock(),
                mock(),
                fromFragment
            )

            verify(mockScreenTracker, times(times)).screenTransitionStart(eq(fromFragment.fragmentIdToScreenName()), any())

            requireNotNull(navController.backStack.last().arguments).let { arguments ->
                assertThat(arguments[ErrorFragment.ARG_ERROR_TITLE])
                    .isEqualTo(ERROR_TITLE)
                assertThat(arguments[ErrorFragment.ARG_ERROR_CONTENT])
                    .isEqualTo(ERROR_BODY)
                assertThat(arguments[ErrorFragment.ARG_GO_BACK_BUTTON_DESTINATION])
                    .isEqualTo(backButtonDestination)
                assertThat(arguments[ErrorFragment.ARG_GO_BACK_BUTTON_TEXT])
                    .isEqualTo(ERROR_BUTTON_TEXT)
            }
            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit navigates to general error fragment when fails`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any(), any())).thenThrow(
                    APIException()
                )
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    mock(),
                    R.id.consentFragment
                )

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.errorFragment)
            }
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit executes notSubmitBlock when it's not null`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any(), any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
            }
            var blockExecuted = false

            launchFragment { _, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    mock(),
                    R.id.consentFragment
                ) {
                    blockExecuted = true
                }

                assertThat(blockExecuted).isEqualTo(true)
            }
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit submits when notSubmitBlock is null`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any(), any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )
                whenever(it.postVerificationPageSubmit()).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
            }

            launchFragment { _, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    mock(),
                    R.id.consentFragment
                )

                verify(mockIdentityViewModel).postVerificationPageSubmit()
            }
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit submits success with error then navigates to errorFragment with error data`() {
        runBlocking {
            val mockIdentityViewModel = mock<IdentityViewModel>().also {
                whenever(it.postVerificationPageData(any(), any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )

                whenever(it.postVerificationPageSubmit()).thenReturn(
                    ERROR_VERIFICATION_PAGE_DATA_BIOMETRICCONSENT
                )
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    mock(),
                    R.id.consentFragment
                )

                requireNotNull(navController.backStack.last().arguments).let { arguments ->
                    assertThat(arguments[ErrorFragment.ARG_ERROR_TITLE])
                        .isEqualTo(ERROR_TITLE)
                    assertThat(arguments[ErrorFragment.ARG_ERROR_CONTENT])
                        .isEqualTo(ERROR_BODY)
                    assertThat(arguments[ErrorFragment.ARG_GO_BACK_BUTTON_DESTINATION])
                        .isEqualTo(R.id.consentFragment)
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
                whenever(it.postVerificationPageData(any(), any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )

                whenever(it.postVerificationPageSubmit()).thenReturn(
                    CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
                )
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    mock(),
                    R.id.consentFragment
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
                whenever(it.postVerificationPageData(any(), any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )

                whenever(it.postVerificationPageSubmit()).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    mock(),
                    R.id.consentFragment
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
                whenever(it.postVerificationPageData(any(), any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )

                whenever(it.postVerificationPageSubmit()).thenThrow(
                    APIException()
                )
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    mock(),
                    R.id.consentFragment
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

    internal companion object {
        internal val ERROR_VERIFICATION_PAGE_DATA_BIOMETRICCONSENT = VerificationPageData(
            id = "id",
            objectType = "type",
            requirements = VerificationPageDataRequirements(
                errors = listOf(
                    VerificationPageDataRequirementError(
                        body = ERROR_BODY,
                        backButtonText = ERROR_BUTTON_TEXT,
                        requirement = VerificationPageDataRequirementError.Requirement.BIOMETRICCONSENT,
                        title = ERROR_TITLE
                    )
                )
            ),
            status = VerificationPageData.Status.VERIFIED,
            submitted = false
        )

        internal val ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK = VerificationPageData(
            id = "id",
            objectType = "type",
            requirements = VerificationPageDataRequirements(
                errors = listOf(
                    VerificationPageDataRequirementError(
                        body = ERROR_BODY,
                        backButtonText = ERROR_BUTTON_TEXT,
                        requirement = VerificationPageDataRequirementError.Requirement.IDDOCUMENTBACK,
                        title = ERROR_TITLE
                    )
                )
            ),
            status = VerificationPageData.Status.VERIFIED,
            submitted = false
        )

        internal val ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT = VerificationPageData(
            id = "id",
            objectType = "type",
            requirements = VerificationPageDataRequirements(
                errors = listOf(
                    VerificationPageDataRequirementError(
                        body = ERROR_BODY,
                        backButtonText = ERROR_BUTTON_TEXT,
                        requirement = VerificationPageDataRequirementError.Requirement.IDDOCUMENTFRONT,
                        title = ERROR_TITLE
                    )
                )
            ),
            status = VerificationPageData.Status.VERIFIED,
            submitted = false
        )

        internal val ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTTYPE = VerificationPageData(
            id = "id",
            objectType = "type",
            requirements = VerificationPageDataRequirements(
                errors = listOf(
                    VerificationPageDataRequirementError(
                        body = ERROR_BODY,
                        backButtonText = ERROR_BUTTON_TEXT,
                        requirement = VerificationPageDataRequirementError.Requirement.IDDOCUMENTTYPE,
                        title = ERROR_TITLE
                    )
                )
            ),
            status = VerificationPageData.Status.VERIFIED,
            submitted = false
        )
    }
}
