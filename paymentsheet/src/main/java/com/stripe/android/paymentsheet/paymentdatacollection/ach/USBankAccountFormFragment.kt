package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.PaymentOptionsActivity
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.SaveForFutureUseElementUI
import kotlinx.coroutines.launch

/**
 * Fragment that displays a form for us_bank_account payment data collection
 */
internal class USBankAccountFormFragment : Fragment() {

    private val formArgs by lazy {
        requireNotNull(
            requireArguments().getParcelable<FormFragmentArguments>(EXTRA_CONFIG)
        )
    }

    private val paymentSheetViewModelFactory: ViewModelProvider.Factory by lazy {
        PaymentSheetViewModel.Factory(
            { requireActivity().application },
            {
                requireNotNull(
                    requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
                )
            },
            (activity as? AppCompatActivity) ?: this,
            (activity as? AppCompatActivity)?.intent?.extras
        )
    }

    private val paymentOptionsViewModelFactory: ViewModelProvider.Factory by lazy {
        PaymentOptionsViewModel.Factory(
            { requireActivity().application },
            {
                requireNotNull(
                    requireArguments().getParcelable(PaymentOptionsActivity.EXTRA_STARTER_ARGS)
                )
            },
            (activity as? AppCompatActivity) ?: this,
            (activity as? AppCompatActivity)?.intent?.extras
        )
    }

    private val sheetViewModel: BaseSheetViewModel<*>? by lazy {
        when (requireActivity()) {
            is PaymentOptionsActivity -> {
                activityViewModels<PaymentOptionsViewModel> {
                    paymentOptionsViewModelFactory
                }.value
            }
            is PaymentSheetActivity -> {
                activityViewModels<PaymentSheetViewModel> {
                    paymentSheetViewModelFactory
                }.value
            }
            else -> {
                null
            }
        }
    }

    private val completePayment by lazy {
        sheetViewModel is PaymentSheetViewModel
    }

    private val clientSecret by lazy {
        when (val intent = sheetViewModel?.stripeIntent?.value) {
            is PaymentIntent -> PaymentIntentClientSecret(intent.clientSecret!!)
            is SetupIntent -> SetupIntentClientSecret(intent.clientSecret!!)
            else -> null
        }
    }

    private val viewModel by activityViewModels<USBankAccountFormViewModel> {
        USBankAccountFormViewModel.Factory(
            applicationSupplier = { requireActivity().application },
            argsSupplier = {
                val savedPaymentMethod =
                    sheetViewModel?.newPaymentSelection as? PaymentSelection.New.USBankAccount

                USBankAccountFormViewModel.Args(
                    formArgs = formArgs,
                    isCompleteFlow = sheetViewModel is PaymentSheetViewModel,
                    clientSecret = clientSecret,
                    savedPaymentMethod = savedPaymentMethod,
                    shippingDetails = sheetViewModel?.config?.shippingDetails,
                    onConfirmStripeIntent = { params ->
                        (sheetViewModel as? PaymentSheetViewModel)?.confirmStripeIntent(params)
                    },
                    onUpdateSelectionAndFinish = { paymentSelection ->
                        sheetViewModel?.updateSelection(paymentSelection)
                        sheetViewModel?.onFinish()
                    }
                )
            },
            owner = this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.registerFragment(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sheetViewModel?.primaryButtonState?.observe(viewLifecycleOwner) { state ->
                    // When the primary button state is StartProcessing or FinishProcessing
                    // we should disable the inputs of this form. StartProcessing shows the loading
                    // spinner, FinishProcessing shows the checkmark animation
                    viewModel.setProcessing(
                        state is PrimaryButton.State.StartProcessing ||
                            state is PrimaryButton.State.FinishProcessing
                    )
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.requiredFields.collect {
                    sheetViewModel?.updatePrimaryButtonUIState(
                        sheetViewModel?.primaryButtonUIState?.value?.copy(
                            enabled = it
                        )
                    )
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveForFutureUse.collect { saved ->
                    updateMandateText(
                        if (saved) {
                            getString(
                                R.string.stripe_paymentsheet_ach_save_mandate,
                                viewModel.formattedMerchantName()
                            )
                        } else {
                            ACHText.getContinueMandateText(requireContext())
                        }
                    )
                }
            }
        }

        setContent {
            PaymentsTheme {
                val currentScreenState by viewModel.currentScreenState.collectAsState()
                val processing by viewModel.processing.collectAsState(false)

                LaunchedEffect(currentScreenState) {
                    sheetViewModel?.onError(currentScreenState.error)

                    val shouldProcess =
                        currentScreenState is USBankAccountFormScreenState.NameAndEmailCollection ||
                            completePayment
                    val enabled = if (
                        currentScreenState is
                            USBankAccountFormScreenState.NameAndEmailCollection
                    ) {
                        viewModel.requiredFields.value
                    } else {
                        true
                    }

                    updatePrimaryButton(
                        text = currentScreenState.primaryButtonText,
                        onClick = {
                            viewModel.handlePrimaryButtonClick(currentScreenState)
                        },
                        enabled = enabled,
                        shouldProcess = shouldProcess
                    )

                    updateMandateText(currentScreenState.mandateText)
                }

                USBankAccountScreen(
                    currentScreenState = currentScreenState,
                    enabled = !processing,
                    saveForFutureUseUI = { saveForFutureUsage ->
                        if (formArgs.showCheckbox) {
                            SaveForFutureUseElementUI(
                                true,
                                viewModel.saveForFutureUseElement.apply {
                                    this.controller.onValueChange(saveForFutureUsage)
                                }
                            )
                        }
                    },
                    nameController = viewModel.nameController,
                    emailController = viewModel.emailController,
                    onConfirm = {
                        viewModel.reset()
                    }
                )
            }
        }
    }

    override fun onDetach() {
        sheetViewModel?.updateBelowButtonText(null)
        sheetViewModel?.updatePrimaryButtonUIState(null)
        viewModel.onDestroy()
        super.onDetach()
    }

    private fun updatePrimaryButton(
        text: String?,
        onClick: () -> Unit,
        shouldProcess: Boolean = true,
        enabled: Boolean = true,
        visible: Boolean = true
    ) {
        sheetViewModel?.updatePrimaryButtonState(PrimaryButton.State.Ready)
        sheetViewModel?.updatePrimaryButtonUIState(
            PrimaryButton.UIState(
                label = text,
                onClick = {
                    if (shouldProcess) {
                        sheetViewModel?.updatePrimaryButtonState(
                            PrimaryButton.State.StartProcessing
                        )
                    }
                    onClick()
                    sheetViewModel?.updatePrimaryButtonUIState(
                        sheetViewModel?.primaryButtonUIState?.value?.copy(
                            onClick = null
                        )
                    )
                },
                enabled = enabled,
                visible = visible
            )
        )
    }

    private fun updateMandateText(mandateText: String?) {
        val microdepositsText =
            if (
                viewModel.currentScreenState.value
                    is USBankAccountFormScreenState.VerifyWithMicrodeposits
            ) {
                getString(
                    R.string.stripe_paymentsheet_microdeposit,
                    viewModel.formattedMerchantName()
                )
            } else {
                ""
            }
        val updatedText = mandateText?.let {
            """
                $microdepositsText
                
                $mandateText
            """.trimIndent()
        } ?: run { null }
        sheetViewModel?.updateBelowButtonText(updatedText)
    }

    companion object {
        const val EXTRA_CONFIG = "com.stripe.android.paymentsheet.extra_config"
    }
}
