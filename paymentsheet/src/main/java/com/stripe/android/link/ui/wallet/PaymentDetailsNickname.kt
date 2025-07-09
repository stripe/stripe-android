package com.stripe.android.link.ui.wallet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.plus
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkPaymentDetails
import com.stripe.android.model.LinkPaymentDetails.BankAccount
import com.stripe.android.model.LinkPaymentDetails.Card
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.R as StripeUiCoreR

internal val LinkPaymentDetails.paymentOptionLabel: ResolvableString
    get() {
        val components = listOfNotNull(label, sublabel)
        return components.joinToString(separator = " ")
    }

internal val LinkPaymentDetails.label: ResolvableString
    get() = when (this) {
        is Card -> makeCardDisplayName(nickname, funding, brand)
        is BankAccount -> bankName?.resolvableString ?: "••••$last4".resolvableString
    }

internal val LinkPaymentDetails.sublabel: ResolvableString?
    get() = when (this) {
        is Card -> "•••• $last4".resolvableString
        is BankAccount -> if (bankName != null) "••••$last4".resolvableString else null
    }

internal val ConsumerPaymentDetails.PaymentDetails.displayName: ResolvableString
    get() = when (this) {
        is ConsumerPaymentDetails.Card -> makeCardDisplayName(nickname, funding, brand)
        is ConsumerPaymentDetails.BankAccount -> makeBankAccountDisplayName(nickname, bankName)
    }

internal val ConsumerPaymentDetails.PaymentDetails.paymentOptionLabel: ResolvableString
    get() {
        val components = when (this) {
            is ConsumerPaymentDetails.Card -> {
                listOf(makeCardDisplayName(nickname, funding, brand), "•••• $last4".resolvableString)
            }
            is ConsumerPaymentDetails.BankAccount -> {
                listOf(makeBankAccountDisplayName(nickname, bankName), "•••• $last4".resolvableString)
            }
        }
        return components.joinToString(separator = " ")
    }

private fun makeCardDisplayName(nickname: String?, funding: String, brand: CardBrand): ResolvableString {
    return nickname?.resolvableString ?: makeFallbackCardName(funding, brand.displayName)
}

private fun makeBankAccountDisplayName(nickname: String?, bankName: String?): ResolvableString {
    return nickname?.resolvableString
        ?: bankName?.resolvableString
        ?: StripeUiCoreR.string.stripe_payment_method_bank.resolvableString
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

private fun List<ResolvableString>.joinToString(separator: String): ResolvableString {
    return reduce { acc, text ->
        acc + separator.resolvableString + text
    }
}
