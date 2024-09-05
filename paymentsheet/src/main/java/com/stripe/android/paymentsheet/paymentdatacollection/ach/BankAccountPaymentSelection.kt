package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.IdentifierSpec

internal fun FormFieldValues.createBankAccountPaymentSelection(
    createParams: PaymentMethodCreateParams,
): PaymentSelection.New.USBankAccount {
    val bankName = fieldValuePairs[IdentifierSpec.BankName]?.value
    val last4 = fieldValuePairs[IdentifierSpec.Last4]?.value

    return PaymentSelection.New.USBankAccount(
        code = PaymentMethod.Type.USBankAccount.code,
        labelResource = resolvableString(
            R.string.stripe_paymentsheet_payment_method_item_card_number,
            last4,
        ),
        iconResource = TransformToBankIcon(bankName),
        paymentMethodCreateParams = createParams,
        paymentMethodOptionsParams = PaymentMethodOptionsParams.USBankAccount(
            setupFutureUsage = userRequestedReuse.setupFutureUsage
        ),
        customerRequestedSave = userRequestedReuse,
        screenState = USBankAccountFormScreenState.BillingDetailsCollection(
            error = null,
            primaryButtonText = "".resolvableString,
            isProcessing = false,
        ),
        instantDebits = null,
        input = PaymentSelection.New.USBankAccount.Input(
            name = "",
            email = null,
            phone = null,
            address = null,
            saveForFutureUse = false,
        ),
    )
}

internal fun FormFieldValues.createInstantDebitsPaymentSelection(
    createParams: PaymentMethodCreateParams,
): PaymentSelection.New.USBankAccount {
    val bankName = fieldValuePairs[IdentifierSpec.BankName]?.value
    val last4 = fieldValuePairs[IdentifierSpec.Last4]?.value

    return PaymentSelection.New.USBankAccount(
        code = PaymentMethod.Type.Link.code,
        labelResource = resolvableString(
            R.string.stripe_paymentsheet_payment_method_item_card_number,
            last4,
        ),
        iconResource = TransformToBankIcon(bankName),
        paymentMethodCreateParams = createParams,
        paymentMethodOptionsParams = null,
        customerRequestedSave = userRequestedReuse,
        screenState = USBankAccountFormScreenState.BillingDetailsCollection(
            error = null,
            primaryButtonText = "".resolvableString,
            isProcessing = false,
        ),
        instantDebits = createParams.instantDebitsPaymentMethodId()?.let {
            PaymentSelection.New.USBankAccount.InstantDebitsInfo(paymentMethodId = it)
        },
        input = PaymentSelection.New.USBankAccount.Input(
            name = "",
            email = null,
            phone = null,
            address = null,
            saveForFutureUse = false,
        ),
    )
}
