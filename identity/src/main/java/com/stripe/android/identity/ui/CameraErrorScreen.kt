package com.stripe.android.identity.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.navigation.DocumentUploadDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.camera.R as CameraR

/**
 * Shown when the camera cannot be used for document capture. When the session requires live
 * capture there is no other way to capture the document, so the screen has no button and the user
 * can only leave the flow.
 */
@Composable
internal fun CameraErrorScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    requireLiveCapture: Boolean
) {
    ErrorScreen(
        identityViewModel = identityViewModel,
        title = stringResource(id = CameraR.string.stripe_error_camera_title),
        message1 = stringResource(id = CameraR.string.stripe_error_camera_open),
        bottomButton = if (requireLiveCapture) {
            null
        } else {
            ErrorScreenButton(
                buttonText = stringResource(id = R.string.stripe_upload_a_photo)
            ) {
                identityViewModel.screenTracker.screenTransitionStart(
                    IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR
                )
                navController.navigateTo(DocumentUploadDestination)
            }
        }
    )
}
