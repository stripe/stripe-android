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
import com.stripe.android.identity.databinding.BaseErrorFragmentBinding
import com.stripe.android.identity.navigation.CameraPermissionDeniedFragment.Companion.ARG_SCAN_TYPE
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_TAKE_PHOTO
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraPermissionDeniedFragmentTest {
    private val mockAppSettingsOpenable = mock<AppSettingsOpenable>()

    @Test
    fun `when scan type is ID_FRONT title is set and clicking upload navigates to id upload fragment`() {
        verifyFragmentWithScanType(
            IdDocumentParam.Type.IDCARD,
            R.id.IDUploadFragment,
            R.string.id_card
        )
    }

    @Test
    fun `when scan type is DL_FRONT title is set and clicking upload navigates to driver license upload fragment`() {
        verifyFragmentWithScanType(
            IdDocumentParam.Type.DRIVINGLICENSE,
            R.id.driverLicenseUploadFragment,
            R.string.driver_license
        )
    }

    @Test
    fun `when scan type is PASSPORT title is set and clicking upload navigates to passport upload fragment`() {
        verifyFragmentWithScanType(
            IdDocumentParam.Type.PASSPORT,
            R.id.passportUploadFragment,
            R.string.passport
        )
    }

    @Test
    fun `when app setting button is clicked app setting is opened and returns to DocSelectionFragment`() {
        launchCameraPermissionDeniedFragment(IdDocumentParam.Type.IDCARD).onFragment {
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

            BaseErrorFragmentBinding.bind(it.requireView()).bottomButton.callOnClick()

            verify(mockAppSettingsOpenable).openAppSettings()
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.docSelectionFragment)
        }
    }

    private fun verifyFragmentWithScanType(
        type: IdDocumentParam.Type,
        @IdRes
        expectedDestination: Int = 0,
        @StringRes
        expectedTitleSuffix: Int = 0
    ) {
        launchCameraPermissionDeniedFragment(type).onFragment {
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

            val binding = BaseErrorFragmentBinding.bind(it.requireView())
            binding.topButton.callOnClick()

            assertThat(navController.currentDestination?.id).isEqualTo(
                expectedDestination
            )
            assertThat(
                requireNotNull(navController.backStack.last().arguments)
                [ARG_SHOULD_SHOW_TAKE_PHOTO]
            ).isEqualTo(false)
            assertThat(binding.message2.text).isEqualTo(
                it.getString(
                    R.string.upload_file_text,
                    it.getString(expectedTitleSuffix)
                )
            )
        }
    }

    private fun launchCameraPermissionDeniedFragment(
        type: IdDocumentParam.Type
    ) = launchFragmentInContainer(
        bundleOf(
            ARG_SCAN_TYPE to type
        ),
        themeResId = R.style.Theme_MaterialComponents
    ) {
        CameraPermissionDeniedFragment(mockAppSettingsOpenable)
    }
}
