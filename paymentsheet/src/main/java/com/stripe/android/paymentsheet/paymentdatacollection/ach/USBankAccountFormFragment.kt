package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
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

    private val paymentSheetViewModelFactory: ViewModelProvider.Factory =
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

    private val paymentOptionsViewModelFactory: ViewModelProvider.Factory =
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
                    clientSecret
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
                                R.string.us_bank_account_payment_sheet_mandate_save,
                                viewModel.formattedMerchantName()
                            )
                        } else {
                            getString(R.string.us_bank_account_payment_sheet_mandate_continue)
                        }
                    )
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentScreenState.collect { screenState ->
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
                        is USBankAccountFormScreenState.ConfirmIntent -> {
                            (sheetViewModel as? PaymentSheetViewModel)
                                ?.confirmStripeIntent(screenState.confirmIntentParams)
                        }
                        is USBankAccountFormScreenState.Finished -> {
                            sheetViewModel?.updateSelection(
                                PaymentSelection.New.GenericPaymentMethod(
                                    labelResource = getString(
                                        R.string.paymentsheet_payment_method_item_card_number,
                                        screenState.last4
                                    ),
                                    iconResource = TransformToBankIcon(
                                        screenState.bankName
                                    ),
                                    paymentMethodCreateParams =
                                    PaymentMethodCreateParams.create(
                                        usBankAccount = PaymentMethodCreateParams.USBankAccount(
                                            linkAccountSessionId = screenState.linkAccountId
                                        ),
                                        billingDetails = PaymentMethod.BillingDetails(
                                            name = viewModel.name.value,
                                            email = viewModel.email.value
                                        )
                                    ),
                                    customerRequestedSave = if (formArgs.showCheckbox) {
                                        if (viewModel.saveForFutureUse.value) {
                                            PaymentSelection.CustomerRequestedSave.RequestReuse
                                        } else {
                                            PaymentSelection.CustomerRequestedSave.RequestNoReuse
                                        }
                                    } else {
                                        PaymentSelection.CustomerRequestedSave.NoRequest
                                    }
                                )
                            )
                            sheetViewModel?.onFinish()
                        }
                    }
                }
            }
        }
    }

    override fun onDetach() {
        viewModel.onDestroy()
        super.onDetach()
    }

    private suspend fun ComposeView.renderNameAndEmailCollectionScreen(
        screenState: USBankAccountFormScreenState.NameAndEmailCollection,
        coroutineScope: CoroutineScope
    ) {
        setContent {
            PaymentsTheme {
                NameAndEmailCollectionScreen(screenState.error)
            }
        }
        updatePrimaryButton(
            text = screenState.primaryButtonText,
            onClick = screenState.primaryButtonOnClick,
            enabled = viewModel.requiredFields.stateIn(coroutineScope).value,
        )
        updateMandateText(null)
    }

    private fun ComposeView.renderMandateCollectionScreen(
        screenState: USBankAccountFormScreenState.MandateCollection
    ) {
        setContent {
            PaymentsTheme {
                MandateCollectionScreen(
                    screenState.bankName,
                    screenState.displayName,
                    screenState.last4
                )
            }
        }
        updatePrimaryButton(
            text = screenState.primaryButtonText,
            onClick = screenState.primaryButtonOnClick,
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
                VerifyWithMicrodepositsScreen(
                    screenState.bankName,
                    screenState.displayName,
                    screenState.last4
                )
            }
        }
        updatePrimaryButton(
            text = screenState.primaryButtonText,
            onClick = screenState.primaryButtonOnClick,
            shouldProcess = isPaymentSheet
        )
        updateMandateText(
            screenState.mandateText
        )
    }

    @Composable
    private fun NameAndEmailCollectionScreen(@StringRes error: Int? = null) {
        Column(Modifier.fillMaxWidth()) {
            NameAndEmailForm()
            error?.let {
                sheetViewModel?.onError(error)
            }
        }
    }

    @Composable
    private fun MandateCollectionScreen(
        bankName: String?,
        displayName: String?,
        last4: String?
    ) {
        Column(Modifier.fillMaxWidth()) {
            NameAndEmailForm()
            AccountDetailsForm(bankName, displayName, last4)
        }
    }

    @Composable
    private fun VerifyWithMicrodepositsScreen(
        bankName: String?,
        displayName: String?,
        last4: String?
    ) {
        Column(Modifier.fillMaxWidth()) {
            NameAndEmailForm()
            AccountDetailsForm(bankName, displayName, last4)
        }
    }

    @Composable
    private fun NameAndEmailForm() {
        Column(Modifier.fillMaxWidth()) {
            H6Text(
                text = stringResource(R.string.us_bank_account_payment_sheet_title),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                SectionElementUI(
                    enabled = true,
                    element = SectionElement(
                        identifier = IdentifierSpec.Name,
                        fields = listOf(viewModel.nameElement),
                        controller = SectionController(
                            null,
                            listOf(viewModel.nameElement.sectionFieldErrorController())
                        )
                    ),
                    emptyList(),
                    viewModel.nameElement.identifier
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                SectionElementUI(
                    enabled = true,
                    element = SectionElement(
                        identifier = IdentifierSpec.Email,
                        fields = listOf(viewModel.emailElement),
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
        displayName: String?,
        last4: String?
    ) {
        val openDialog = remember { mutableStateOf(false) }
        val bankIcon = TransformToBankIcon(bankName)

        Column(
            Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            H6Text(
                text = stringResource(R.string.us_bank_account_payment_sheet_bank_account),
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
                        Text(text = "$displayName ••••$last4")
                    }
                    Image(
                        painter = painterResource(R.drawable.stripe_ic_clear),
                        contentDescription = null,
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp)
                            .clickable {
                                openDialog.value = true
                            }
                    )
                }
            }
            if (formArgs.showCheckbox) {
                SaveForFutureUseElementUI(true, viewModel.saveForFutureUseElement)
            }
        }
        last4?.let {
            SimpleDialogElementUI(
                openDialog = openDialog,
                titleText = stringResource(
                    id = R.string.us_bank_account_payment_sheet_alert_title
                ),
                messageText = stringResource(
                    id = R.string.us_bank_account_payment_sheet_alert_text,
                    last4
                ),
                confirmText = stringResource(
                    id = R.string.us_bank_account_payment_sheet_alert_remove
                ),
                dismissText = stringResource(
                    id = R.string.us_bank_account_payment_sheet_alert_cancel
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
                    R.string.us_bank_account_payment_sheet_mandate_verify_with_microdeposit,
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
