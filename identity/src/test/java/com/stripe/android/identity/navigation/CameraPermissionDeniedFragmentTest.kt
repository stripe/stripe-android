package com.stripe.android.identity.navigation

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.CameraPermissionDeniedFragmentBinding
import com.stripe.android.identity.navigation.CameraPermissionDeniedFragment.Companion.ARG_SCAN_TYPE
import com.stripe.android.identity.states.IdentityScanState
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class CameraPermissionDeniedFragmentTest {
    private val mockAppSettingsOpenable = mock<AppSettingsOpenable>()

    @Test
    fun `when scan type is ID_FRONT title is set and clicking upload navigates to id upload fragment`() {
        verifyFragmentWithScanType(
            IdentityScanState.ScanType.ID_FRONT,
            R.id.IDUploadFragment,
            R.string.displayname_id
        )
    }

    @Test
    fun `when scan type is DL_FRONT title is set and clicking upload navigates to driver license upload fragment`() {
        verifyFragmentWithScanType(
            IdentityScanState.ScanType.DL_FRONT,
            R.id.driverLicenseUploadFragment,
            R.string.displayname_dl
        )
    }

    @Test
    fun `when scan type is PASSPORT title is set and clicking upload navigates to passport upload fragment`() {
        verifyFragmentWithScanType(
            IdentityScanState.ScanType.PASSPORT,
            R.id.passportUploadFragment,
            R.string.displayname_passport
        )
    }

    @Test
    fun `when scan type is ID_BACK clicking upload throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            verifyFragmentWithScanType(
                IdentityScanState.ScanType.ID_BACK
            )
        }
    }

    @Test
    fun `when app setting button is clicked app setting is opened`() {
        launchCameraPermissionDeniedFragment(IdentityScanState.ScanType.ID_FRONT).onFragment {
            CameraPermissionDeniedFragmentBinding.bind(it.requireView()).appSettings.callOnClick()

            verify(mockAppSettingsOpenable).openAppSettings()
        }
    }

    private fun verifyFragmentWithScanType(
        identityScanType: IdentityScanState.ScanType,
        @IdRes
        expectedDestination: Int = 0,
        @StringRes
        expectedTitleSuffix: Int = 0
    ) {
        launchCameraPermissionDeniedFragment(identityScanType).onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.cameraPermissionDeniedFragment)
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )

            val binding = CameraPermissionDeniedFragmentBinding.bind(it.requireView())
            binding.fileUpload.callOnClick()

            assertThat(navController.currentDestination?.id).isEqualTo(
                expectedDestination
            )

            assertThat(binding.uploadFileText.text).isEqualTo(
                it.getString(
                    R.string.upload_file_text,
                    it.getString(expectedTitleSuffix)
                )
            )
        }
    }

    private fun launchCameraPermissionDeniedFragment(
        identityScanType: IdentityScanState.ScanType
    ) = launchFragmentInContainer(
        bundleOf(
            ARG_SCAN_TYPE to identityScanType
        ),
        themeResId = R.style.Theme_MaterialComponents
    ) {
        CameraPermissionDeniedFragment(mockAppSettingsOpenable)
    }
}
