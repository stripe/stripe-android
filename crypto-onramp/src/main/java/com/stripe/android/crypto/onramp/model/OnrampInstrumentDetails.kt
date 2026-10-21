package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding

/** A snapshot of the selected funding instrument at successful collection. */
@ExperimentalCryptoOnramp
sealed interface OnrampInstrumentDetails {
    /** Selected card metadata. Wallet last4 may identify a tokenized card number. */
    @ExperimentalCryptoOnramp
    class Card internal constructor(
        val funding: CardFunding,
        val brand: CardBrand,
        val last4: String?
    ) : OnrampInstrumentDetails

    /** Selected bank account metadata, not proof of ownership or mandate validity. */
    @ExperimentalCryptoOnramp
    class BankAccount internal constructor(
        val last4: String?
    ) : OnrampInstrumentDetails
}
