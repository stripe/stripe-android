package com.stripe.android.link.ui.wallet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.R as StripeUiCoreR

internal val ConsumerPaymentDetails.PaymentDetails.displayName: ResolvableString
    get() = when (this) {
        is ConsumerPaymentDetails.Card -> {
            nickname?.resolvableString ?: makeFallbackCardName(funding, brand.displayName)
        }
        is ConsumerPaymentDetails.BankAccount -> {
            nickname?.resolvableString ?: bankName?.resolvableString
                ?: StripeUiCoreR.string.stripe_payment_method_bank.resolvableString
        }
        is ConsumerPaymentDetails.Passthrough -> {
            "•••• $last4".resolvableString
        }
    }

private fun makeFallbackCardName(funding: String, brand: String): ResolvableString {
    return when (funding) {
        "CREDIT" -> resolvableString(R.string.stripe_link_card_type_credit, brand)
        "DEBIT" -> resolvableString(R.string.stripe_link_card_type_debit, brand)
        "PREPAID" -> resolvableString(R.string.stripe_link_card_type_prepaid, brand)
        "CHARGE", "FUNDING_INVALID" -> resolvableString(R.string.stripe_link_card_type_unknown, brand)
        else -> resolvableString(R.string.stripe_link_card_type_unknown, brand)
    }
}
