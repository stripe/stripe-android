package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_ERROR
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_CAUSE
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_ERROR_CONTENT
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_ERROR_TITLE
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_GO_BACK_BUTTON_DESTINATION
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_GO_BACK_BUTTON_TEXT
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_SHOULD_FAIL
import com.stripe.android.identity.navigation.ErrorDestination.Companion.UNEXPECTED_DESTINATION
import com.stripe.android.identity.ui.ErrorScreen
import com.stripe.android.identity.ui.ErrorScreenButton
import com.stripe.android.identity.utils.clearDataAndNavigateUp
import com.stripe.android.identity.utils.navigateOnResume

/**
 * Fragment to show generic error.
 */
internal class ErrorFragment(
    private val verificationFlowFinishable: VerificationFlowFinishable,
    identityViewModelFactory: ViewModelProvider.Factory
) : BaseErrorFragment(identityViewModelFactory) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        val args = requireNotNull(arguments)
        val cause = requireNotNull(args.getSerializable(ARG_CAUSE) as? Throwable) {
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
                        val destination = args.getInt(ARG_GO_BACK_BUTTON_DESTINATION)
                        if (destination == UNEXPECTED_DESTINATION) {
                            navigateOnResume(ConsentDestination)
                        } else {
                            findNavController().let { navController ->
                                var shouldContinueNavigateUp = true
                                while (
                                    shouldContinueNavigateUp &&
                                    navController.currentDestination?.id != destination
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
}
