package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun AddPaymentMethod(
    interactor: AddPaymentMethodInteractor,
    modifier: Modifier = Modifier,
) {
    val state by interactor.state.collectAsState()

    PaymentElement(
        enabled = !state.processing,
        supportedPaymentMethods = state.supportedPaymentMethods,
        incentive = state.incentive,
        selectedItemCode = state.selectedPaymentMethodCode,
        formElements = state.formElements,
        onItemSelectedListener = { selectedLpm ->
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected(
                    selectedLpm.code
                )
            )
        },
        formArguments = state.arguments,
        usBankAccountFormArguments = state.usBankAccountFormArguments,
        onFormFieldValuesChanged = { formValues ->
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnFormFieldValuesChanged(
                    formValues,
                    state.selectedPaymentMethodCode
                )
            )
        },
        modifier = modifier.testTag(PAYMENT_SHEET_FORM_TEST_TAG),
        onInteractionEvent = {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.ReportFieldInteraction(
                    state.selectedPaymentMethodCode
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
        allowRedisplay = paymentMethodMetadata.allowRedisplay(userRequestedReuse),
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
            label = paymentMethod.displayName,
            iconResource = paymentMethod.iconResource,
            lightThemeIconUrl = paymentMethod.lightThemeIconUrl,
            darkThemeIconUrl = paymentMethod.darkThemeIconUrl,
        )
    } else {
        PaymentSelection.New.GenericPaymentMethod(
            label = paymentMethod.displayName,
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

@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val PAYMENT_SHEET_FORM_TEST_TAG = "PaymentSheetAddPaymentMethodForm"
