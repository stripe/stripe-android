package com.stripe.android.link

import android.content.Context
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

/** Supplies retained passthrough metadata to the consumer-details preview mapper. */
internal fun LinkPaymentMethod.toPreview(
    context: Context,
    iconLoader: PaymentSelection.IconLoader,
    reduceLinkBranding: Boolean,
    isDarkTheme: Boolean,
): LinkController.PaymentMethodPreview {
    val linkDetails = (this as? LinkPaymentMethod.LinkPaymentDetails)?.linkPaymentDetails
    return details.toPreview(
        context = context,
        iconLoader = iconLoader,
        reduceLinkBranding = reduceLinkBranding,
        isDarkTheme = isDarkTheme,
        passthroughCard = (linkDetails as? LinkPaymentDetails.Passthrough)?.paymentMethod?.card,
    )
}

internal fun ConsumerPaymentDetails.PaymentDetails.instrumentDetails(
    passthroughCard: PaymentMethod.Card?
): PaymentMethodPreviewDetails? = when (this) {
    is ConsumerPaymentDetails.Card -> PaymentMethodPreviewDetails.Card(
        brand = brand,
        funding = funding.cardFunding.code,
        last4 = last4,
    )
    is ConsumerPaymentDetails.BankAccount -> PaymentMethodPreviewDetails.BankAccount(
        bankIconCode = bankIconCode,
        bankName = bankAccountName,
        last4 = last4,
    )
    is ConsumerPaymentDetails.Passthrough -> PaymentMethodPreviewDetails.Card(
        // Only a known passthrough card can use retained PaymentMethod metadata.
        brand = passthroughCard?.brand ?: CardBrand.Unknown,
        funding = CardFunding.fromCode(passthroughCard?.funding).code,
        last4 = passthroughCard?.last4 ?: last4,
    )
    is ConsumerPaymentDetails.Generic -> null
}
