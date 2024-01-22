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
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.LocalAutofillEventReporter
import com.stripe.android.uicore.elements.ParameterDestination
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun AddPaymentMethod(
    sheetViewModel: BaseSheetViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val linkHandler = sheetViewModel.linkHandler
    val showCheckboxFlow = remember { MutableStateFlow(false) }

    val processing by sheetViewModel.processing.collectAsState(false)
    val linkSignupMode by sheetViewModel.linkSignupMode.collectAsState()

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

    val linkInlineSignupMode = remember(linkSignupMode, selectedPaymentMethodCode) {
        linkSignupMode.takeIf { selectedPaymentMethodCode == PaymentMethod.Type.Card.code }
    }

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
                formViewModelSubComponentBuilderProvider = sheetViewModel.formViewModelSubComponentBuilderProvider,
                enabled = !processing,
                supportedPaymentMethods = sheetViewModel.supportedPaymentMethods,
                selectedItem = selectedItem,
                linkSignupMode = linkInlineSignupMode,
                linkConfigurationCoordinator = sheetViewModel.linkConfigurationCoordinator,
                showCheckboxFlow = showCheckboxFlow,
                onItemSelectedListener = { selectedLpm ->
                    if (selectedItem != selectedLpm) {
                        selectedPaymentMethodCode = selectedLpm.code
                        sheetViewModel.reportPaymentMethodTypeSelected(selectedLpm.code)
                    }
                },
                onLinkSignupStateChanged = { _, inlineSignupViewState ->
                    linkSignupState = inlineSignupViewState
                },
                formArguments = arguments,
                usBankAccountFormArguments = USBankAccountFormArguments(
                    onBehalfOf = onBehalfOf,
                    isCompleteFlow = sheetViewModel is PaymentSheetViewModel,
                    isPaymentFlow = stripeIntent.value is PaymentIntent,
                    stripeIntentId = stripeIntent.value?.id,
                    clientSecret = stripeIntent.value?.clientSecret,
                    shippingDetails = sheetViewModel.config.shippingDetails,
                    draftPaymentSelection = sheetViewModel.newPaymentSelection,
                    onMandateTextChanged = sheetViewModel::updateMandateText,
                    onConfirmUSBankAccount = sheetViewModel::handleConfirmUSBankAccount,
                    onCollectBankAccountResult = null,
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

internal fun FormFieldValues.transformToPaymentMethodCreateParams(
    paymentMethod: LpmRepository.SupportedPaymentMethod
): PaymentMethodCreateParams {
    return FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
        fieldValuePairs = fieldValuePairs.filter { entry ->
            entry.key.destination == ParameterDestination.Api.Params
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
            entry.key.destination == ParameterDestination.Api.Options
        },
        code = paymentMethod.code,
    )
}

internal fun FormFieldValues.transformToExtraParams(
    paymentMethod: LpmRepository.SupportedPaymentMethod
): PaymentMethodExtraParams? {
    return FieldValuesToParamsMapConverter.transformToPaymentMethodExtraParams(
        fieldValuePairs = fieldValuePairs.filter { entry ->
            entry.key.destination == ParameterDestination.Local.Extras
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
    val extras = transformToExtraParams(paymentMethod)
    return if (paymentMethod.code == PaymentMethod.Type.Card.code) {
        PaymentSelection.New.Card(
            paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = userRequestedReuse.setupFutureUsage
            ),
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
            paymentMethodExtraParams = extras,
            customerRequestedSave = userRequestedReuse,
        )
    }
}
