package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_ERROR
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_ERROR_CONTENT
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_ERROR_TITLE
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_GO_BACK_BUTTON_DESTINATION
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_GO_BACK_BUTTON_TEXT
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_SHOULD_FAIL
import com.stripe.android.identity.navigation.ErrorDestination.Companion.UNEXPECTED_ROUTE
import com.stripe.android.identity.ui.ErrorScreen
import com.stripe.android.identity.ui.ErrorScreenButton
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Fragment to show generic error.
 */
internal class ErrorFragment(
    private val verificationFlowFinishable: VerificationFlowFinishable,
    identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {

    private val identityViewModel: IdentityViewModel by activityViewModels {
        identityViewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        val args = requireNotNull(arguments)

        val cause = requireNotNull(identityViewModel.errorCause.value) {
            "cause of error is null"
        }

        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.genericError(
                message = cause.message,
                stackTrace = cause.stackTraceToString()
            )
        )
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ErrorScreen(
                identityViewModel = identityViewModel,
                title = requireNotNull(args.getString(ARG_ERROR_TITLE)),
                message1 = requireNotNull(args.getString(ARG_ERROR_CONTENT)),
                bottomButton = ErrorScreenButton(
                    buttonText = requireNotNull(args.getString(ARG_GO_BACK_BUTTON_TEXT))
                ) {
                    identityViewModel.screenTracker.screenTransitionStart(
                        SCREEN_NAME_ERROR
                    )
                    if (args.getBoolean(ARG_SHOULD_FAIL, false)) {
                        verificationFlowFinishable.finishWithResult(
                            IdentityVerificationSheet.VerificationFlowResult.Failed(cause)
                        )
                    } else {
                        val destination = args.getString(ARG_GO_BACK_BUTTON_DESTINATION)

                        if (destination == UNEXPECTED_ROUTE) {
                            findNavController().navigateTo(ConsentDestination)
                        } else {
                            findNavController().let { navController ->
                                var shouldContinueNavigateUp = true
                                // TODO(ccen) After migrated to Jetpack compose, remove [toFragmentId]
                                // and use navController.currentDestination?.route == destination instead
                                while (
                                    shouldContinueNavigateUp &&
                                    navController.currentDestination?.id != destination?.toFragmentId()
                                ) {
                                    shouldContinueNavigateUp =
                                        navController.clearDataAndNavigateUp(identityViewModel)
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    /**
     * Temporarily convert String route back to a Fragment ID.
     * TODO(ccen) Remove this method after migrated to Jetpack compose.
     */
    private fun String.toFragmentId(): Int {
        return when (this) {
            CameraPermissionDeniedDestination.ROUTE.route -> {
                R.id.cameraPermissionDeniedFragment
            }
            CouldNotCaptureDestination.ROUTE.route -> {
                R.id.couldNotCaptureFragment
            }
            ErrorDestination.ROUTE.route -> {
                R.id.errorFragment
            }
            ConfirmationDestination.ROUTE.route -> {
                R.id.confirmationFragment
            }
            DocSelectionDestination.ROUTE.route -> {
                R.id.docSelectionFragment
            }
            ConsentDestination.ROUTE.route -> {
                R.id.consentFragment
            }
            PassportUploadDestination.ROUTE.route -> {
                R.id.passportUploadFragment
            }
            IDUploadDestination.ROUTE.route -> {
                R.id.IDUploadFragment
            }
            DriverLicenseUploadDestination.ROUTE.route -> {
                R.id.driverLicenseUploadFragment
            }
            PassportScanDestination.ROUTE.route -> {
                R.id.passportScanFragment
            }
            IDScanDestination.ROUTE.route -> {
                R.id.IDScanFragment
            }
            DriverLicenseScanDestination.ROUTE.route -> {
                R.id.driverLicenseScanFragment
            }
            SelfieDestination.ROUTE.route -> {
                R.id.selfieFragment
            }
            else -> {
                throw IllegalStateException("Unknown route: $this")
            }
        }
    }
}
