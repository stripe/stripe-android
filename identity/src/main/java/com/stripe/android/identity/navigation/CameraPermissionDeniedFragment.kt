package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_ERROR
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.ui.ErrorScreen
import com.stripe.android.identity.ui.ErrorScreenButton
import com.stripe.android.identity.utils.navigateOnResume
import com.stripe.android.identity.utils.navigateToUploadFragment

/**
 * Fragment to show user denies camera permission.
 */
internal class CameraPermissionDeniedFragment(
    private val appSettingsOpenable: AppSettingsOpenable,
    identityViewModelFactory: ViewModelProvider.Factory
) : BaseErrorFragment(identityViewModelFactory) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        val scanType = arguments?.getSerializable(ARG_SCAN_TYPE) as? CollectedDataParam.Type
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ErrorScreen(
                title = stringResource(id = R.string.camera_permission),
                message1 = stringResource(id = R.string.grant_camera_permission_text),
                message2 = scanType?.let {
                    stringResource(R.string.upload_file_text, it.getDisplayName())
                },
                topButton = scanType?.let {
                    ErrorScreenButton(
                        buttonText = stringResource(id = R.string.file_upload)
                    ) {
                        identityViewModel.screenTracker.screenTransitionStart(SCREEN_NAME_ERROR)
                        navigateToUploadFragment(
                            it.toUploadDestinationId(),
                            shouldShowTakePhoto = false,
                            shouldShowChoosePhoto = true
                        )
                    }
                },
                bottomButton = ErrorScreenButton(
                    buttonText = stringResource(id = R.string.app_settings)
                ) {
                    appSettingsOpenable.openAppSettings()
                    // navigate back to DocSelectFragment, so that when user is back to the app from settings
                    // the camera permission check can be triggered again from there.
                    navigateOnResume(R.id.action_cameraPermissionDeniedFragment_to_docSelectionFragment)
                }
            )
        }
    }

    private fun CollectedDataParam.Type.getDisplayName() =
        when (this) {
            CollectedDataParam.Type.IDCARD -> {
                getString(R.string.id_card)
            }
            CollectedDataParam.Type.DRIVINGLICENSE -> {
                getString(R.string.driver_license)
            }
            CollectedDataParam.Type.PASSPORT -> {
                getString(R.string.passport)
            }
        }

    internal companion object {
        const val ARG_SCAN_TYPE = "scanType"

        @IdRes
        private fun CollectedDataParam.Type.toUploadDestinationId() =
            when (this) {
                CollectedDataParam.Type.IDCARD ->
                    R.id.action_cameraPermissionDeniedFragment_to_IDUploadFragment
                CollectedDataParam.Type.DRIVINGLICENSE ->
                    R.id.action_cameraPermissionDeniedFragment_to_driverLicenseUploadFragment
                CollectedDataParam.Type.PASSPORT ->
                    R.id.action_cameraPermissionDeniedFragment_to_passportUploadFragment
            }
    }
}
