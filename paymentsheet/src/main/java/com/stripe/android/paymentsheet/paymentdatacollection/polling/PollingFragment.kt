package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stripe.android.StripeIntentResult
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.uicore.StripeTheme
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private const val KEY_POLLING_ARGS = "KEY_POLLING_ARGS"

internal class PollingFragment : BottomSheetDialogFragment() {

    private val args: PollingContract.Args by lazy {
        @Suppress("DEPRECATION")
        requireNotNull(arguments?.getParcelable(KEY_POLLING_ARGS))
    }

    private val viewModel by viewModels<PollingViewModel> {
        PollingViewModel.Factory {
            PollingViewModel.Args(
                clientSecret = args.clientSecret,
                timeLimit = args.timeLimitInSeconds.seconds,
                initialDelay = args.initialDelayInSeconds.seconds,
                maxAttempts = args.maxAttempts,
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                StripeTheme {
                    Surface(
                        color = MaterialTheme.colors.surface,
                    ) {
                        PollingScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            owner = viewLifecycleOwner,
            enabled = false,
            onBackPressed = {}
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(this@PollingFragment::handleUiState)
            }
        }
    }

    private fun handleUiState(uiState: PollingUiState) {
        if (uiState.pollingState == PollingState.Success) {
            finishWithSuccess()
        } else if (uiState.pollingState == PollingState.Canceled) {
            finishWithCancellation()
        }
    }

    private fun finishWithSuccess() {
        val successResult = PaymentFlowResult.Unvalidated(
            clientSecret = args.clientSecret,
            flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
        )
        finishWithResult(successResult)
    }

    private fun finishWithCancellation() {
        val cancelResult = PaymentFlowResult.Unvalidated(
            clientSecret = args.clientSecret,
            flowOutcome = StripeIntentResult.Outcome.CANCELED,
            canCancelSource = false,
        )
        finishWithResult(cancelResult)
    }

    private fun finishWithResult(paymentFlowResult: PaymentFlowResult.Unvalidated) {
        setFragmentResult(
            requestKey = KEY_FRAGMENT_RESULT,
            result = paymentFlowResult.toBundle(),
        )
    }

    companion object {

        const val KEY_FRAGMENT_RESULT = "KEY_FRAGMENT_RESULT_PollingFragment"

        fun newInstance(args: PollingContract.Args): PollingFragment {
            return PollingFragment().apply {
                arguments = bundleOf(
                    KEY_POLLING_ARGS to args
                )
            }
        }
    }
}
