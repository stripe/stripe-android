package com.stripe.android.link

import android.content.Context
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.model.PaymentSelection

/** Builds display data and the bounded instrument snapshot from the same committed selection. */
internal fun LinkPaymentMethod.toPreview(
    context: Context,
    iconLoader: PaymentSelection.IconLoader,
    reduceLinkBranding: Boolean,
    isDarkTheme: Boolean,
): LinkController.PaymentMethodPreview {
    val preview = details.toPreview(context, iconLoader, reduceLinkBranding, isDarkTheme)
    return LinkController.PaymentMethodPreview(
        imageLoader = preview.imageLoader,
        label = preview.label,
        sublabel = preview.sublabel,
        type = preview.type,
        instrumentDetails = instrumentDetails(),
    )
}

private fun LinkPaymentMethod.instrumentDetails(): PaymentMethodPreviewDetails? {
    return when (val selectedDetails = details) {
        is ConsumerPaymentDetails.Card -> PaymentMethodPreviewDetails.Card(
            brand = selectedDetails.brand,
            funding = selectedDetails.funding.cardFunding.code,
            last4 = selectedDetails.last4,
        )
        is ConsumerPaymentDetails.BankAccount -> PaymentMethodPreviewDetails.BankAccount(
            bankIconCode = selectedDetails.bankIconCode,
            bankName = selectedDetails.bankAccountName,
            last4 = selectedDetails.last4,
        )
        is ConsumerPaymentDetails.Passthrough -> {
            // Only a known passthrough card can use the retained PaymentMethod. An arbitrary
            // outer Link PaymentMethod does not identify the underlying funding instrument.
            val linkDetails = (this as? LinkPaymentMethod.LinkPaymentDetails)?.linkPaymentDetails
            val card = (linkDetails as? LinkPaymentDetails.Passthrough)?.paymentMethod?.card
            PaymentMethodPreviewDetails.Card(
                brand = card?.brand ?: CardBrand.Unknown,
                funding = CardFunding.fromCode(card?.funding).code,
                last4 = card?.last4 ?: selectedDetails.last4,
            )
        }
        is ConsumerPaymentDetails.Generic -> null
    }
}
