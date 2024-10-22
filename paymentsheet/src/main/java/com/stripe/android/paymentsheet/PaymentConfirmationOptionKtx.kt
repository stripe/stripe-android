package com.stripe.android.paymentsheet

import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun PaymentSelection.toPaymentConfirmationOption(
    initializationMode: PaymentSheet.InitializationMode,
    configuration: PaymentSheet.Configuration,
): PaymentConfirmationOption? {
    return when (this) {
        is PaymentSelection.Saved -> PaymentConfirmationOption.PaymentMethod.Saved(
            initializationMode = initializationMode,
            shippingDetails = configuration.shippingDetails,
            paymentMethod = paymentMethod,
            optionsParams = paymentMethodOptionsParams,
        )
        is PaymentSelection.ExternalPaymentMethod -> PaymentConfirmationOption.ExternalPaymentMethod(
            type = type,
            billingDetails = billingDetails,
        )
        is PaymentSelection.New -> {
            if (paymentMethodCreateParams.typeCode == PaymentMethod.Type.BacsDebit.code) {
                PaymentConfirmationOption.BacsPaymentMethod(
                    initializationMode = initializationMode,
                    shippingDetails = configuration.shippingDetails,
                    createParams = paymentMethodCreateParams,
                    optionsParams = paymentMethodOptionsParams,
                    appearance = configuration.appearance,
                )
            } else {
                PaymentConfirmationOption.PaymentMethod.New(
                    initializationMode = initializationMode,
                    shippingDetails = configuration.shippingDetails,
                    createParams = paymentMethodCreateParams,
                    optionsParams = paymentMethodOptionsParams,
                    shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
                )
            }
        }
        is PaymentSelection.GooglePay -> configuration.googlePay?.let { googlePay ->
            PaymentConfirmationOption.GooglePay(
                initializationMode = initializationMode,
                shippingDetails = configuration.shippingDetails,
                config = PaymentConfirmationOption.GooglePay.Config(
                    environment = googlePay.environment,
                    merchantName = configuration.merchantDisplayName,
                    merchantCountryCode = googlePay.countryCode,
                    merchantCurrencyCode = googlePay.currencyCode,
                    customAmount = googlePay.amount,
                    customLabel = googlePay.label,
                    billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
                    cardBrandFilter = PaymentSheetCardBrandFilter(configuration.cardBrandAcceptance)
                )
            )
        }
        is PaymentSelection.Link -> null
    }
}
