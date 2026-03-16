package com.stripe.android.stripecardscan.payment.card

/**
 * A class that provides a method to determine if a PAN is valid.
 */
interface PanValidator {
    fun isValidPan(pan: String): Boolean

    operator fun plus(other: PanValidator): PanValidator = CompositePanValidator(this, other)
}

/**
 * A [PanValidator] comprised of two separate validators.
 */
private class CompositePanValidator(
    private val validator1: PanValidator,
    private val validator2: PanValidator
) : PanValidator {
    override fun isValidPan(pan: String): Boolean =
        validator1.isValidPan(pan) && validator2.isValidPan(pan)
}

/**
 * A [PanValidator] that ensures the PAN is of a valid length.
 */
object LengthPanValidator : PanValidator {
    override fun isValidPan(pan: String): Boolean {
        val iinData = getIssuerData(pan) ?: return false
        return pan.length in iinData.panLengths
    }
}

/**
 * A [PanValidator] that performs the Luhn check for validation.
 *
 * see https://en.wikipedia.org/wiki/Luhn_algorithm
 */
object LuhnPanValidator : PanValidator {
    override fun isValidPan(pan: String): Boolean {
        if (pan.isEmpty()) {
            return false
        }

        fun doubleDigit(digit: Int) = if (digit * 2 > 9) digit * 2 - 9 else digit * 2

        var sum = pan.takeLast(1).toInt()
        val parity = pan.length % 2

        for (i in 0 until pan.length - 1) {
            sum += if (i % 2 == parity) {
                doubleDigit(Character.getNumericValue(pan[i]))
            } else {
                Character.getNumericValue(pan[i])
            }
        }

        return sum % 10 == 0
    }
}
