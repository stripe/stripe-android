package com.stripe.android.paymentsheet.ui

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.elements.ApiParameterDestination
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.LocalAutofillEventReporter
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryMethod
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate

@Composable
internal fun AddPaymentMethod(
    sheetViewModel: BaseSheetViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val linkHandler = sheetViewModel.linkHandler
    val showCheckboxFlow = remember { MutableStateFlow(false) }

    val processing by sheetViewModel.processing.collectAsState(false)
    val linkAccountStatus by linkHandler.accountStatus.collectAsState(initial = null)

    var selectedPaymentMethodCode: String by rememberSaveable {
        mutableStateOf(sheetViewModel.initiallySelectedPaymentMethodType)
    }

    val selectedItem = remember(selectedPaymentMethodCode) {
        sheetViewModel.supportedPaymentMethods.first {
            it.code == selectedPaymentMethodCode
        }
    }

    val arguments = remember(selectedItem) {
        sheetViewModel.createFormArguments(selectedItem)
    }

    val showLinkInlineSignup = sheetViewModel.showLinkInlineSignupView(
        selectedPaymentMethodCode,
        linkAccountStatus,
        arguments.showCheckbox,
    )

    LaunchedEffect(arguments) {
        showCheckboxFlow.emit(arguments.showCheckbox)
    }

    val stripeIntent = sheetViewModel.stripeIntent.collectAsState()
    val paymentSelection by sheetViewModel.selection.collectAsState()
    val linkInlineSelection by linkHandler.linkInlineSelection.collectAsState()
    var linkSignupState by remember {
        mutableStateOf<InlineSignupViewState?>(null)
    }

    LaunchedEffect(paymentSelection, linkSignupState, linkInlineSelection) {
        val state = linkSignupState
        val isUsingLinkInline = linkInlineSelection != null &&
            paymentSelection is PaymentSelection.New.Card

        if (state != null) {
            sheetViewModel.updatePrimaryButtonForLinkSignup(state)
        } else if (isUsingLinkInline) {
            sheetViewModel.updatePrimaryButtonForLinkInline()
        }
    }

//    LaunchedEffect(key1 = sheetViewModel.connectionToken) {
    LaunchedEffect(key1 = "Fwe") {
        println("asdf launched reader discovery")
        Terminal.getInstance().discoverReaders(
            DiscoveryConfiguration(
                discoveryMethod = DiscoveryMethod.INTERNET,
            ),
            object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    sheetViewModel.readers.getAndUpdate { current ->
                        val online = readers.filterNot { it.networkStatus == Reader.NetworkStatus.OFFLINE }
                        val new = online.filter { it.serialNumber !in current.map { it.serialNumber } }

                        // Remove readers no longer in list and append new readers at the
                        // *end* of the list in order to minimize order shuffling.
                        current.filter { it.serialNumber in online.map { it.serialNumber } }.plus(new)
                    }
//                    sheetViewModel.readers.value = readers
                    println("asdf readers ${sheetViewModel.readers.value}")
                }

            },
            object : com.stripe.stripeterminal.external.callable.Callback {
                override fun onFailure(e: TerminalException) {
                    e.printStackTrace()
                }

                override fun onSuccess() {
                    println("Finished discovering readers")
                }

            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        CompositionLocalProvider(
            LocalAutofillEventReporter provides sheetViewModel::reportAutofillEvent
        ) {
            val initializationMode = (sheetViewModel as? PaymentSheetViewModel)
                ?.args
                ?.initializationMode
            val onBehalfOf = (initializationMode as? PaymentSheet.InitializationMode.DeferredIntent)
                ?.intentConfiguration
                ?.onBehalfOf
            PaymentElement(
                sheetViewModel = sheetViewModel,
                enabled = !processing,
                supportedPaymentMethods = sheetViewModel.supportedPaymentMethods,
                selectedItem = selectedItem,
                showLinkInlineSignup = showLinkInlineSignup,
                linkConfigurationCoordinator = sheetViewModel.linkConfigurationCoordinator,
                showCheckboxFlow = showCheckboxFlow,
                onItemSelectedListener = { selectedLpm ->
//                    if (selectedItem != selectedLpm) {
//                        selectedPaymentMethodCode = selectedLpm.code
//                        sheetViewModel.reportPaymentMethodTypeSelected(selectedLpm.code)
//                    }
                },
                onLinkSignupStateChanged = { _, inlineSignupViewState ->
                    linkSignupState = inlineSignupViewState
                },
                formArguments = arguments,
                usBankAccountFormArguments = USBankAccountFormArguments(
                    onBehalfOf = onBehalfOf,
                    isCompleteFlow = sheetViewModel is PaymentSheetViewModel,
                    isPaymentFlow = initializationMode is PaymentSheet.InitializationMode.PaymentIntent,
                    stripeIntentId = stripeIntent.value?.id,
                    clientSecret = stripeIntent.value?.clientSecret,
                    shippingDetails = sheetViewModel.config?.shippingDetails,
                    draftPaymentSelection = sheetViewModel.newPaymentSelection,
                    onMandateTextChanged = sheetViewModel::updateBelowButtonText,
                    onHandleUSBankAccount = sheetViewModel::handleConfirmUSBankAccount,
                    onUpdatePrimaryButtonUIState = sheetViewModel::updateCustomPrimaryButtonUiState,
                    onUpdatePrimaryButtonState = sheetViewModel::updatePrimaryButtonState,
                    onError = sheetViewModel::onError
                ),
                onFormFieldValuesChanged = { formValues ->
                    val newSelection = formValues?.transformToPaymentSelection(
                        resources = context.resources,
                        paymentMethod = selectedItem,
                    )
                    sheetViewModel.updateSelection(newSelection)
                }
            )
        }
    }
}

private val BaseSheetViewModel.initiallySelectedPaymentMethodType: PaymentMethodCode
    get() = when (val selection = newPaymentSelection) {
        is PaymentSelection.New.LinkInline -> PaymentMethod.Type.Card.code
        is PaymentSelection.New.Card,
        is PaymentSelection.New.USBankAccount,
        is PaymentSelection.New.GenericPaymentMethod -> selection.paymentMethodCreateParams.typeCode
        else -> supportedPaymentMethods.first().code
    }

private fun BaseSheetViewModel.showLinkInlineSignupView(
    paymentMethodCode: String,
    linkAccountStatus: AccountStatus?,
    showSaveToCustomerCheckbox: Boolean,
): Boolean {
    val validStatusStates = setOf(
        AccountStatus.Verified,
        AccountStatus.SignedOut,
    )
    val linkInlineSelectionValid = linkHandler.linkInlineSelection.value != null
    val ableToShowLink = linkHandler.isLinkEnabled.value == true && stripeIntent.value
        ?.linkFundingSources?.contains(PaymentMethod.Type.Card.code) == true &&
        paymentMethodCode == PaymentMethod.Type.Card.code &&
        (linkAccountStatus in validStatusStates || linkInlineSelectionValid)
    return !showSaveToCustomerCheckbox && ableToShowLink
}

internal fun FormFieldValues.transformToPaymentMethodCreateParams(
    paymentMethod: LpmRepository.SupportedPaymentMethod
): PaymentMethodCreateParams {
    return FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
        fieldValuePairs = fieldValuePairs.filter { entry ->
            entry.key.apiParameterDestination == ApiParameterDestination.Params
        }.filterNot { entry ->
            entry.key == IdentifierSpec.SaveForFutureUse || entry.key == IdentifierSpec.CardBrand
        },
        code = paymentMethod.code,
        requiresMandate = paymentMethod.requiresMandate,
    )
}

internal fun FormFieldValues.transformToPaymentMethodOptionsParams(
    paymentMethod: LpmRepository.SupportedPaymentMethod
): PaymentMethodOptionsParams? {
    return FieldValuesToParamsMapConverter.transformToPaymentMethodOptionsParams(
        fieldValuePairs = fieldValuePairs.filter { entry ->
            entry.key.apiParameterDestination == ApiParameterDestination.Options
        },
        code = paymentMethod.code,
    )
}

internal fun FormFieldValues.transformToPaymentSelection(
    resources: Resources,
    paymentMethod: LpmRepository.SupportedPaymentMethod
): PaymentSelection.New {
    val params = transformToPaymentMethodCreateParams(paymentMethod)
    val options = transformToPaymentMethodOptionsParams(paymentMethod)
    return if (paymentMethod.code == PaymentMethod.Type.Card.code) {
        PaymentSelection.New.Card(
            paymentMethodOptionsParams = options,
            paymentMethodCreateParams = params,
            brand = CardBrand.fromCode(fieldValuePairs[IdentifierSpec.CardBrand]?.value),
            customerRequestedSave = userRequestedReuse,
        )
    } else {
        PaymentSelection.New.GenericPaymentMethod(
            labelResource = resources.getString(paymentMethod.displayNameResource),
            iconResource = paymentMethod.iconResource,
            lightThemeIconUrl = paymentMethod.lightThemeIconUrl,
            darkThemeIconUrl = paymentMethod.darkThemeIconUrl,
            paymentMethodCreateParams = params,
            paymentMethodOptionsParams = options,
            customerRequestedSave = userRequestedReuse,
        )
    }
}
