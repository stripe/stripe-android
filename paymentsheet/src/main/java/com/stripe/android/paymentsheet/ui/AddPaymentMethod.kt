package com.stripe.android.paymentsheet.ui

import android.content.Context
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
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
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
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.LocalAutofillEventReporter
import com.stripe.android.uicore.elements.ParameterDestination

@Composable
internal fun AddPaymentMethod(
    sheetViewModel: BaseSheetViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedPaymentMethodCode: String by rememberSaveable {
        mutableStateOf(sheetViewModel.initiallySelectedPaymentMethodType)
    }
    val supportedPaymentMethods: List<SupportedPaymentMethod> = sheetViewModel.supportedPaymentMethods
    val selectedItem = remember(selectedPaymentMethodCode) {
        sheetViewModel.supportedPaymentMethodForCode(selectedPaymentMethodCode)
    }
    val arguments = remember(selectedItem) {
        sheetViewModel.createFormArguments(selectedItem)
    }

    val paymentSelection by sheetViewModel.selection.collectAsState()

    val linkSignupMode by sheetViewModel.linkSignupMode.collectAsState()
    val linkInlineSignupMode = remember(linkSignupMode, selectedPaymentMethodCode) {
        linkSignupMode.takeIf { selectedPaymentMethodCode == PaymentMethod.Type.Card.code }
    }
    val linkInlineSelection by sheetViewModel.linkHandler.linkInlineSelection.collectAsState()
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

        sheetViewModel.clearErrorMessages()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        CompositionLocalProvider(
            LocalAutofillEventReporter provides sheetViewModel::reportAutofillEvent,
            LocalCardNumberCompletedEventReporter provides sheetViewModel::reportCardNumberCompleted,
        ) {
            val initializationMode = (sheetViewModel as? PaymentSheetViewModel)
                ?.args
                ?.initializationMode
            val onBehalfOf = (initializationMode as? PaymentSheet.InitializationMode.DeferredIntent)
                ?.intentConfiguration
                ?.onBehalfOf
            val processing by sheetViewModel.processing.collectAsState()
            val context = LocalContext.current
            val paymentMethodMetadata by sheetViewModel.paymentMethodMetadata.collectAsState()
            val stripeIntent = paymentMethodMetadata?.stripeIntent
            val formElements = remember(selectedItem.code) {
                sheetViewModel.formElementsForCode(selectedItem.code)
            }

            val isSaveForFutureUseValueChangeable = paymentMethodMetadata?.let {
                isSaveForFutureUseValueChangeable(
                    code = arguments.paymentMethodCode,
                    metadata = it,
                )
            } ?: false

            val instantDebits = remember(supportedPaymentMethods) {
                supportedPaymentMethods.find { it.code == PaymentMethod.Type.Link.code } != null
            }

            PaymentElement(
                enabled = !processing,
                supportedPaymentMethods = supportedPaymentMethods,
                selectedItem = selectedItem,
                formElements = formElements,
                linkSignupMode = linkInlineSignupMode,
                linkConfigurationCoordinator = sheetViewModel.linkConfigurationCoordinator,
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
                    showCheckbox = isSaveForFutureUseValueChangeable,
                    instantDebits = instantDebits,
                    onBehalfOf = onBehalfOf,
                    isCompleteFlow = sheetViewModel is PaymentSheetViewModel,
                    isPaymentFlow = stripeIntent is PaymentIntent,
                    stripeIntentId = stripeIntent?.id,
                    clientSecret = stripeIntent?.clientSecret,
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
                    paymentMethodMetadata?.let { paymentMethodMetadata ->
                        val newSelection = formValues?.transformToPaymentSelection(
                            context = context,
                            paymentMethod = selectedItem,
                            paymentMethodMetadata = paymentMethodMetadata,
                        )
                        sheetViewModel.updateSelection(newSelection)
                    }
                },
                onInteractionEvent = {
                    sheetViewModel.reportFieldInteraction(selectedPaymentMethodCode)
                }
            )
        }
    }
}

internal fun FormFieldValues.transformToPaymentMethodCreateParams(
    paymentMethod: SupportedPaymentMethod,
    paymentMethodMetadata: PaymentMethodMetadata,
): PaymentMethodCreateParams {
    return FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
        fieldValuePairs = fieldValuePairs.filter { entry ->
            entry.key.destination == ParameterDestination.Api.Params
        }.filterNot { entry ->
            entry.key == IdentifierSpec.SaveForFutureUse || entry.key == IdentifierSpec.CardBrand
        },
        code = paymentMethod.code,
        requiresMandate = paymentMethodMetadata.requiresMandate(paymentMethod.code),
    )
}

internal fun FormFieldValues.transformToPaymentMethodOptionsParams(
    paymentMethod: SupportedPaymentMethod
): PaymentMethodOptionsParams? {
    return FieldValuesToParamsMapConverter.transformToPaymentMethodOptionsParams(
        fieldValuePairs = fieldValuePairs.filter { entry ->
            entry.key.destination == ParameterDestination.Api.Options
        },
        code = paymentMethod.code,
    )
}

internal fun FormFieldValues.transformToExtraParams(
    paymentMethod: SupportedPaymentMethod
): PaymentMethodExtraParams? {
    return FieldValuesToParamsMapConverter.transformToPaymentMethodExtraParams(
        fieldValuePairs = fieldValuePairs.filter { entry ->
            entry.key.destination == ParameterDestination.Local.Extras
        },
        code = paymentMethod.code,
    )
}

internal fun FormFieldValues.transformToPaymentSelection(
    context: Context,
    paymentMethod: SupportedPaymentMethod,
    paymentMethodMetadata: PaymentMethodMetadata,
): PaymentSelection {
    val params = transformToPaymentMethodCreateParams(paymentMethod, paymentMethodMetadata)
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
    } else if (paymentMethodMetadata.isExternalPaymentMethod(paymentMethod.code)) {
        PaymentSelection.ExternalPaymentMethod(
            type = paymentMethod.code,
            label = paymentMethod.displayName.resolve(context),
            iconResource = paymentMethod.iconResource,
            lightThemeIconUrl = paymentMethod.lightThemeIconUrl,
            darkThemeIconUrl = paymentMethod.darkThemeIconUrl,
        )
    } else {
        PaymentSelection.New.GenericPaymentMethod(
            labelResource = paymentMethod.displayName.resolve(context),
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
