package com.stripe.android.crypto.onramp

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.plus
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R as PaymentSheetR

internal fun PaymentMethod.Card.onrampSublabel(): ResolvableString? {
    val last4 = last4 ?: return null
    // Reuse Link's localized strings to keep wallet card descriptions consistent.
    val nameResource = when (funding?.lowercase()) {
        "credit" -> PaymentSheetR.string.stripe_link_card_type_credit
        "debit" -> PaymentSheetR.string.stripe_link_card_type_debit
        "prepaid" -> PaymentSheetR.string.stripe_link_card_type_prepaid
        else -> PaymentSheetR.string.stripe_link_card_type_unknown
    }
    val name = resolvableString(nameResource, brand.displayName)
    return name + " ".resolvableString + resolvableString(PaymentSheetR.string.stripe_link_payment_method_last4, last4)
}
