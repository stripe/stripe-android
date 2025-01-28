package com.stripe.android.paymentelement.confirmation

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationOption
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.confirmation.linkinline.LinkInlineSignupConfirmationOption
import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun PaymentSelection.toConfirmationOption(
    configuration: CommonConfiguration,
    linkConfiguration: LinkConfiguration?,
): ConfirmationHandler.Option? {
    return when (this) {
        is PaymentSelection.Saved -> toConfirmationOption()
        is PaymentSelection.ExternalPaymentMethod -> toConfirmationOption()
        is PaymentSelection.New.USBankAccount -> toConfirmationOption()
        is PaymentSelection.New.LinkInline -> toConfirmationOption(linkConfiguration)
        is PaymentSelection.New -> toConfirmationOption()
        is PaymentSelection.GooglePay -> toConfirmationOption(configuration)
        is PaymentSelection.Link -> toConfirmationOption(linkConfiguration)
    }
}

private fun PaymentSelection.Saved.toConfirmationOption(): PaymentMethodConfirmationOption.Saved {
    return PaymentMethodConfirmationOption.Saved(
        paymentMethod = paymentMethod,
        optionsParams = paymentMethodOptionsParams,
    )
}

private fun PaymentSelection.ExternalPaymentMethod.toConfirmationOption(): ExternalPaymentMethodConfirmationOption {
    return ExternalPaymentMethodConfirmationOption(
        type = type,
        billingDetails = billingDetails,
    )
}

private fun PaymentSelection.New.USBankAccount.toConfirmationOption(): PaymentMethodConfirmationOption {
    return if (instantDebits != null) {
        // For Instant Debits, we create the PaymentMethod inside the bank auth flow. Therefore,
        // we can just use the already created object here.
        PaymentMethodConfirmationOption.Saved(
            paymentMethod = instantDebits.paymentMethod,
            optionsParams = paymentMethodOptionsParams,
        )
    } else {
        PaymentMethodConfirmationOption.New(
            createParams = paymentMethodCreateParams,
            optionsParams = paymentMethodOptionsParams,
            shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
        )
    }
}

private fun PaymentSelection.New.LinkInline.toConfirmationOption(
    linkConfiguration: LinkConfiguration?
): LinkInlineSignupConfirmationOption? {
    return linkConfiguration?.let {
        LinkInlineSignupConfirmationOption(
            createParams = paymentMethodCreateParams,
            optionsParams = paymentMethodOptionsParams,
            userInput = input,
            linkConfiguration = linkConfiguration,
            saveOption = when (customerRequestedSave) {
                PaymentSelection.CustomerRequestedSave.RequestReuse ->
                    LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.RequestedReuse
                PaymentSelection.CustomerRequestedSave.RequestNoReuse ->
                    LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.RequestedNoReuse
                PaymentSelection.CustomerRequestedSave.NoRequest ->
                    LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.NoRequest
            }
        )
    }
}

private fun PaymentSelection.New.toConfirmationOption(): ConfirmationHandler.Option {
    return if (paymentMethodCreateParams.typeCode == PaymentMethod.Type.BacsDebit.code) {
        BacsConfirmationOption(
            createParams = paymentMethodCreateParams,
            optionsParams = paymentMethodOptionsParams,
        )
    } else {
        PaymentMethodConfirmationOption.New(
            createParams = paymentMethodCreateParams,
            optionsParams = paymentMethodOptionsParams,
            shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
        )
    }
}

private fun PaymentSelection.GooglePay.toConfirmationOption(
    configuration: CommonConfiguration,
): GooglePayConfirmationOption? {
    return configuration.googlePay?.let { googlePay ->
        GooglePayConfirmationOption(
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
}

private fun PaymentSelection.Link.toConfirmationOption(
    linkConfiguration: LinkConfiguration?
): LinkConfirmationOption? {
    return linkConfiguration?.let {
        LinkConfirmationOption(
            configuration = linkConfiguration,
            eagerLaunch = eagerLaunch
        )
    }
}
