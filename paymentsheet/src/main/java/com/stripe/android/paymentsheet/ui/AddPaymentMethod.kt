package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun AddPaymentMethod(
    interactor: AddPaymentMethodInteractor,
    modifier: Modifier = Modifier,
) {
    val state by interactor.state.collectAsState()

    var selectedPaymentMethodCode: String by rememberSaveable {
        mutableStateOf(state.selectedPaymentMethodCode)
    }
    val supportedPaymentMethods: List<SupportedPaymentMethod> = state.supportedPaymentMethods
//    val arguments = remember(selectedPaymentMethodCode) {
//        interactor.createFormArguments(selectedPaymentMethodCode)
//    }

    val linkInlineSignupMode = remember(state.linkSignupMode, selectedPaymentMethodCode) {
        state.linkSignupMode.takeIf { selectedPaymentMethodCode == PaymentMethod.Type.Card.code }
    }

    LaunchedEffect(state.paymentSelection) {
        interactor.handleViewAction(AddPaymentMethodInteractor.ViewAction.ClearErrorMessages)
    }

    val formElements = remember(selectedPaymentMethodCode) {
        interactor.formElementsForCode(selectedPaymentMethodCode)
    }

    val usBankAccountFormArguments = remember(selectedPaymentMethodCode) {
        interactor.createUsBankAccountFormElements(selectedPaymentMethodCode)
    }

    PaymentElement(
        enabled = !state.processing,
        supportedPaymentMethods = supportedPaymentMethods,
        selectedItemCode = selectedPaymentMethodCode,
        formElements = formElements,
        linkSignupMode = linkInlineSignupMode,
        linkConfigurationCoordinator = state.linkConfigurationCoordinator,
        onItemSelectedListener = { selectedLpm ->
            if (selectedPaymentMethodCode != selectedLpm.code) {
                selectedPaymentMethodCode = selectedLpm.code
                interactor.handleViewAction(
                    AddPaymentMethodInteractor.ViewAction.ReportPaymentMethodTypeSelected(
                        selectedLpm.code
                    )
                )
            }
        },
        onLinkSignupStateChanged = {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnLinkSignUpStateUpdated(it)
            )
        },
        formArguments = state.arguments, // TODO: figure out why this doesn't work
        usBankAccountFormArguments = usBankAccountFormArguments,
        onFormFieldValuesChanged = { formValues ->
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnFormFieldValuesChanged(
                    formValues,
                    selectedPaymentMethodCode
                )
            )
        },
        modifier = modifier,
        onInteractionEvent = {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.ReportFieldInteraction(
                    selectedPaymentMethodCode
                )
            )
        },
    )
}

internal fun FormFieldValues.transformToPaymentMethodCreateParams(
    paymentMethodCode: PaymentMethodCode,
    paymentMethodMetadata: PaymentMethodMetadata,
): PaymentMethodCreateParams {
    return FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
        fieldValuePairs = fieldValuePairs,
        code = paymentMethodCode,
        requiresMandate = paymentMethodMetadata.requiresMandate(paymentMethodCode),
    )
}

internal fun FormFieldValues.transformToPaymentMethodOptionsParams(
    paymentMethodCode: PaymentMethodCode
): PaymentMethodOptionsParams? {
    return FieldValuesToParamsMapConverter.transformToPaymentMethodOptionsParams(
        fieldValuePairs = fieldValuePairs,
        code = paymentMethodCode,
    )
}

internal fun FormFieldValues.transformToExtraParams(
    paymentMethodCode: PaymentMethodCode
): PaymentMethodExtraParams? {
    return FieldValuesToParamsMapConverter.transformToPaymentMethodExtraParams(
        fieldValuePairs = fieldValuePairs,
        code = paymentMethodCode,
    )
}

internal fun FormFieldValues.transformToPaymentSelection(
    context: Context,
    paymentMethod: SupportedPaymentMethod,
    paymentMethodMetadata: PaymentMethodMetadata,
): PaymentSelection {
    val params = transformToPaymentMethodCreateParams(paymentMethod.code, paymentMethodMetadata)
    val options = transformToPaymentMethodOptionsParams(paymentMethod.code)
    val extras = transformToExtraParams(paymentMethod.code)
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
            billingDetails = params.billingDetails,
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
