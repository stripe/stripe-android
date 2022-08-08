package com.stripe.android.identity.navigation

import android.view.View
import android.widget.Button
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EVENT_META_DATA
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_ERROR
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.databinding.BaseErrorFragmentBinding
import com.stripe.android.identity.navigation.CouldNotCaptureFragment.Companion.ARG_COULD_NOT_CAPTURE_SCAN_TYPE
import com.stripe.android.identity.navigation.CouldNotCaptureFragment.Companion.ARG_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.states.IdentityScanState.ScanType
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_CHOOSE_PHOTO
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class CouldNotCaptureFragmentTest {
    private val mockScreenTracker = mock<ScreenTracker>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { identityAnalyticsRequestFactory } doReturn
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            )

        on { screenTracker } doReturn mockScreenTracker
        on { uiContext } doReturn testDispatcher
        on { workContext } doReturn testDispatcher
    }

    @Test
    fun `ID_FRONT navigates to IDUploadFragment when requireLiveCapture true`() {
        testClickingFileUpload(ScanType.ID_FRONT, true, R.id.IDUploadFragment)
    }

    @Test
    fun `ID_FRONT navigates to IDUploadFragment when requireLiveCapture false`() {
        testClickingFileUpload(ScanType.ID_FRONT, false, R.id.IDUploadFragment)
    }

    @Test
    fun `ID_BACK navigates to IDUploadFragment when requireLiveCapture true`() {
        testClickingFileUpload(ScanType.ID_BACK, true, R.id.IDUploadFragment)
    }

    @Test
    fun `ID_BACK navigates to IDUploadFragment when requireLiveCapture false`() {
        testClickingFileUpload(ScanType.ID_BACK, false, R.id.IDUploadFragment)
    }

    @Test
    fun `DL_FRONT navigates to DriverLicenseUploadFragment when requireLiveCapture true`() {
        testClickingFileUpload(ScanType.DL_FRONT, true, R.id.driverLicenseUploadFragment)
    }

    @Test
    fun `DL_FRONT navigates to DriverLicenseUploadFragment when requireLiveCapture false`() {
        testClickingFileUpload(ScanType.DL_FRONT, false, R.id.driverLicenseUploadFragment)
    }

    @Test
    fun `DL_BACK navigates to DriverLicenseUploadFragment when requireLiveCapture true`() {
        testClickingFileUpload(ScanType.DL_BACK, true, R.id.driverLicenseUploadFragment)
    }

    @Test
    fun `DL_BACK navigates to DriverLicenseUploadFragment when requireLiveCapture false`() {
        testClickingFileUpload(ScanType.DL_BACK, false, R.id.driverLicenseUploadFragment)
    }

    @Test
    fun `PASSPORT navigates to PassportUploadFragment when requireLiveCapture true`() {
        testClickingFileUpload(ScanType.PASSPORT, true, R.id.passportUploadFragment)
    }

    @Test
    fun `PASSPORT navigates to PassportUploadFragment when requireLiveCapture false`() {
        testClickingFileUpload(ScanType.PASSPORT, false, R.id.passportUploadFragment)
    }

    @Test
    fun `ID_FRONT navigates to IDScanFragment`() {
        testClickingRetry(ScanType.ID_FRONT, R.id.IDScanFragment, false)
    }

    @Test
    fun `ID_BACK navigates to IDScanFragment`() {
        testClickingRetry(ScanType.ID_BACK, R.id.IDScanFragment, true)
    }

    @Test
    fun `DL_FRONT navigates to DriverLicenseScanFragment`() {
        testClickingRetry(ScanType.DL_FRONT, R.id.driverLicenseScanFragment, false)
    }

    @Test
    fun `DL_BACK navigates to DriverLicenseScanFragment`() {
        testClickingRetry(ScanType.DL_BACK, R.id.driverLicenseScanFragment, true)
    }

    @Test
    fun `PASSPORT navigates to PassportScanFragment`() {
        testClickingRetry(ScanType.PASSPORT, R.id.passportScanFragment, false)
    }

    @Test
    fun `SELFIE navigates to SelfieFragment`() {
        testClickingRetry(ScanType.SELFIE, R.id.selfieFragment, false)
    }

    private fun testClickingFileUpload(
        scanType: ScanType,
        requireLiveCapture: Boolean,
        @IdRes destination: Int
    ) {
        launchCameraPermissionDeniedFragment(
            scanType,
            requireLiveCapture
        ) { fileUpload, _, navController ->
            fileUpload.callOnClick()
            verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_ERROR), any())
            assertThat(
                requireNotNull(navController.backStack.last().arguments)
                [ARG_SHOULD_SHOW_CHOOSE_PHOTO]
            ).isEqualTo(!requireLiveCapture)

            assertThat(navController.currentDestination?.id)
                .isEqualTo(destination)
        }
    }

    private fun testClickingRetry(
        scanType: ScanType,
        @IdRes destination: Int,
        shouldStartFromBack: Boolean
    ) {
        launchCameraPermissionDeniedFragment(scanType, true) { _, retry, navController ->
            retry.callOnClick()
            verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_ERROR), any())
            assertThat(navController.currentDestination?.id)
                .isEqualTo(destination)

            assertThat(
                requireNotNull(navController.backStack.last().arguments)
                [IdentityDocumentScanFragment.ARG_SHOULD_START_FROM_BACK]
            ).isEqualTo(shouldStartFromBack)
        }
    }

    private fun launchCameraPermissionDeniedFragment(
        type: ScanType,
        requireLiveCapture: Boolean,
        testBlock: (Button, Button, TestNavHostController) -> Unit
    ) = launchFragmentInContainer(
        bundleOf(
            ARG_COULD_NOT_CAPTURE_SCAN_TYPE to type,
            ARG_REQUIRE_LIVE_CAPTURE to requireLiveCapture
        ),
        themeResId = R.style.Theme_MaterialComponents
    ) {
        CouldNotCaptureFragment(viewModelFactoryFor(mockIdentityViewModel))
    }.onFragment {
        val binding: BaseErrorFragmentBinding = BaseErrorFragmentBinding.bind(it.requireView())
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )
        navController.setGraph(
            R.navigation.identity_nav_graph
        )
        navController.setCurrentDestination(R.id.couldNotCaptureFragment)
        Navigation.setViewNavController(
            it.requireView(),
            navController
        )

        assertThat(binding.titleText.text).isEqualTo(it.getString(R.string.could_not_capture_title))
        assertThat(binding.message1.text).isEqualTo(it.getString(R.string.could_not_capture_body1))
        assertThat(binding.bottomButton.text).isEqualTo(it.getString(R.string.try_again))

        if (type == ScanType.SELFIE) {
            assertThat(binding.topButton.visibility).isEqualTo(View.GONE)
            assertThat(binding.message2.visibility).isEqualTo(View.GONE)
        } else {
            assertThat(binding.topButton.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.message2.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.topButton.text).isEqualTo(it.getString(R.string.file_upload))
            assertThat(binding.message2.text).isEqualTo(it.getString(R.string.could_not_capture_body2))
        }

        runBlocking {
            verify(mockScreenTracker).screenTransitionFinish(eq(SCREEN_NAME_ERROR))
        }
        verify(mockIdentityViewModel).sendAnalyticsRequest(
            argThat {
                eventName == EVENT_SCREEN_PRESENTED &&
                    (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCREEN_NAME] == SCREEN_NAME_ERROR
            }
        )

        testBlock(binding.topButton, binding.bottomButton, navController)
    }
}
