package com.stripe.android.identity.navigation

import android.content.Context
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult.Failed
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError.Requirement.Companion.matchesFromFragment
import com.stripe.android.identity.utils.navigateUpAndSetArgForUploadFragment

/**
 * Fragment to show generic error.
 */
internal class ErrorFragment(
    private val verificationFlowFinishable: VerificationFlowFinishable
) : BaseErrorFragment() {
    override fun onCustomizingViews() {
        val args = requireNotNull(arguments)
        title.text = args[ARG_ERROR_TITLE] as String
        message1.text = args[ARG_ERROR_CONTENT] as String
        message2.visibility = View.GONE

        topButton.visibility = View.GONE

        if (args.getInt(ARG_GO_BACK_BUTTON_DESTINATION) == UNSET_DESTINATION &&
            !args.containsKey(ARG_FAILED_REASON)
        ) {
            bottomButton.visibility = View.GONE
        } else {
            bottomButton.text = args[ARG_GO_BACK_BUTTON_TEXT] as String
            bottomButton.visibility = View.VISIBLE

            // If this is final destination, clicking bottom button and pressBack would end flow
            (args.getSerializable(ARG_FAILED_REASON) as? Throwable)?.let { failedReason ->
                bottomButton.setOnClickListener {
                    verificationFlowFinishable.finishWithResult(
                        Failed(failedReason)
                    )
                }
                requireActivity().onBackPressedDispatcher.addCallback(
                    this,
                    object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            verificationFlowFinishable.finishWithResult(
                                Failed(failedReason)
                            )
                        }
                    }
                )
            } ?: run {
                bottomButton.setOnClickListener {
                    val destination = args[ARG_GO_BACK_BUTTON_DESTINATION] as Int
                    if (destination == UNEXPECTED_DESTINATION) {
                        findNavController().navigate(DEFAULT_BACK_BUTTON_DESTINATION)
                    } else {
                        findNavController().let { navController ->
                            while (navController.currentDestination?.id != destination) {
                                navController.navigateUpAndSetArgForUploadFragment()
                            }
                        }
                    }
                }
            }
        }
    }

    internal companion object {
        const val ARG_ERROR_TITLE = "errorTitle"
        const val ARG_ERROR_CONTENT = "errorContent"

        // if set, shows go_back button, clicking it would navigate to the destination.
        const val ARG_GO_BACK_BUTTON_TEXT = "goBackButtonText"
        const val ARG_GO_BACK_BUTTON_DESTINATION = "goBackButtonDestination"
        const val ARG_FAILED_REASON = "failedReason"
        private const val UNSET_DESTINATION = 0

        // Indicates the server returns a requirementError that doesn't match with current Fragment.
        //  E.g ConsentFragment->DocSelectFragment could only have BIOMETRICCONSENT error but not IDDOCUMENTFRONT error.
        // If this happens, set the back button destination to [DEFAULT_BACK_BUTTON_DESTINATION]
        internal const val UNEXPECTED_DESTINATION = -1

        private val DEFAULT_BACK_BUTTON_DESTINATION =
            R.id.action_errorFragment_to_consentFragment

        fun NavController.navigateToErrorFragmentWithRequirementError(
            @IdRes fromFragment: Int,
            requirementError: VerificationPageDataRequirementError
        ) {
            navigate(
                R.id.action_global_errorFragment,
                bundleOf(
                    ARG_ERROR_TITLE to requirementError.title,
                    ARG_ERROR_CONTENT to requirementError.body,
                    ARG_GO_BACK_BUTTON_DESTINATION to
                        if (requirementError.requirement.matchesFromFragment(fromFragment))
                            fromFragment
                        else
                            UNEXPECTED_DESTINATION,
                    ARG_GO_BACK_BUTTON_TEXT to requirementError.backButtonText,
                    // TODO(ccen) build continue button after backend behavior is finalized
                    // ARG_CONTINUE_BUTTON_TEXT to requirementError.continueButtonText,
                )
            )
        }

        fun NavController.navigateToErrorFragmentWithDefaultValues(context: Context) {
            navigate(
                R.id.action_global_errorFragment,
                bundleOf(
                    ARG_ERROR_TITLE to context.getString(R.string.error),
                    ARG_ERROR_CONTENT to context.getString(R.string.unexpected_error_try_again),
                    ARG_GO_BACK_BUTTON_DESTINATION to R.id.action_errorFragment_to_consentFragment,
                    ARG_GO_BACK_BUTTON_TEXT to context.getString(R.string.go_back)
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
                    ARG_FAILED_REASON to failedReason
                )
            )
        }
    }
}
