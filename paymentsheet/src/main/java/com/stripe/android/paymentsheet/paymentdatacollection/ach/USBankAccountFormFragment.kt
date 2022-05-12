package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
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
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.H6Text
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseElementUI
import com.stripe.android.ui.core.elements.SectionCard
import com.stripe.android.ui.core.elements.SectionController
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.elements.SectionElementUI
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Fragment that displays a form for us_bank_account payment data collection
 */
internal class USBankAccountFormFragment : Fragment() {

    private val formArgs by lazy {
        requireNotNull(
            requireArguments().getParcelable<FormFragmentArguments>(
                ComposeFormDataCollectionFragment.EXTRA_CONFIG
            )
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

    private val isPaymentSheet by lazy {
        sheetViewModel is PaymentSheetViewModel
    }

    private val clientSecret by lazy {
        when (val intent = sheetViewModel?.stripeIntent?.value) {
            is PaymentIntent -> PaymentIntentClientSecret(intent.clientSecret!!)
            is SetupIntent -> SetupIntentClientSecret(intent.clientSecret!!)
            else -> null
        }
    }

    private val viewModel by viewModels<USBankAccountFormViewModel> {
        USBankAccountFormViewModel.Factory(
            { requireActivity().application },
            {
                USBankAccountFormViewModel.Args(
                    formArgs,
                    sheetViewModel is PaymentSheetViewModel,
                    clientSecret,
                    sheetViewModel?.usBankAccountSavedScreenState,
                    (sheetViewModel?.newLpm as? PaymentSelection.New.USBankAccount)
                )
            },
            this
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
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentScreenState.collect { screenState ->
                    sheetViewModel?.onError(screenState.error)
                    when (screenState) {
                        is USBankAccountFormScreenState.NameAndEmailCollection -> {
                            renderNameAndEmailCollectionScreen(screenState, this)
                        }
                        is USBankAccountFormScreenState.MandateCollection -> {
                            renderMandateCollectionScreen(screenState)
                        }
                        is USBankAccountFormScreenState.VerifyWithMicrodeposits -> {
                            renderVerifyWithMicrodepositsScreen(screenState)
                        }
                        is USBankAccountFormScreenState.SavedAccount -> {
                            renderSavedAccountScreen(screenState)
                        }
                        is USBankAccountFormScreenState.ConfirmIntent -> {
                            (sheetViewModel as? PaymentSheetViewModel)
                                ?.confirmStripeIntent(screenState.confirmIntentParams)
                        }
                        is USBankAccountFormScreenState.Finished -> {
                            sheetViewModel?.updateSelection(screenState.paymentSelection)
                            sheetViewModel?.usBankAccountSavedScreenState =
                                viewModel.generateSavedState(screenState)
                            sheetViewModel?.onFinish()
                        }
                    }
                }
            }
        }
    }

    override fun onDetach() {
        sheetViewModel?.usBankAccountSavedScreenState =
            viewModel.currentScreenState.value.updateInputs(
                viewModel.name.value,
                viewModel.email.value,
                viewModel.saveForFutureUse.value
            )
        viewModel.onDestroy()
        super.onDetach()
    }

    private suspend fun ComposeView.renderNameAndEmailCollectionScreen(
        screenState: USBankAccountFormScreenState.NameAndEmailCollection,
        coroutineScope: CoroutineScope
    ) {
        setContent {
            PaymentsTheme {
                NameAndEmailCollectionScreen(screenState)
            }
        }
        updatePrimaryButton(
            text = screenState.primaryButtonText,
            onClick = {
                viewModel.handlePrimaryButtonClick(screenState)
            },
            enabled = viewModel.requiredFields.stateIn(coroutineScope).value,
        )
        updateMandateText(null)
    }

    private fun ComposeView.renderMandateCollectionScreen(
        screenState: USBankAccountFormScreenState.MandateCollection
    ) {
        setContent {
            PaymentsTheme {
                MandateCollectionScreen(screenState)
            }
        }
        updatePrimaryButton(
            text = screenState.primaryButtonText,
            onClick = {
                viewModel.handlePrimaryButtonClick(screenState)
            },
            shouldProcess = isPaymentSheet
        )
        updateMandateText(
            screenState.mandateText
        )
    }

    private fun ComposeView.renderVerifyWithMicrodepositsScreen(
        screenState: USBankAccountFormScreenState.VerifyWithMicrodeposits
    ) {
        setContent {
            PaymentsTheme {
                VerifyWithMicrodepositsScreen(screenState)
            }
        }
        updatePrimaryButton(
            text = screenState.primaryButtonText,
            onClick = {
                viewModel.handlePrimaryButtonClick(screenState)
            },
            shouldProcess = isPaymentSheet
        )
        updateMandateText(
            screenState.mandateText
        )
    }

    private fun ComposeView.renderSavedAccountScreen(
        screenState: USBankAccountFormScreenState.SavedAccount
    ) {
        setContent {
            PaymentsTheme {
                SavedAccountScreen(screenState)
            }
        }
        updatePrimaryButton(
            text = screenState.primaryButtonText,
            onClick = {
                viewModel.handlePrimaryButtonClick(screenState)
            },
            shouldProcess = isPaymentSheet
        )
        updateMandateText(
            screenState.mandateText
        )
    }

    @Composable
    private fun NameAndEmailCollectionScreen(
        screenState: USBankAccountFormScreenState.NameAndEmailCollection
    ) {
        Column(Modifier.fillMaxWidth()) {
            NameAndEmailForm(screenState.name, screenState.email)
        }
    }

    @Composable
    private fun MandateCollectionScreen(
        screenState: USBankAccountFormScreenState.MandateCollection
    ) {
        Column(Modifier.fillMaxWidth()) {
            NameAndEmailForm(screenState.name, screenState.email)
            AccountDetailsForm(
                screenState.paymentAccount.institutionName,
                screenState.paymentAccount.last4,
                screenState.saveForFutureUsage
            )
        }
    }

    @Composable
    private fun VerifyWithMicrodepositsScreen(
        screenState: USBankAccountFormScreenState.VerifyWithMicrodeposits
    ) {
        Column(Modifier.fillMaxWidth()) {
            NameAndEmailForm(screenState.name, screenState.email)
            AccountDetailsForm(
                screenState.paymentAccount.bankName,
                screenState.paymentAccount.last4,
                screenState.saveForFutureUsage
            )
        }
    }

    @Composable
    private fun SavedAccountScreen(
        screenState: USBankAccountFormScreenState.SavedAccount
    ) {
        Column(Modifier.fillMaxWidth()) {
            NameAndEmailForm(screenState.name, screenState.email)
            AccountDetailsForm(
                screenState.bankName,
                screenState.last4,
                screenState.saveForFutureUsage
            )
        }
    }

    @Composable
    private fun NameAndEmailForm(
        name: String,
        email: String?
    ) {
        val processing = viewModel.processing.collectAsState(false)
        Column(Modifier.fillMaxWidth()) {
            H6Text(
                text = stringResource(R.string.stripe_paymentsheet_pay_with_bank_title),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                SectionElementUI(
                    enabled = !processing.value,
                    element = SectionElement(
                        identifier = IdentifierSpec.Name,
                        fields = listOf(
                            viewModel.nameElement.apply {
                                setRawValue(
                                    mapOf(IdentifierSpec.Name to name)
                                )
                            }
                        ),
                        controller = SectionController(
                            null,
                            listOf(viewModel.nameElement.sectionFieldErrorController())
                        ),
                    ),
                    emptyList(),
                    null
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                SectionElementUI(
                    enabled = !processing.value,
                    element = SectionElement(
                        identifier = IdentifierSpec.Email,
                        fields = listOf(
                            viewModel.emailElement.apply {
                                setRawValue(
                                    mapOf(IdentifierSpec.Email to email)
                                )
                            }
                        ),
                        controller = SectionController(
                            null,
                            listOf(viewModel.emailElement.sectionFieldErrorController())
                        )
                    ),
                    emptyList(),
                    viewModel.emailElement.identifier
                )
            }
        }
    }

    @Composable
    private fun AccountDetailsForm(
        bankName: String?,
        last4: String?,
        saveForFutureUsage: Boolean
    ) {
        val openDialog = remember { mutableStateOf(false) }
        val bankIcon = TransformToBankIcon(bankName)
        val processing = viewModel.processing.collectAsState(false)

        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            H6Text(
                text = stringResource(R.string.title_bank_account),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(bankIcon),
                            contentDescription = null,
                            modifier = Modifier
                                .height(40.dp)
                                .width(56.dp)
                        )
                        Text(
                            text = "$bankName ••••$last4",
                            modifier = Modifier.alpha(if (processing.value) 0.5f else 1f),
                            color = PaymentsTheme.colors.onComponent
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.stripe_ic_clear),
                        contentDescription = null,
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp)
                            .alpha(if (processing.value) 0.5f else 1f)
                            .clickable {
                                if (!processing.value) {
                                    openDialog.value = true
                                }
                            }
                    )
                }
            }
            if (formArgs.showCheckbox) {
                SaveForFutureUseElementUI(
                    true,
                    viewModel.saveForFutureUseElement.apply {
                        this.controller.onValueChange(saveForFutureUsage)
                    }
                )
            }
        }
        last4?.let {
            SimpleDialogElementUI(
                openDialog = openDialog,
                titleText = stringResource(
                    id = R.string.stripe_paymentsheet_remove_bank_account_title
                ),
                messageText = stringResource(
                    id = R.string.bank_account_ending_in,
                    last4
                ),
                confirmText = stringResource(
                    id = R.string.remove
                ),
                dismissText = stringResource(
                    id = R.string.cancel
                ),
                onConfirmListener = {
                    openDialog.value = false
                    viewModel.reset()
                },
                onDismissListener = {
                    openDialog.value = false
                }
            )
        }
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
            if (viewModel.currentScreenState.value
                is USBankAccountFormScreenState.VerifyWithMicrodeposits
            ) {
                getString(
                    R.string.stripe_paymentsheet_microdeposit,
                    viewModel.formattedMerchantName()
                )
            } else ""
        val updatedText = mandateText?.let {
            """
                $microdepositsText
                
                $mandateText
            """.trimIndent()
        } ?: run { null }
        sheetViewModel?.updateBelowButtonText(updatedText)
    }
}
