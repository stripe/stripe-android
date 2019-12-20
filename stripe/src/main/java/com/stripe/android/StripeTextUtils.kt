package com.stripe.android

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale

/**
 * Utility class for common text-related operations on Stripe data coming from the server.
 */
object StripeTextUtils {

    /**
     * Converts a card number that may have spaces between the numbers into one without any spaces.
     * Note: method does not check that all characters are digits or spaces.
     *
     * @param cardNumberWithSpaces a card number, for instance "4242 4242 4242 4242"
     * @return the input number minus any spaces, for instance "4242424242424242".
     * Returns `null` if the input was `null` or all spaces.
     */
    @JvmStatic
    fun removeSpacesAndHyphens(cardNumberWithSpaces: String?): String? {
        return cardNumberWithSpaces.takeUnless { it.isNullOrBlank() }
            ?.replace("\\s|-".toRegex(), "")
    }

    /**
     * Calculate a hash value of a String input and convert the result to a hex string.
     *
     * @param toHash a value to hash
     * @return a hexadecimal string
     */
    internal fun shaHashInput(toHash: String?): String? {
        if (toHash.isNullOrBlank()) {
            return null
        }

        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            val bytes = toHash.toByteArray(StandardCharsets.UTF_8)
            digest.update(bytes, 0, bytes.size)
            bytesToHex(digest.digest())
        } catch (noSuchAlgorithm: NoSuchAlgorithmException) {
            null
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes
            .joinToString("") { String.format(Locale.ROOT, "%02x", it) }
            .toUpperCase(Locale.ROOT)
    }
}
