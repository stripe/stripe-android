package com.stripe.android.identity.navigation

import android.widget.Button
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.BaseErrorFragmentBinding
import com.stripe.android.identity.navigation.CouldNotCaptureFragment.Companion.ARG_COULD_NOT_CAPTURE_SCAN_TYPE
import com.stripe.android.identity.states.IdentityScanState.ScanType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CouldNotCaptureFragmentTest {
    @Test
    fun `ID_FRONT navigates to IDUploadFragment`() {
        testClickingFileUpload(ScanType.ID_FRONT, R.id.IDUploadFragment)
    }

    @Test
    fun `ID_BACK navigates to IDUploadFragment`() {
        testClickingFileUpload(ScanType.ID_BACK, R.id.IDUploadFragment)
    }

    @Test
    fun `DL_FRONT navigates to DriverLicenseUploadFragment`() {
        testClickingFileUpload(ScanType.DL_FRONT, R.id.driverLicenseUploadFragment)
    }

    @Test
    fun `DL_BACK navigates to DriverLicenseUploadFragment`() {
        testClickingFileUpload(ScanType.DL_BACK, R.id.driverLicenseUploadFragment)
    }

    @Test
    fun `PASSPORT navigates to PassportUploadFragment`() {
        testClickingFileUpload(ScanType.PASSPORT, R.id.passportUploadFragment)
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

    private fun testClickingFileUpload(scanType: ScanType, @IdRes destination: Int) {
        launchCameraPermissionDeniedFragment(scanType) { fileUpload, _, navController ->
            fileUpload.callOnClick()
            assertThat(navController.currentDestination?.id)
                .isEqualTo(destination)
        }
    }

    private fun testClickingRetry(
        scanType: ScanType,
        @IdRes destination: Int,
        shouldStartFromBack: Boolean
    ) {
        launchCameraPermissionDeniedFragment(scanType) { _, retry, navController ->
            retry.callOnClick()
            assertThat(navController.currentDestination?.id)
                .isEqualTo(destination)

            assertThat(
                requireNotNull(navController.backStack.last().arguments)
                [IdentityCameraScanFragment.ARG_SHOULD_START_FROM_BACK]
            ).isEqualTo(shouldStartFromBack)
        }
    }

    private fun launchCameraPermissionDeniedFragment(
        type: ScanType,
        testBlock: (Button, Button, TestNavHostController) -> Unit
    ) = launchFragmentInContainer(
        bundleOf(
            ARG_COULD_NOT_CAPTURE_SCAN_TYPE to type
        ),
        themeResId = R.style.Theme_MaterialComponents
    ) {
        CouldNotCaptureFragment()
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
        testBlock(binding.topButton, binding.bottomButton, navController)
    }
}
