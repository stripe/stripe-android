package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun PaymentSelection.toPaymentConfirmationOption(
    configuration: PaymentSheet.Configuration?,
): PaymentConfirmationOption? {
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
        is PaymentSelection.GooglePay -> configuration?.googlePay?.let { googlePay ->
            PaymentConfirmationOption.GooglePay(
                config = PaymentConfirmationOption.GooglePay.Config(
                    environment = googlePay.environment,
                    merchantName = configuration.merchantDisplayName,
                    merchantCountryCode = googlePay.countryCode,
                    merchantCurrencyCode = googlePay.currencyCode,
                    customAmount = googlePay.amount,
                    customLabel = googlePay.label,
                    billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
                )
            )
        }
        is PaymentSelection.Link -> null
    }
}
