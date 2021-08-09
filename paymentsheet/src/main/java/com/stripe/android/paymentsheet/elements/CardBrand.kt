package com.stripe.android.paymentsheet.elements

/**
 * A representation of supported card brands and related data
 */
enum class CardBrand {
    Unknown;

    // TODO: Need to fill this in with real values
    val maxCvcLength: Int = 3

    // TODO: Need to fill this in with real values
    fun getMaxLengthForCardNumber(number: String): Int = 16

    companion object {
        fun fromText(text: String) = Unknown
    }

}
