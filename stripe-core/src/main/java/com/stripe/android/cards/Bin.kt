package com.stripe.android.cards

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class Bin internal constructor(
    internal val value: String
) : StripeModel {
    override fun toString() = value

    companion object {
        fun create(cardNumber: String): Bin? {
            return cardNumber
                .take(BIN_LENGTH)
                .takeIf {
                    it.length == BIN_LENGTH
                }?.let {
                    Bin(it)
                }
        }

        private const val BIN_LENGTH = 6
    }
}
