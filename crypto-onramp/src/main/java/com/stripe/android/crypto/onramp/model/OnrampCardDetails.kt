package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding

/** A snapshot of the selected card. Wallet last4 may identify a tokenized card number. */
@ExperimentalCryptoOnramp
class OnrampCardDetails internal constructor(
    val funding: CardFunding,
    val brand: CardBrand,
    val last4: String?
)
