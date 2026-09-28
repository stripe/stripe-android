package com.stripe.android.common.nfcscan.scanner.apdu.pdol

internal object BcdEncoding {
    /**
     * Converts the receiving [Long] integer into a **packed Binary-Coded Decimal (BCD) byte array**.
     *
     * In packed BCD, two decimal digits (0-99) are stored per byte: one digit in the upper nibble
     * and one in the lower nibble. The conversion is performed from right to left (least significant digits first).
     *
     * @param length The required length of the resulting BCD byte array. Must be sufficient
     * to hold the number (e.g., length 3 can hold up to 6 digits, or 999,999).
     * @return A [ByteArray] of size [length] containing the packed BCD representation.
     */
    fun encode(value: Long, length: Int): ByteArray {
        val bcdArray = ByteArray(length)

        var num = value
        for (i in length - 1 downTo 0) {
            // Get the last two digits (0-99).
            val pair = num % TWO_DIGIT_DIVISOR

            val tens = pair / FIRST_DIGIT_DIVISOR
            val ones = pair % FIRST_DIGIT_DIVISOR

            // Pack: (tens digit << 4) OR (ones digit).
            bcdArray[i] = (tens shl NIBBLE_SHIFT or ones).toByte()

            // Move to the next pair of digits.
            num /= TWO_DIGIT_DIVISOR
        }

        return bcdArray
    }

    private const val TWO_DIGIT_DIVISOR = 100
    private const val NIBBLE_SHIFT = 4
    private const val FIRST_DIGIT_DIVISOR = 10
}
