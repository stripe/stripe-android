package com.stripe.android.paymentelement.confirmation

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationOption
import com.stripe.android.paymentelement.confirmation.cpms.CustomPaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.confirmation.linkinline.LinkInlineSignupConfirmationOption
import com.stripe.android.paymentelement.confirmation.shoppay.ShopPayConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun PaymentSelection.toConfirmationOption(
    configuration: CommonConfiguration,
    linkConfiguration: LinkConfiguration?,
    passiveCaptchaParams: PassiveCaptchaParams?,
    clientAttributionMetadata: ClientAttributionMetadata?,
    attestOnIntentConfirmation: Boolean = false
): ConfirmationHandler.Option? {
    return when (this) {
        is PaymentSelection.Saved -> {
            toConfirmationOption(passiveCaptchaParams, clientAttributionMetadata, attestOnIntentConfirmation)
        }
        is PaymentSelection.ExternalPaymentMethod -> toConfirmationOption()
        is PaymentSelection.CustomPaymentMethod -> toConfirmationOption(configuration)
        is PaymentSelection.New.USBankAccount -> {
            toConfirmationOption(passiveCaptchaParams, clientAttributionMetadata, attestOnIntentConfirmation)
        }
        is PaymentSelection.New.LinkInline -> toConfirmationOption(linkConfiguration, passiveCaptchaParams)
        is PaymentSelection.New -> {
            toConfirmationOption(passiveCaptchaParams, clientAttributionMetadata, attestOnIntentConfirmation)
        }
        is PaymentSelection.GooglePay -> {
            toConfirmationOption(
                configuration,
                passiveCaptchaParams,
                clientAttributionMetadata,
                attestOnIntentConfirmation
            )
        }
        is PaymentSelection.Link -> {
            toConfirmationOption(linkConfiguration, passiveCaptchaParams, attestOnIntentConfirmation)
        }
        is PaymentSelection.ShopPay -> toConfirmationOption(configuration)
    }
}

private fun PaymentSelection.Saved.toConfirmationOption(
    passiveCaptchaParams: PassiveCaptchaParams?,
    clientAttributionMetadata: ClientAttributionMetadata?,
    attestOnIntentConfirmation: Boolean,
): PaymentMethodConfirmationOption.Saved {
    return PaymentMethodConfirmationOption.Saved(
        paymentMethod = paymentMethod,
        optionsParams = paymentMethodOptionsParams,
        passiveCaptchaParams = passiveCaptchaParams,
        clientAttributionMetadata = clientAttributionMetadata,
        attestationRequired = attestOnIntentConfirmation
    )
}

private fun PaymentSelection.ExternalPaymentMethod.toConfirmationOption(): ExternalPaymentMethodConfirmationOption {
    return ExternalPaymentMethodConfirmationOption(
        type = type,
        billingDetails = billingDetails,
    )
}

private fun PaymentSelection.New.USBankAccount.toConfirmationOption(
    passiveCaptchaParams: PassiveCaptchaParams?,
    clientAttributionMetadata: ClientAttributionMetadata?,
    attestOnIntentConfirmation: Boolean,
): PaymentMethodConfirmationOption {
    return if (instantDebits != null) {
        // For Instant Debits, we create the PaymentMethod inside the bank auth flow. Therefore,
        // we can just use the already created object here.
        PaymentMethodConfirmationOption.Saved(
            paymentMethod = instantDebits.paymentMethod,
            optionsParams = paymentMethodOptionsParams,
            passiveCaptchaParams = passiveCaptchaParams,
            clientAttributionMetadata = clientAttributionMetadata,
            attestationRequired = attestOnIntentConfirmation
        )
    } else {
        PaymentMethodConfirmationOption.New(
            createParams = paymentMethodCreateParams,
            optionsParams = paymentMethodOptionsParams,
            extraParams = paymentMethodExtraParams,
            shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
            passiveCaptchaParams = passiveCaptchaParams,
            clientAttributionMetadata = clientAttributionMetadata,
            attestationRequired = attestOnIntentConfirmation
        )
    }
}

private fun PaymentSelection.New.LinkInline.toConfirmationOption(
    linkConfiguration: LinkConfiguration?,
    passiveCaptchaParams: PassiveCaptchaParams?
): LinkInlineSignupConfirmationOption? {
    return linkConfiguration?.let {
        LinkInlineSignupConfirmationOption(
            createParams = paymentMethodCreateParams,
            optionsParams = paymentMethodOptionsParams,
            extraParams = paymentMethodExtraParams,
            saveOption = when (customerRequestedSave) {
                PaymentSelection.CustomerRequestedSave.RequestReuse ->
                    LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.RequestedReuse
                PaymentSelection.CustomerRequestedSave.RequestNoReuse ->
                    LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.RequestedNoReuse
                PaymentSelection.CustomerRequestedSave.NoRequest ->
                    LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.NoRequest
            },
            linkConfiguration = linkConfiguration,
            userInput = input,
            passiveCaptchaParams = passiveCaptchaParams
        )
    }
}

private fun PaymentSelection.New.toConfirmationOption(
    passiveCaptchaParams: PassiveCaptchaParams?,
    clientAttributionMetadata: ClientAttributionMetadata?,
    attestOnIntentConfirmation: Boolean,
): ConfirmationHandler.Option {
    return if (paymentMethodCreateParams.typeCode == PaymentMethod.Type.BacsDebit.code) {
        BacsConfirmationOption(
            createParams = paymentMethodCreateParams,
            optionsParams = paymentMethodOptionsParams,
            passiveCaptchaParams = passiveCaptchaParams,
            attestationRequired = attestOnIntentConfirmation
        )
    } else {
        PaymentMethodConfirmationOption.New(
            createParams = paymentMethodCreateParams,
            optionsParams = paymentMethodOptionsParams,
            extraParams = paymentMethodExtraParams,
            shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
            passiveCaptchaParams = passiveCaptchaParams,
            clientAttributionMetadata = clientAttributionMetadata,
            attestationRequired = attestOnIntentConfirmation
        )
    }
}

private fun PaymentSelection.GooglePay.toConfirmationOption(
    configuration: CommonConfiguration,
    passiveCaptchaParams: PassiveCaptchaParams?,
    clientAttributionMetadata: ClientAttributionMetadata?,
    attestOnIntentConfirmation: Boolean,
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
            ),
            passiveCaptchaParams = passiveCaptchaParams,
            clientAttributionMetadata = clientAttributionMetadata,
            attestationRequired = attestOnIntentConfirmation,
        )
    }
}

private fun PaymentSelection.Link.toConfirmationOption(
    linkConfiguration: LinkConfiguration?,
    passiveCaptchaParams: PassiveCaptchaParams?,
    attestOnIntentConfirmation: Boolean,
): LinkConfirmationOption? {
    return linkConfiguration?.let {
        LinkConfirmationOption(
            configuration = linkConfiguration,
            linkExpressMode = linkExpressMode,
            linkLaunchMode = when {
                // If a payment is included in the confirmation option, launch confirmation right away
                selectedPayment != null -> LinkLaunchMode.Confirmation(selectedPayment)
                // If a payment is not included, launch the link flow regularly
                else -> LinkLaunchMode.Full
            },
            passiveCaptchaParams = passiveCaptchaParams,
            attestationRequired = attestOnIntentConfirmation,
        )
    }
}

private fun PaymentSelection.CustomPaymentMethod.toConfirmationOption(
    configuration: CommonConfiguration
): CustomPaymentMethodConfirmationOption? {
    return configuration.customPaymentMethods.firstOrNull { type ->
        type.id == id
    }?.let { type ->
        CustomPaymentMethodConfirmationOption(
            customPaymentMethodType = type,
            billingDetails = billingDetails
        )
    }
}

private fun PaymentSelection.ShopPay.toConfirmationOption(
    configuration: CommonConfiguration
): ShopPayConfirmationOption? {
    val customerSessionClientSecret = when (val accessType = configuration.customer?.accessType) {
        is PaymentSheet.CustomerAccessType.CustomerSession -> accessType.customerSessionClientSecret
        else -> return null
    }
    return configuration.shopPayConfiguration?.let { config ->
        ShopPayConfirmationOption(
            shopPayConfiguration = config,
            customerSessionClientSecret = customerSessionClientSecret,
            merchantDisplayName = configuration.merchantDisplayName
        )
    }
}
