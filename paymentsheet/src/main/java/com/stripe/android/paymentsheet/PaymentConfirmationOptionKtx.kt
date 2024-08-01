package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun PaymentSelection.toPaymentConfirmationOption(
    configuration: PaymentSheet.Configuration?,
): PaymentConfirmationOption<*>? {
    return when (this) {
        is PaymentSelection.Saved -> PaymentConfirmationOption.PaymentMethod.Saved(
            arguments = PaymentConfirmationOption.PaymentMethod.Saved.Args(
                paymentMethod = paymentMethod,
                optionsParams = paymentMethodOptionsParams,
            )
        )
        is PaymentSelection.ExternalPaymentMethod -> PaymentConfirmationOption.ExternalPaymentMethod(
            arguments = PaymentConfirmationOption.ExternalPaymentMethod.Args(
                type = type,
                billingDetails = billingDetails,
            )
        )
        is PaymentSelection.New -> {
            PaymentConfirmationOption.PaymentMethod.New(
                arguments = PaymentConfirmationOption.PaymentMethod.New.Args(
                    createParams = paymentMethodCreateParams,
                    optionsParams = paymentMethodOptionsParams,
                    shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
                )
            )
        }
        is PaymentSelection.GooglePay -> configuration?.googlePay?.let { googlePay ->
            PaymentConfirmationOption.GooglePay(
                arguments = PaymentConfirmationOption.GooglePay.Args(
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
