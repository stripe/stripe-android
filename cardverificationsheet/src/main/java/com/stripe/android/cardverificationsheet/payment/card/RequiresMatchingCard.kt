package com.stripe.android.cardverificationsheet.payment.card

/**
 * An interface for a class that requires a card to match. This provides the methods used to
 * determine if a given card matches or does not match the required card.
 */
internal interface RequiresMatchingCard {
    val requiredCardIssuer: CardIssuer?
    val requiredLastFour: String

    /**
     * Returns true if the card matches the [requiredCardIssuer] and/or [requiredLastFour], or if
     * the two fields are null.
     *
     * TODO: Use contracts once they're supported. True guarantees that lastFour != null
     */
    fun matchesRequiredCard(cardIssuer: CardIssuer?, lastFour: String?): Boolean =
        lastFour == requiredLastFour.lastFour() &&
            (requiredCardIssuer == null || cardIssuer == requiredCardIssuer)

    /**
     * Returns true if the card does not match the [requiredCardIssuer] and [requiredLastFour].
     *
     * TODO: Use contracts once they're supported. True guarantees that lastFour != null
     */
    fun doesNotMatchRequiredCard(cardIssuer: CardIssuer?, lastFour: String?): Boolean =
        (lastFour != null && lastFour != requiredLastFour) ||
            (cardIssuer != null && requiredCardIssuer != null && cardIssuer != requiredCardIssuer)
}
