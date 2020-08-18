package com.stripe.android.cards

internal data class Bin internal constructor(
    internal val value: String
) {
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
