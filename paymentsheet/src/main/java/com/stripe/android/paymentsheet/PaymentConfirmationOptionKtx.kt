package com.stripe.android.paymentsheet

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal fun PaymentSelection.toPaymentConfirmationOption(
    initializationMode: PaymentElementLoader.InitializationMode,
    configuration: CommonConfiguration,
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
        is PaymentSelection.New.USBankAccount -> {
            if (instantDebits != null) {
                // For Instant Debits, we create the PaymentMethod inside the bank auth flow. Therefore,
                // we can just use the already created object here.
                PaymentConfirmationOption.PaymentMethod.Saved(
                    initializationMode = initializationMode,
                    shippingDetails = configuration.shippingDetails,
                    paymentMethod = instantDebits.paymentMethod,
                    optionsParams = paymentMethodOptionsParams,
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
