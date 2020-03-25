package com.stripe.android

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
}
