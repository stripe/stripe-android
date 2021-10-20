package com.stripe.android.cardverificationsheet.payment.card

/**
 * An interface for a class that requires a card to match. This provides the methods used to determine if a given card
 * matches or does not match the required card.
 */
internal interface RequiresMatchingCard {
    val requiredIin: String?
    val requiredLastFour: String?

    /**
     * Returns true if the card matches the [requiredIin] and/or [requiredLastFour], or if the two fields are null.
     *
     * TODO: use contracts when they are no longer experimental
     */
    fun matchesRequiredCard(pan: String?): Boolean {
        // contract { returns(true) implies (pan != null) }
        return pan != null && isValidPan(pan) && panMatches(requiredIin, requiredLastFour, pan)
    }

    /**
     * Returns true if the required card fields are non-null and the given pan does not match.
     *
     * TODO: use contracts when they are no longer experimental
     */
    fun doesNotMatchRequiredCard(pan: String?): Boolean {
        // contract { returns(true) implies (pan != null) }
        return pan != null && isValidPan(pan) && !panMatches(requiredIin, requiredLastFour, pan)
    }
}
