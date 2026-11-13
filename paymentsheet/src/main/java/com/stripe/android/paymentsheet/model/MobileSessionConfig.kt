package com.stripe.android.paymentsheet.model

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode

internal fun CommonConfiguration.toMobileSessionConfig(hasFinancialConnectionsSdk: Boolean): MobileSession.Config {
    val billingCollection = billingDetailsCollectionConfiguration
    return MobileSession.Config(
        paymentElementConfig = MobileSession.PaymentElementConfig(
            allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
            hasReturnUrl = true,
            defaultBillingDetails = MobileSession.DefaultBillingDetailsPresence(
                email = !defaultBillingDetails?.email.isNullOrBlank(),
            ),
            billingDetailsCollection = MobileSession.BillingDetailsCollection(
                name = billingCollection.name.toMobileSessionCollectionMode(),
                phone = billingCollection.phone.toMobileSessionCollectionMode(),
                email = billingCollection.email.toMobileSessionCollectionMode(),
                address = when (billingCollection.address) {
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic ->
                        MobileSession.AddressCollectionMode.AUTOMATIC
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never ->
                        MobileSession.AddressCollectionMode.NEVER
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full ->
                        MobileSession.AddressCollectionMode.FULL
                },
                attachDefaultsToPaymentMethod = billingCollection.attachDefaultsToPaymentMethod,
            ),
            link = MobileSession.LinkConfig(
                displayMode = when (link.display) {
                    PaymentSheet.LinkConfiguration.Display.Automatic -> MobileSession.LinkDisplay.AUTOMATIC
                    PaymentSheet.LinkConfiguration.Display.Never -> MobileSession.LinkDisplay.NEVER
                    PaymentSheet.LinkConfiguration.Display.WalletButtonHidden ->
                        MobileSession.LinkDisplay.WALLET_BUTTON_HIDDEN
                },
            ),
            cardBrandAcceptance = cardBrandAcceptance.toMobileSessionCardBrandAcceptance(),
            paymentMethodOrder = paymentMethodOrder,
        ),
        environment = MobileSession.SdkEnvironment(
            hasFinancialConnectionsSdk = hasFinancialConnectionsSdk,
            deviceSupportsNativeLink = true,
        ),
    )
}

private fun CollectionMode.toMobileSessionCollectionMode() =
    when (this) {
        CollectionMode.Automatic -> MobileSession.CollectionMode.AUTOMATIC
        CollectionMode.Never -> MobileSession.CollectionMode.NEVER
        CollectionMode.Always -> MobileSession.CollectionMode.ALWAYS
    }

private fun PaymentSheet.CardBrandAcceptance.toMobileSessionCardBrandAcceptance(): MobileSession.CardBrandAcceptance {
    return when (this) {
        PaymentSheet.CardBrandAcceptance.All -> MobileSession.CardBrandAcceptance(
            filter = MobileSession.CardBrandFilter.ALL,
            brands = emptyList(),
        )
        is PaymentSheet.CardBrandAcceptance.Allowed -> MobileSession.CardBrandAcceptance(
            filter = MobileSession.CardBrandFilter.ALLOWED,
            brands = brands.map { it.toMobileSessionCardBrandCategory() },
        )
        is PaymentSheet.CardBrandAcceptance.Disallowed -> MobileSession.CardBrandAcceptance(
            filter = MobileSession.CardBrandFilter.DISALLOWED,
            brands = brands.map { it.toMobileSessionCardBrandCategory() },
        )
    }
}

private fun PaymentSheet.CardBrandAcceptance.BrandCategory.toMobileSessionCardBrandCategory() = when (this) {
    PaymentSheet.CardBrandAcceptance.BrandCategory.Visa -> MobileSession.CardBrandCategory.VISA
    PaymentSheet.CardBrandAcceptance.BrandCategory.Mastercard -> MobileSession.CardBrandCategory.MASTERCARD
    PaymentSheet.CardBrandAcceptance.BrandCategory.Amex -> MobileSession.CardBrandCategory.AMEX
    PaymentSheet.CardBrandAcceptance.BrandCategory.Discover -> MobileSession.CardBrandCategory.DISCOVER
}
