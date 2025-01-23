package com.stripe.android.paymentelement.confirmation

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationOption
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.confirmation.linkexpress.LinkExpressConfirmationOption
import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun PaymentSelection.toConfirmationOption(
    configuration: CommonConfiguration,
    linkConfiguration: LinkConfiguration?,
): ConfirmationHandler.Option? {
    return when (this) {
        is PaymentSelection.Saved -> PaymentMethodConfirmationOption.Saved(
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
        is PaymentSelection.New -> {
            if (paymentMethodCreateParams.typeCode == PaymentMethod.Type.BacsDebit.code) {
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
        is PaymentSelection.GooglePay -> configuration.googlePay?.let { googlePay ->
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
        is PaymentSelection.Link -> linkConfiguration?.let {
            LinkConfirmationOption(
                configuration = linkConfiguration,
                linkAccount = null
            )
        }
    }
}

internal fun LinkAccount.toConfirmationOption(
    linkConfiguration: LinkConfiguration
): ConfirmationHandler.Option? {
    return toLinkConfirmationOption(
        linkConfiguration = linkConfiguration,
        linkAccount = this
    )
}

private fun toLinkConfirmationOption(
    linkConfiguration: LinkConfiguration?,
    linkAccount: LinkAccount
): ConfirmationHandler.Option? {
    if (linkConfiguration == null) return null
    return when (linkAccount.accountStatus) {
        AccountStatus.Verified -> {
            LinkConfirmationOption(
                configuration = linkConfiguration,
                linkAccount = linkAccount
            )
        }
        AccountStatus.NeedsVerification,
        AccountStatus.VerificationStarted -> {
            LinkExpressConfirmationOption(
                configuration = linkConfiguration,
                linkAccount = linkAccount
            )
        }
        else -> null
    }
}
