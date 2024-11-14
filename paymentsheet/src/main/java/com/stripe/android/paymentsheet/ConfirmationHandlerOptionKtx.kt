package com.stripe.android.paymentsheet

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal fun PaymentSelection.toConfirmationOption(
    initializationMode: PaymentElementLoader.InitializationMode,
    configuration: CommonConfiguration,
    appearance: PaymentSheet.Appearance,
): ConfirmationHandler.Option? {
    return when (this) {
        is PaymentSelection.Saved -> ConfirmationHandler.Option.PaymentMethod.Saved(
            initializationMode = initializationMode,
            shippingDetails = configuration.shippingDetails,
            paymentMethod = paymentMethod,
            optionsParams = paymentMethodOptionsParams,
        )
        is PaymentSelection.ExternalPaymentMethod -> ConfirmationHandler.Option.ExternalPaymentMethod(
            type = type,
            billingDetails = billingDetails,
        )
        is PaymentSelection.New.USBankAccount -> {
            if (instantDebits != null) {
                // For Instant Debits, we create the PaymentMethod inside the bank auth flow. Therefore,
                // we can just use the already created object here.
                ConfirmationHandler.Option.PaymentMethod.Saved(
                    initializationMode = initializationMode,
                    shippingDetails = configuration.shippingDetails,
                    paymentMethod = instantDebits.paymentMethod,
                    optionsParams = paymentMethodOptionsParams,
                )
            } else {
                ConfirmationHandler.Option.PaymentMethod.New(
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
                ConfirmationHandler.Option.BacsPaymentMethod(
                    initializationMode = initializationMode,
                    shippingDetails = configuration.shippingDetails,
                    createParams = paymentMethodCreateParams,
                    optionsParams = paymentMethodOptionsParams,
                    appearance = appearance,
                )
            } else {
                ConfirmationHandler.Option.PaymentMethod.New(
                    initializationMode = initializationMode,
                    shippingDetails = configuration.shippingDetails,
                    createParams = paymentMethodCreateParams,
                    optionsParams = paymentMethodOptionsParams,
                    shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
                )
            }
        }
        is PaymentSelection.GooglePay -> configuration.googlePay?.let { googlePay ->
            ConfirmationHandler.Option.GooglePay(
                initializationMode = initializationMode,
                shippingDetails = configuration.shippingDetails,
                config = ConfirmationHandler.Option.GooglePay.Config(
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
