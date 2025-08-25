package com.stripe.android.paymentelement.confirmation

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
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

@SuppressWarnings("UnusedParameter")
internal fun PaymentSelection.toConfirmationOption(
    configuration: CommonConfiguration,
    linkConfiguration: LinkConfiguration?,
    passiveCaptchaParams: PassiveCaptchaParams?
): ConfirmationHandler.Option? {
    return when (this) {
        is PaymentSelection.Saved -> toConfirmationOption(passiveCaptchaParams = null)
        is PaymentSelection.ExternalPaymentMethod -> toConfirmationOption()
        is PaymentSelection.CustomPaymentMethod -> toConfirmationOption(configuration)
        is PaymentSelection.New.USBankAccount -> toConfirmationOption(passiveCaptchaParams = null)
        is PaymentSelection.New.LinkInline -> toConfirmationOption(linkConfiguration, passiveCaptchaParams = null)
        is PaymentSelection.New -> toConfirmationOption(passiveCaptchaParams = null)
        is PaymentSelection.GooglePay -> toConfirmationOption(configuration, passiveCaptchaParams = null)
        is PaymentSelection.Link -> toConfirmationOption(linkConfiguration, passiveCaptchaParams = null)
        is PaymentSelection.ShopPay -> toConfirmationOption(configuration)
    }
}

private fun PaymentSelection.Saved.toConfirmationOption(
    passiveCaptchaParams: PassiveCaptchaParams?
): PaymentMethodConfirmationOption.Saved {
    return PaymentMethodConfirmationOption.Saved(
        paymentMethod = paymentMethod,
        optionsParams = paymentMethodOptionsParams,
        passiveCaptchaParams = passiveCaptchaParams
    )
}

private fun PaymentSelection.ExternalPaymentMethod.toConfirmationOption(): ExternalPaymentMethodConfirmationOption {
    return ExternalPaymentMethodConfirmationOption(
        type = type,
        billingDetails = billingDetails,
    )
}

private fun PaymentSelection.New.USBankAccount.toConfirmationOption(
    passiveCaptchaParams: PassiveCaptchaParams?
): PaymentMethodConfirmationOption {
    return if (instantDebits != null) {
        // For Instant Debits, we create the PaymentMethod inside the bank auth flow. Therefore,
        // we can just use the already created object here.
        PaymentMethodConfirmationOption.Saved(
            paymentMethod = instantDebits.paymentMethod,
            optionsParams = paymentMethodOptionsParams,
            passiveCaptchaParams = passiveCaptchaParams
        )
    } else {
        PaymentMethodConfirmationOption.New(
            createParams = paymentMethodCreateParams,
            optionsParams = paymentMethodOptionsParams,
            extraParams = paymentMethodExtraParams,
            shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
            passiveCaptchaParams = passiveCaptchaParams
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
    passiveCaptchaParams: PassiveCaptchaParams?
): ConfirmationHandler.Option {
    return if (paymentMethodCreateParams.typeCode == PaymentMethod.Type.BacsDebit.code) {
        BacsConfirmationOption(
            createParams = paymentMethodCreateParams,
            optionsParams = paymentMethodOptionsParams,
            passiveCaptchaParams = passiveCaptchaParams
        )
    } else {
        PaymentMethodConfirmationOption.New(
            createParams = paymentMethodCreateParams,
            optionsParams = paymentMethodOptionsParams,
            extraParams = paymentMethodExtraParams,
            shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
            passiveCaptchaParams = passiveCaptchaParams
        )
    }
}

private fun PaymentSelection.GooglePay.toConfirmationOption(
    configuration: CommonConfiguration,
    passiveCaptchaParams: PassiveCaptchaParams?
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
            passiveCaptchaParams = passiveCaptchaParams
        )
    }
}

private fun PaymentSelection.Link.toConfirmationOption(
    linkConfiguration: LinkConfiguration?,
    passiveCaptchaParams: PassiveCaptchaParams?
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
            passiveCaptchaParams = passiveCaptchaParams
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
