package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun PaymentSelection.toPaymentConfirmationOption(): PaymentConfirmationOption? {
    return when (this) {
        is PaymentSelection.Saved -> PaymentConfirmationOption.Saved(
            paymentMethod = paymentMethod,
            optionsParams = paymentMethodOptionsParams,
        )
        is PaymentSelection.ExternalPaymentMethod -> PaymentConfirmationOption.ExternalPaymentMethod(
            type = type,
            billingDetails = billingDetails,
        )
        is PaymentSelection.New -> {
            PaymentConfirmationOption.New(
                createParams = paymentMethodCreateParams,
                optionsParams = paymentMethodOptionsParams,
                shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
            )
        }
        is PaymentSelection.Link,
        is PaymentSelection.GooglePay -> null
    }
}
