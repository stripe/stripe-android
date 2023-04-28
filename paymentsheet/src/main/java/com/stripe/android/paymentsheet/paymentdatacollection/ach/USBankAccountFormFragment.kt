package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState.BillingDetailsCollection
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.utils.launchAndCollectIn
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.StripeTheme
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch

/**
 * Fragment that displays a form for us_bank_account payment data collection
 */
internal class USBankAccountFormFragment : Fragment() {

    private val formArgs by lazy {
        requireNotNull(
            (requireActivity() as BaseSheetActivity<*>).formArgs
        )
    }

    private val paymentSheetViewModelFactory: ViewModelProvider.Factory by lazy {
        PaymentSheetViewModel.Factory {
            requireNotNull(
                @Suppress("DEPRECATION")
                requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
            )
        }
    }

    private val paymentOptionsViewModelFactory: ViewModelProvider.Factory by lazy {
        PaymentOptionsViewModel.Factory {
            requireNotNull(
                @Suppress("DEPRECATION")
                requireArguments().getParcelable(PaymentOptionsActivity.EXTRA_STARTER_ARGS)
            )
        }
    }

    private val sheetViewModel: BaseSheetViewModel? by lazy {
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
        USBankAccountFormViewModel.Factory {
            val savedPaymentMethod =
                sheetViewModel?.newPaymentSelection as? PaymentSelection.New.USBankAccount

            USBankAccountFormViewModel.Args(
                formArgs = formArgs,
                isCompleteFlow = sheetViewModel is PaymentSheetViewModel,
                clientSecret = clientSecret,
                savedPaymentMethod = savedPaymentMethod,
                shippingDetails = sheetViewModel?.config?.shippingDetails,
            )
        }
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

        viewModel.result.launchAndCollectIn(viewLifecycleOwner) { result ->
            sheetViewModel?.handleUSBankAccountConfirmed(result)
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sheetViewModel?.primaryButtonState?.launchAndCollectIn(viewLifecycleOwner) { state ->
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
                viewModel.requiredFields.collect { hasRequiredFields ->
                    sheetViewModel?.updateCustomPrimaryButtonUiState {
                        it?.copy(enabled = hasRequiredFields)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveForFutureUse
                    .filterNot { viewModel.currentScreenState.value is BillingDetailsCollection }
                    .collect { saved ->
                        val mandateText = ACHText.getContinueMandateText(
                            context = requireContext(),
                            merchantName = viewModel.formattedMerchantName(),
                            isSaveForFutureUseSelected = saved,
                        )
                        updateMandateText(mandateText)
                    }
            }
        }

        setContent {
            StripeTheme {
                val currentScreenState by viewModel.currentScreenState.collectAsState()
                val processing by viewModel.processing.collectAsState()
                val lastTextFieldIdentifier by viewModel.lastTextFieldIdentifier.collectAsState(null)

                LaunchedEffect(currentScreenState) {
                    handleScreenStateChanged(currentScreenState)
                }

                when (val screenState = currentScreenState) {
                    is BillingDetailsCollection -> {
                        BillingDetailsCollectionScreen(
                            formArgs = formArgs,
                            processing = processing,
                            screenState = screenState,
                            nameController = viewModel.nameController,
                            emailController = viewModel.emailController,
                            phoneController = viewModel.phoneController,
                            addressController = viewModel.addressElement.controller,
                            lastTextFieldIdentifier = lastTextFieldIdentifier,
                            sameAsShippingElement = viewModel.sameAsShippingElement,
                        )
                    }
                    is USBankAccountFormScreenState.MandateCollection -> {
                        MandateCollectionScreen(
                            formArgs = formArgs,
                            processing = processing,
                            screenState = screenState,
                            nameController = viewModel.nameController,
                            emailController = viewModel.emailController,
                            phoneController = viewModel.phoneController,
                            addressController = viewModel.addressElement.controller,
                            lastTextFieldIdentifier = lastTextFieldIdentifier,
                            sameAsShippingElement = viewModel.sameAsShippingElement,
                            saveForFutureUseElement = viewModel.saveForFutureUseElement,
                            onRemoveAccount = viewModel::reset,
                        )
                    }
                    is USBankAccountFormScreenState.VerifyWithMicrodeposits -> {
                        VerifyWithMicrodepositsScreen(
                            formArgs = formArgs,
                            processing = processing,
                            screenState = screenState,
                            nameController = viewModel.nameController,
                            emailController = viewModel.emailController,
                            phoneController = viewModel.phoneController,
                            addressController = viewModel.addressElement.controller,
                            lastTextFieldIdentifier = lastTextFieldIdentifier,
                            sameAsShippingElement = viewModel.sameAsShippingElement,
                            saveForFutureUseElement = viewModel.saveForFutureUseElement,
                            onRemoveAccount = viewModel::reset,
                        )
                    }
                    is USBankAccountFormScreenState.SavedAccount -> {
                        SavedAccountScreen(
                            formArgs = formArgs,
                            processing = processing,
                            screenState = screenState,
                            nameController = viewModel.nameController,
                            emailController = viewModel.emailController,
                            phoneController = viewModel.phoneController,
                            addressController = viewModel.addressElement.controller,
                            lastTextFieldIdentifier = lastTextFieldIdentifier,
                            sameAsShippingElement = viewModel.sameAsShippingElement,
                            saveForFutureUseElement = viewModel.saveForFutureUseElement,
                            onRemoveAccount = viewModel::reset,
                        )
                    }
                }
            }
        }
    }

    private fun handleScreenStateChanged(screenState: USBankAccountFormScreenState) {
        sheetViewModel?.onError(screenState.error)

        val showProcessingWhenClicked = screenState is BillingDetailsCollection || completePayment
        val enabled = if (screenState is BillingDetailsCollection) {
            viewModel.requiredFields.value
        } else {
            true
        }

        updatePrimaryButton(
            text = screenState.primaryButtonText,
            onClick = { viewModel.handlePrimaryButtonClick(screenState) },
            enabled = enabled,
            shouldShowProcessingWhenClicked = showProcessingWhenClicked
        )

        updateMandateText(screenState.mandateText)
    }

    override fun onDetach() {
        sheetViewModel?.resetUSBankPrimaryButton()
        viewModel.onDestroy()
        super.onDetach()
    }

    private fun updatePrimaryButton(
        text: String,
        onClick: () -> Unit,
        shouldShowProcessingWhenClicked: Boolean = true,
        enabled: Boolean = true,
    ) {
        sheetViewModel?.updatePrimaryButtonState(PrimaryButton.State.Ready)
        sheetViewModel?.updateCustomPrimaryButtonUiState {
            PrimaryButton.UIState(
                label = text,
                onClick = {
                    if (shouldShowProcessingWhenClicked) {
                        sheetViewModel?.updatePrimaryButtonState(PrimaryButton.State.StartProcessing)
                    }
                    onClick()
                    sheetViewModel?.updateCustomPrimaryButtonUiState { button ->
                        button?.copy(enabled = false)
                    }
                },
                enabled = enabled,
                lockVisible = completePayment,
            )
        }
    }

    private fun updateMandateText(mandateText: String?) {
        val microdepositsText =
            if (viewModel.currentScreenState.value
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
}
