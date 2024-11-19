package com.stripe.android.paymentelement.confirmation

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationOption
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal fun PaymentSelection.toConfirmationOption(
    initializationMode: PaymentElementLoader.InitializationMode,
    configuration: CommonConfiguration,
    appearance: PaymentSheet.Appearance,
): ConfirmationHandler.Option? {
    return when (this) {
        is PaymentSelection.Saved -> PaymentMethodConfirmationOption.Saved(
            initializationMode = initializationMode,
            shippingDetails = configuration.shippingDetails,
            paymentMethod = paymentMethod,
            optionsParams = paymentMethodOptionsParams,
        )
        is PaymentSelection.ExternalPaymentMethod -> ExternalPaymentMethodConfirmationOption(
            type = type,
            billingDetails = billingDetails,
        )
        is PaymentSelection.New.USBankAccount -> {
            if (instantDebits != null) {
                // For Instant Debits, we create the PaymentMethod inside the bank auth flow. Therefore,
                // we can just use the already created object here.
                PaymentMethodConfirmationOption.Saved(
                    initializationMode = initializationMode,
                    shippingDetails = configuration.shippingDetails,
                    paymentMethod = instantDebits.paymentMethod,
                    optionsParams = paymentMethodOptionsParams,
                )
            } else {
                PaymentMethodConfirmationOption.New(
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
                BacsConfirmationOption(
                    initializationMode = initializationMode,
                    shippingDetails = configuration.shippingDetails,
                    createParams = paymentMethodCreateParams,
                    optionsParams = paymentMethodOptionsParams,
                    appearance = appearance,
                )
            } else {
                PaymentMethodConfirmationOption.New(
                    initializationMode = initializationMode,
                    shippingDetails = configuration.shippingDetails,
                    createParams = paymentMethodCreateParams,
                    optionsParams = paymentMethodOptionsParams,
                    shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
                )
            }
        }
        is PaymentSelection.GooglePay -> configuration.googlePay?.let { googlePay ->
            GooglePayConfirmationOption(
                initializationMode = initializationMode,
                shippingDetails = configuration.shippingDetails,
                config = GooglePayConfirmationOption.Config(
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
