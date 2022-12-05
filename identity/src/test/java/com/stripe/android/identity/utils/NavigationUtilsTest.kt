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
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.DocSelectionDestination
import com.stripe.android.identity.navigation.DriverLicenseScanDestination
import com.stripe.android.identity.navigation.DriverLicenseUploadDestination
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_ERROR_CONTENT
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_ERROR_TITLE
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_GO_BACK_BUTTON_DESTINATION
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_GO_BACK_BUTTON_TEXT
import com.stripe.android.identity.navigation.ErrorDestination.Companion.UNEXPECTED_ROUTE
import com.stripe.android.identity.navigation.IDScanDestination
import com.stripe.android.identity.navigation.IDUploadDestination
import com.stripe.android.identity.navigation.PassportScanDestination
import com.stripe.android.identity.navigation.PassportUploadDestination
import com.stripe.android.identity.navigation.routeToScreenName
import com.stripe.android.identity.networking.models.Requirement
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
            ConsentDestination.ROUTE.route,
            ConsentDestination.ROUTE.route
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from passportUpload navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            PassportUploadDestination.ROUTE.route,
            PassportUploadDestination.ROUTE.route
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            PassportUploadDestination.ROUTE.route,
            PassportUploadDestination.ROUTE.route,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from idUpload navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            IDUploadDestination.ROUTE.route,
            IDUploadDestination.ROUTE.route
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            IDUploadDestination.ROUTE.route,
            IDUploadDestination.ROUTE.route,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from driverLicenseUpload navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            DriverLicenseUploadDestination.ROUTE.route,
            DriverLicenseUploadDestination.ROUTE.route
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            DriverLicenseUploadDestination.ROUTE.route,
            DriverLicenseUploadDestination.ROUTE.route,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from passportScan navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            PassportScanDestination.ROUTE.route,
            PassportScanDestination.ROUTE.route
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            PassportScanDestination.ROUTE.route,
            PassportScanDestination.ROUTE.route,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from idScan navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            IDScanDestination.ROUTE.route,
            IDScanDestination.ROUTE.route
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            IDScanDestination.ROUTE.route,
            IDScanDestination.ROUTE.route,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from driverLicenseScan navigates to error fragment when has IDDOCUMENT error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            DriverLicenseScanDestination.ROUTE.route,
            DriverLicenseScanDestination.ROUTE.route
        )

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            DriverLicenseScanDestination.ROUTE.route,
            DriverLicenseScanDestination.ROUTE.route,
            2
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit from docSelection navigates to error fragment when has IDDOCUMENTTYPE error`() {
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTTYPE,
            DocSelectionDestination.ROUTE.route,
            DocSelectionDestination.ROUTE.route
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeSubmit navigates to error fragment with default destination when error type doesn't match`() {
        // only uploadFragment and scanFragment could possible have IDDOCUMENTFRONT or IDDOCUMENTBACK errors,
        // all other fragments should return with default destination when error returns with this type.

        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            ConsentDestination.ROUTE.route,
            UNEXPECTED_ROUTE
        )
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            ConsentDestination.ROUTE.route,
            UNEXPECTED_ROUTE,
            2
        )
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTFRONT,
            DocSelectionDestination.ROUTE.route,
            UNEXPECTED_ROUTE
        )
        testPostVerificationPageDataAndMaybeSubmitWithError(
            ERROR_VERIFICATION_PAGE_DATA_IDDOCUMENTBACK,
            DocSelectionDestination.ROUTE.route,
            UNEXPECTED_ROUTE,
            2
        )
    }

    private fun testPostVerificationPageDataAndMaybeSubmitWithError(
        errorResponse: VerificationPageData,
        fromRoute: String,
        backButtonDestination: String,
        times: Int = 1
    ) = runBlocking {
        val mockIdentityViewModel = mock<IdentityViewModel>().also {
            whenever(it.postVerificationPageData(any())).thenReturn(
                errorResponse
            )

            whenever(it.screenTracker).thenReturn(mockScreenTracker)
            whenever(it.errorCause).thenReturn(mock())
        }

        launchFragment { navController, fragment ->
            fragment.postVerificationPageDataAndMaybeSubmit(
                mockIdentityViewModel,
                mock(),
                fromRoute
            )

            verify(
                mockScreenTracker,
                times(times)
            ).screenTransitionStart(eq(fromRoute.routeToScreenName()), any())

            requireNotNull(navController.backStack.last().arguments).let { arguments ->
                assertThat(arguments.getString(ARG_ERROR_TITLE))
                    .isEqualTo(ERROR_TITLE)
                assertThat(arguments.getString(ARG_ERROR_CONTENT))
                    .isEqualTo(ERROR_BODY)
                assertThat(arguments.getString(ARG_GO_BACK_BUTTON_DESTINATION))
                    .isEqualTo(backButtonDestination)
                assertThat(arguments.getString(ARG_GO_BACK_BUTTON_TEXT))
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
                whenever(it.postVerificationPageData(any())).thenThrow(
                    APIException()
                )
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
                whenever(it.errorCause).thenReturn(mock())
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    ConsentDestination.ROUTE.route
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
                whenever(it.postVerificationPageData(any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
            }
            var blockExecuted = false

            launchFragment { _, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    ConsentDestination.ROUTE.route
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
                whenever(it.postVerificationPageData(any())).thenReturn(
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
                    ConsentDestination.ROUTE.route
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
                    ERROR_VERIFICATION_PAGE_DATA_BIOMETRICCONSENT
                )
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
                whenever(it.errorCause).thenReturn(mock())
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    ConsentDestination.ROUTE.route
                )

                requireNotNull(navController.backStack.last().arguments).let { arguments ->
                    assertThat(arguments.getString(ARG_ERROR_TITLE))
                        .isEqualTo(ERROR_TITLE)
                    assertThat(arguments.getString(ARG_ERROR_CONTENT))
                        .isEqualTo(ERROR_BODY)
                    assertThat(arguments.getString(ARG_GO_BACK_BUTTON_DESTINATION))
                        .isEqualTo(ConsentDestination.ROUTE.route)
                    assertThat(arguments.getString(ARG_GO_BACK_BUTTON_TEXT))
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
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    ConsentDestination.ROUTE.route
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
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
                whenever(it.errorCause).thenReturn(mock())
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    ConsentDestination.ROUTE.route
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
                whenever(it.screenTracker).thenReturn(mockScreenTracker)
                whenever(it.errorCause).thenReturn(mock())
            }

            launchFragment { navController, fragment ->
                fragment.postVerificationPageDataAndMaybeSubmit(
                    mockIdentityViewModel,
                    mock(),
                    ConsentDestination.ROUTE.route
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
                        requirement = Requirement.BIOMETRICCONSENT,
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
                        requirement = Requirement.IDDOCUMENTBACK,
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
                        requirement = Requirement.IDDOCUMENTFRONT,
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
                        requirement = Requirement.IDDOCUMENTTYPE,
                        title = ERROR_TITLE
                    )
                )
            ),
            status = VerificationPageData.Status.VERIFIED,
            submitted = false
        )
    }
}
