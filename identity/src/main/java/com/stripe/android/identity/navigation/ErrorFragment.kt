package com.stripe.android.identity.navigation

import android.content.Context
import android.view.View
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError

/**
 * Fragment to show generic error.
 */
internal class ErrorFragment : BaseErrorFragment() {

    override fun onCustomizingViews() {
        val args = requireNotNull(arguments)
        title.text = args[ARG_ERROR_TITLE] as String
        message1.text = args[ARG_ERROR_CONTENT] as String
        message2.visibility = View.GONE

        topButton.visibility = View.GONE
        if (args.getInt(ARG_GO_BACK_BUTTON_DESTINATION) == UNSET_DESTINATION) {
            bottomButton.visibility = View.GONE
        } else {
            bottomButton.text = args[ARG_GO_BACK_BUTTON_TEXT] as String
            bottomButton.visibility = View.VISIBLE
            bottomButton.setOnClickListener {
                // Can only go back to consent page at the moment
                findNavController().navigate(args[ARG_GO_BACK_BUTTON_DESTINATION] as Int)
            }
        }
    }

    internal companion object {
        const val ARG_ERROR_TITLE = "errorTitle"
        const val ARG_ERROR_CONTENT = "errorContent"

        // if set, shows go_back button, clicking it would navigate to the destination.
        const val ARG_GO_BACK_BUTTON_TEXT = "goBackButtonText"
        const val ARG_GO_BACK_BUTTON_DESTINATION = "goBackButtonDestination"
        private const val UNSET_DESTINATION = 0

        fun NavController.navigateToErrorFragmentWithRequirementErrorAndDestination(
            requirementError: VerificationPageDataRequirementError,
            @IdRes backButtonDestination: Int
        ) {
            navigate(
                R.id.action_global_errorFragment,
                bundleOf(
                    ARG_ERROR_TITLE to requirementError.title,
                    ARG_ERROR_CONTENT to requirementError.body,
                    ARG_GO_BACK_BUTTON_DESTINATION to backButtonDestination,
                    ARG_GO_BACK_BUTTON_TEXT to requirementError.buttonText,
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
                    ARG_GO_BACK_BUTTON_TEXT to context.getString(R.string.go_back),
                )
            )
        }
    }
}
