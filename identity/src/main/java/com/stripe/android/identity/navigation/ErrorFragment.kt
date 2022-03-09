package com.stripe.android.identity.navigation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.ErrorFragmentBinding
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError

/**
 * Fragment to show generic error.
 */
internal class ErrorFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ErrorFragmentBinding.inflate(inflater, container, false)
        val args = requireNotNull(arguments)
        binding.errorTitle.text = args[ARG_ERROR_TITLE] as String
        binding.errorContent.text = args[ARG_ERROR_CONTENT] as String
        if (args.getInt(ARG_GO_BACK_BUTTON_DESTINATION) != UNSET_DESTINATION) {
            binding.goBack.text = args[ARG_GO_BACK_BUTTON_TEXT] as String
            binding.goBack.visibility = View.VISIBLE
            binding.goBack.setOnClickListener {
                // Can only go back to consent page at the moment
                findNavController().navigate(args[ARG_GO_BACK_BUTTON_DESTINATION] as Int)
            }
        }

        return binding.root
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
