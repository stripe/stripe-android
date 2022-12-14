package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.Fragment
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkInlineSignedIn
import com.stripe.android.link.ui.inline.LinkInlineSignup
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.databinding.FragmentAchBinding
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.getPMAddForm
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.Loading
import com.stripe.android.paymentsheet.ui.PaymentMethodForm
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@FlowPreview
internal abstract class BaseAddPaymentMethodFragment : Fragment() {
    abstract val sheetViewModel: BaseSheetViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        val showCheckboxFlow = MutableStateFlow(false)

        setContent {
            PaymentsTheme {
                AddPaymentMethod(showCheckboxFlow)
            }
        }
    }

    @Composable
    internal fun AddPaymentMethod(
        showCheckboxFlow: MutableStateFlow<Boolean>
    ) {
        val isRepositoryReady by sheetViewModel.isResourceRepositoryReady.observeAsState()
        val processing by sheetViewModel.processing.observeAsState(false)

        val linkConfig by sheetViewModel.linkConfiguration.observeAsState()
        val linkAccountStatus by linkConfig?.let {
            sheetViewModel.linkLauncher.getAccountStatusFlow(it).collectAsState(null)
        } ?: mutableStateOf(null)

        if (isRepositoryReady == true) {
            var selectedPaymentMethodCode: String by rememberSaveable {
                mutableStateOf(getInitiallySelectedPaymentMethodType())
            }

            val selectedItem = remember(selectedPaymentMethodCode) {
                sheetViewModel.supportedPaymentMethods.first {
                    it.code == selectedPaymentMethodCode
                }
            }

            val showLinkInlineSignup = showLinkInlineSignupView(
                selectedPaymentMethodCode,
                linkAccountStatus
            )

            val arguments = remember(selectedItem, showLinkInlineSignup) {
                createFormArguments(selectedItem, showLinkInlineSignup)
            }

            LaunchedEffect(arguments) {
                showCheckboxFlow.emit(arguments.showCheckbox)
            }

            val paymentSelection by sheetViewModel.selection.observeAsState()
            val linkInlineSelection by sheetViewModel.linkInlineSelection.observeAsState()
            var linkSignupState by remember {
                mutableStateOf<InlineSignupViewState?>(null)
            }

            LaunchedEffect(paymentSelection, linkSignupState, linkInlineSelection) {
                val state = linkSignupState
                if (state != null) {
                    onLinkSignupStateChanged(linkConfig!!, state, paymentSelection)
                } else if (linkInlineSelection != null) {
                    (paymentSelection as? PaymentSelection.New.Card)?.let {
                        sheetViewModel.updatePrimaryButtonUIState(
                            PrimaryButton.UIState(
                                label = null,
                                onClick = {
                                    sheetViewModel.payWithLinkInline(
                                        linkConfig!!,
                                        null
                                    )
                                },
                                enabled = true,
                                visible = true
                            )
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                PaymentElement(
                    enabled = !processing,
                    supportedPaymentMethods = sheetViewModel.supportedPaymentMethods,
                    selectedItem = selectedItem,
                    showLinkInlineSignup = showLinkInlineSignup,
                    linkPaymentLauncher = sheetViewModel.linkLauncher,
                    showCheckboxFlow = showCheckboxFlow,
                    onItemSelectedListener = { selectedLpm ->
                        if (selectedItem != selectedLpm) {
                            sheetViewModel.updatePrimaryButtonUIState(null)
                            selectedPaymentMethodCode = selectedLpm.code
                        }
                    },
                    onLinkSignupStateChanged = { _, inlineSignupViewState ->
                        linkSignupState = inlineSignupViewState
                    },
                    formArguments = arguments,
                    onFormFieldValuesChanged = { formValues ->
                        sheetViewModel.updateSelection(
                            transformToPaymentSelection(
                                formValues,
                                selectedItem
                            )
                        )
                    }
                )
            }
        } else {
            Loading()
        }
    }

    @Composable
    internal fun PaymentElement(
        enabled: Boolean,
        supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>,
        selectedItem: LpmRepository.SupportedPaymentMethod,
        showLinkInlineSignup: Boolean,
        linkPaymentLauncher: LinkPaymentLauncher,
        showCheckboxFlow: Flow<Boolean>,
        onItemSelectedListener: (LpmRepository.SupportedPaymentMethod) -> Unit,
        onLinkSignupStateChanged: (LinkPaymentLauncher.Configuration, InlineSignupViewState) -> Unit,
        formArguments: FormFragmentArguments,
        onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    ) {
        val horizontalPadding = dimensionResource(
            id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            if (supportedPaymentMethods.size > 1) {
                PaymentMethodsUI(
                    selectedIndex = supportedPaymentMethods.indexOf(selectedItem),
                    isEnabled = enabled,
                    paymentMethods = supportedPaymentMethods,
                    onItemSelectedListener = onItemSelectedListener,
                    modifier = Modifier.padding(top = 26.dp, bottom = 12.dp),
                )
            }

            if (selectedItem.code == PaymentMethod.Type.USBankAccount.code) {
                (activity as BaseSheetActivity<*>).formArgs = formArguments
                Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                    AndroidViewBinding(FragmentAchBinding::inflate)
                }
            } else {
                PaymentMethodForm(
                    args = formArguments,
                    enabled = enabled,
                    onFormFieldValuesChanged = onFormFieldValuesChanged,
                    showCheckboxFlow = showCheckboxFlow,
                    injector = sheetViewModel.injector,
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                )
            }

            if (showLinkInlineSignup) {
                if (sheetViewModel.linkInlineSelection.value != null) {
                    LinkInlineSignedIn(
                        linkPaymentLauncher = linkPaymentLauncher,
                        onLogout = {
                            sheetViewModel.linkInlineSelection.value = null
                        },
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding, vertical = 6.dp)
                            .fillMaxWidth()
                    )
                } else {
                    LinkInlineSignup(
                        linkPaymentLauncher = linkPaymentLauncher,
                        enabled = enabled,
                        onStateChanged = onLinkSignupStateChanged,
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding, vertical = 6.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sheetViewModel.headerText.value =
            getString(R.string.stripe_paymentsheet_add_payment_method_title)

        sheetViewModel.eventReporter.onShowNewPaymentOptionForm(
            linkEnabled = sheetViewModel.isLinkEnabled.value ?: false,
            activeLinkSession = sheetViewModel.activeLinkSession.value ?: false
        )
    }

    private fun getInitiallySelectedPaymentMethodType() =
        when (val selection = sheetViewModel.newPaymentSelection) {
            is PaymentSelection.New.LinkInline -> PaymentMethod.Type.Card.code
            is PaymentSelection.New.Card,
            is PaymentSelection.New.USBankAccount,
            is PaymentSelection.New.GenericPaymentMethod ->
                selection.paymentMethodCreateParams.typeCode
            else -> sheetViewModel.supportedPaymentMethods.first().code
        }

    private fun showLinkInlineSignupView(
        paymentMethodCode: String,
        linkAccountStatus: AccountStatus?
    ): Boolean {
        return sheetViewModel.isLinkEnabled.value == true &&
            sheetViewModel.stripeIntent.value
            ?.linkFundingSources?.contains(PaymentMethod.Type.Card.code) == true &&
            paymentMethodCode == PaymentMethod.Type.Card.code &&
            (
                linkAccountStatus in setOf(
                    AccountStatus.NeedsVerification,
                    AccountStatus.VerificationStarted,
                    AccountStatus.SignedOut
                ) ||
                    sheetViewModel.linkInlineSelection.value != null
                )
    }

    private fun onLinkSignupStateChanged(
        config: LinkPaymentLauncher.Configuration,
        viewState: InlineSignupViewState,
        paymentSelection: PaymentSelection?
    ) {
        sheetViewModel.updatePrimaryButtonUIState(
            if (viewState.useLink) {
                val userInput = viewState.userInput
                if (userInput != null &&
                    paymentSelection != null
                ) {
                    PrimaryButton.UIState(
                        label = null,
                        onClick = {
                            sheetViewModel.payWithLinkInline(
                                config,
                                userInput
                            )
                        },
                        enabled = true,
                        visible = true
                    )
                } else {
                    PrimaryButton.UIState(
                        label = null,
                        onClick = null,
                        enabled = false,
                        visible = true
                    )
                }
            } else {
                null
            }
        )
    }

    @VisibleForTesting
    internal fun createFormArguments(
        selectedItem: LpmRepository.SupportedPaymentMethod,
        showLinkInlineSignup: Boolean
    ) = getFormArguments(
        showPaymentMethod = selectedItem,
        stripeIntent = requireNotNull(sheetViewModel.stripeIntent.value),
        config = sheetViewModel.config,
        merchantName = sheetViewModel.merchantName,
        amount = sheetViewModel.amount.value,
        newLpm = sheetViewModel.newPaymentSelection,
        isShowingLinkInlineSignup = showLinkInlineSignup
    )

    @VisibleForTesting
    internal fun transformToPaymentSelection(
        formFieldValues: FormFieldValues?,
        selectedPaymentMethodResources: LpmRepository.SupportedPaymentMethod
    ) = formFieldValues?.let {
        FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
            formFieldValues.fieldValuePairs
                .filterNot { entry ->
                    entry.key == IdentifierSpec.SaveForFutureUse ||
                        entry.key == IdentifierSpec.CardBrand
                },
            selectedPaymentMethodResources.code,
            selectedPaymentMethodResources.requiresMandate
        ).run {
            if (selectedPaymentMethodResources.code == PaymentMethod.Type.Card.code) {
                PaymentSelection.New.Card(
                    paymentMethodCreateParams = this,
                    brand = CardBrand.fromCode(
                        formFieldValues.fieldValuePairs[IdentifierSpec.CardBrand]?.value
                    ),
                    customerRequestedSave = formFieldValues.userRequestedReuse

                )
            } else {
                PaymentSelection.New.GenericPaymentMethod(
                    getString(selectedPaymentMethodResources.displayNameResource),
                    selectedPaymentMethodResources.iconResource,
                    this,
                    customerRequestedSave = formFieldValues.userRequestedReuse
                )
            }
        }
    }

    companion object {

        @VisibleForTesting
        fun getFormArguments(
            showPaymentMethod: LpmRepository.SupportedPaymentMethod,
            stripeIntent: StripeIntent,
            config: PaymentSheet.Configuration?,
            merchantName: String,
            amount: Amount? = null,
            newLpm: PaymentSelection.New?,
            isShowingLinkInlineSignup: Boolean = false
        ): FormFragmentArguments {
            val layoutFormDescriptor = showPaymentMethod.getPMAddForm(stripeIntent, config)

            return FormFragmentArguments(
                paymentMethodCode = showPaymentMethod.code,
                showCheckbox = layoutFormDescriptor.showCheckbox && !isShowingLinkInlineSignup,
                showCheckboxControlledFields = newLpm?.let {
                    newLpm.customerRequestedSave ==
                        PaymentSelection.CustomerRequestedSave.RequestReuse
                } ?: layoutFormDescriptor.showCheckboxControlledFields,
                merchantName = merchantName,
                amount = amount,
                billingDetails = config?.defaultBillingDetails,
                shippingDetails = config?.shippingDetails,
                initialPaymentMethodCreateParams =
                if (newLpm is PaymentSelection.New.LinkInline) {
                    newLpm.linkPaymentDetails.originalParams
                } else {
                    newLpm?.paymentMethodCreateParams?.typeCode?.takeIf {
                        it == showPaymentMethod.code
                    }?.let {
                        when (newLpm) {
                            is PaymentSelection.New.GenericPaymentMethod ->
                                newLpm.paymentMethodCreateParams
                            is PaymentSelection.New.Card ->
                                newLpm.paymentMethodCreateParams
                            else -> null
                        }
                    }
                }
            )
        }
    }
}
