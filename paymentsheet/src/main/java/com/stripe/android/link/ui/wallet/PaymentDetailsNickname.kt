package com.stripe.android.link.ui.wallet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.plus
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkPaymentDetails
import com.stripe.android.model.LinkPaymentDetails.BankAccount
import com.stripe.android.model.LinkPaymentDetails.Card
import com.stripe.android.model.LinkPaymentDetails.Generic
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
        is BankAccount -> bankName?.resolvableString ?: maskedLast4(last4)
        is Generic -> label.resolvableString
    }

internal val LinkPaymentDetails.sublabel: ResolvableString?
    get() = when (this) {
        is Card -> maskedLast4(last4)
        is BankAccount -> if (bankName != null) maskedLast4(last4) else null
        is Generic -> sublabel?.resolvableString
    }

internal val ConsumerPaymentDetails.PaymentDetails.displayName: ResolvableString
    get() = when (this) {
        is ConsumerPaymentDetails.Card -> makeCardDisplayName(nickname, funding.code, brand)
        is ConsumerPaymentDetails.BankAccount -> makeBankAccountDisplayName(nickname, bankAccountName)
        is ConsumerPaymentDetails.Passthrough -> {
            maskedLast4(last4)
        }
        is ConsumerPaymentDetails.Generic -> display.label.resolvableString
    }

internal val ConsumerPaymentDetails.PaymentDetails.paymentOptionLabel: ResolvableString
    get() {
        val components = when (this) {
            is ConsumerPaymentDetails.Card -> {
                listOf(makeCardDisplayName(nickname, funding.code, brand), maskedLast4(last4))
            }
            is ConsumerPaymentDetails.BankAccount -> {
                listOf(makeBankAccountDisplayName(nickname, bankAccountName), maskedLast4(last4))
            }
            is ConsumerPaymentDetails.Passthrough -> {
                listOf(maskedLast4(last4))
            }
            is ConsumerPaymentDetails.Generic -> {
                listOfNotNull(display.label.resolvableString, display.sublabel?.resolvableString)
            }
        }
        return components.joinToString(separator = " ")
    }

internal fun makeFallbackCardName(funding: String, brand: String): ResolvableString {
    return when (funding) {
        "CREDIT" -> resolvableString(R.string.stripe_link_card_type_credit, brand)
        "DEBIT" -> resolvableString(R.string.stripe_link_card_type_debit, brand)
        "PREPAID" -> resolvableString(R.string.stripe_link_card_type_prepaid, brand)
        "CHARGE", "FUNDING_INVALID" -> resolvableString(R.string.stripe_link_card_type_unknown, brand)
        else -> resolvableString(R.string.stripe_link_card_type_unknown, brand)
    }
}

private fun makeCardDisplayName(nickname: String?, funding: String, brand: CardBrand): ResolvableString {
    return nickname?.resolvableString ?: makeFallbackCardName(funding, brand.displayName)
}

private fun makeBankAccountDisplayName(nickname: String?, bankName: String?): ResolvableString {
    return nickname?.resolvableString
        ?: bankName?.resolvableString
        ?: StripeUiCoreR.string.stripe_payment_method_bank.resolvableString
}

private fun maskedLast4(last4: String): ResolvableString {
    return resolvableString(R.string.stripe_link_payment_method_last4, last4)
}

private fun List<ResolvableString>.joinToString(separator: String): ResolvableString {
    return reduce { acc, text ->
        acc + separator.resolvableString + text
    }
}
