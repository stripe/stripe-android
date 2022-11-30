package com.stripe.android.identity.navigation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_ERROR
import com.stripe.android.identity.networking.models.Requirement.Companion.matchesFromFragment
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError
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
                            navigateOnResume(DEFAULT_BACK_BUTTON_NAVIGATION)
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

    internal companion object {
        const val ARG_ERROR_TITLE = "errorTitle"
        const val ARG_ERROR_CONTENT = "errorContent"

        // if set, shows go_back button, clicking it would navigate to the destination.
        const val ARG_GO_BACK_BUTTON_TEXT = "goBackButtonText"
        const val ARG_GO_BACK_BUTTON_DESTINATION = "goBackButtonDestination"

        // if set to true, clicking bottom button and pressBack would end flow with Failed
        const val ARG_SHOULD_FAIL = "shouldFail"
        const val ARG_CAUSE = "cause"

        // Indicates the server returns a requirementError that doesn't match with current Fragment.
        //  E.g ConsentFragment->DocSelectFragment could only have BIOMETRICCONSENT error but not IDDOCUMENTFRONT error.
        // If this happens, set the back button destination to [DEFAULT_BACK_BUTTON_DESTINATION]
        internal const val UNEXPECTED_DESTINATION = -1

        private val DEFAULT_BACK_BUTTON_NAVIGATION =
            R.id.action_errorFragment_to_consentFragment

        fun NavController.navigateToErrorFragmentWithRequirementError(
            @IdRes fromFragment: Int,
            requirementError: VerificationPageDataRequirementError
        ) {
            navigate(
                R.id.action_global_errorFragment,
                bundleOf(
                    ARG_ERROR_TITLE to (
                        requirementError.title ?: context.getString(R.string.error)
                        ),
                    ARG_ERROR_CONTENT to (
                        requirementError.body
                            ?: context.getString(R.string.unexpected_error_try_again)
                        ),
                    ARG_GO_BACK_BUTTON_DESTINATION to
                        if (requirementError.requirement.matchesFromFragment(fromFragment)) {
                            fromFragment
                        } else {
                            UNEXPECTED_DESTINATION
                        },
                    ARG_GO_BACK_BUTTON_TEXT to (
                        requirementError.backButtonText ?: context.getString(R.string.go_back)
                        ),
                    ARG_SHOULD_FAIL to false,
                    ARG_CAUSE to IllegalStateException("VerificationPageDataRequirementError: $requirementError")
                )
            )
        }

        fun NavController.navigateToErrorFragmentWithDefaultValues(
            context: Context,
            cause: Throwable
        ) {
            navigate(
                R.id.action_global_errorFragment,
                bundleOf(
                    ARG_ERROR_TITLE to context.getString(R.string.error),
                    ARG_ERROR_CONTENT to context.getString(R.string.unexpected_error_try_again),
                    ARG_GO_BACK_BUTTON_DESTINATION to R.id.consentFragment,
                    ARG_GO_BACK_BUTTON_TEXT to context.getString(R.string.go_back),
                    ARG_SHOULD_FAIL to false,
                    ARG_CAUSE to cause
                )
            )
        }

        /**
         * Navigate to error fragment with failed reason. This would be the final destination of
         * verification flow, clicking back button would end the follow with
         * [IdentityVerificationSheet.VerificationFlowResult.Failed] with [failedReason].
         */
        fun NavController.navigateToErrorFragmentWithFailedReason(
            context: Context,
            failedReason: Throwable
        ) {
            navigate(
                R.id.action_global_errorFragment,
                bundleOf(
                    ARG_ERROR_TITLE to context.getString(R.string.error),
                    ARG_ERROR_CONTENT to context.getString(R.string.unexpected_error_try_again),
                    ARG_GO_BACK_BUTTON_TEXT to context.getString(R.string.go_back),
                    ARG_SHOULD_FAIL to true,
                    ARG_CAUSE to failedReason
                )
            )
        }
    }
}
