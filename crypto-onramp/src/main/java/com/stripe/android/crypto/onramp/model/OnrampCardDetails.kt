package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding

/**
 * Card summary for the wallet payment method used to create a crypto payment token.
 *
 * @param funding The card's funding classification, or [CardFunding.Unknown] when unavailable.
 * Funding is not proof of card ownership or identity.
 * @param brand The card's brand.
 * @param last4 The last four digits, when available. For wallets, these may represent the tokenized
 * card number rather than the physical card number and must not be used as an ownership identifier.
 */
@ExperimentalCryptoOnramp
class OnrampCardDetails internal constructor(
    val funding: CardFunding,
    val brand: CardBrand,
    val last4: String?,
)
