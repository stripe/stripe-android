package com.stripe.android.link

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

/** Onramp-only snapshot; the ordinary Link display preview remains unchanged. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampPaymentMethodPreview(
    val preview: LinkController.PaymentMethodPreview,
    val instrumentDetails: PaymentMethodPreviewDetails?
)

internal fun LinkPaymentMethod.toOnrampPreview(
    context: Context,
    iconLoader: PaymentSelection.IconLoader,
    reduceLinkBranding: Boolean,
    isDarkTheme: Boolean,
): OnrampPaymentMethodPreview {
    val linkDetails = (this as? LinkPaymentMethod.LinkPaymentDetails)?.linkPaymentDetails
    val passthroughCard = (linkDetails as? LinkPaymentDetails.Passthrough)?.paymentMethod?.card
    return details.toPreview(
        context = context,
        iconLoader = iconLoader,
        reduceLinkBranding = reduceLinkBranding,
        isDarkTheme = isDarkTheme,
        instrumentDetails = details.instrumentDetails(passthroughCard),
    )
}

/** Passes bounded metadata alongside the existing display preview only for onramp. */
internal fun ConsumerPaymentDetails.PaymentDetails.toPreview(
    context: Context,
    iconLoader: PaymentSelection.IconLoader,
    reduceLinkBranding: Boolean,
    isDarkTheme: Boolean,
    instrumentDetails: PaymentMethodPreviewDetails?,
): OnrampPaymentMethodPreview = OnrampPaymentMethodPreview(
    preview = toPreview(context, iconLoader, reduceLinkBranding, isDarkTheme),
    instrumentDetails = instrumentDetails,
)

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
