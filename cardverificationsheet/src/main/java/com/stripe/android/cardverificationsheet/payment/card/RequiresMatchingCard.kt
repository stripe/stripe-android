package com.stripe.android.cardverificationsheet.payment.card

/**
 * An interface for a class that requires a card to match. This provides the methods used to
 * determine if a given card matches or does not match the required card.
 */
internal interface RequiresMatchingCard {
    val requiredCardIssuer: CardIssuer?
    val requiredLastFour: String?

    /**
     * Returns whether the card matches the [requiredCardIssuer] and/or [requiredLastFour], or if
     * there is no required card.
     */
    fun compareToRequiredCard(pan: String?): CardMatch {
        /*
         * TODO: Use contracts once they're supported. [CardMatch.Match], [CardMatch.Mismatch], and
         * [CardMatch.NoRequiredCard] guarantees that pan != null
         */
        if (pan.isNullOrEmpty()) return CardMatch.NoPan
        if (requiredLastFour == null && requiredCardIssuer == null) return CardMatch.NoRequiredCard

        val lastFourMatches = requiredLastFour != null && pan.lastFour() == requiredLastFour
        val cardIssuerMatches =
            requiredCardIssuer != null && getCardIssuer(pan) == requiredCardIssuer

        return when {
            lastFourMatches && cardIssuerMatches -> CardMatch.Match
            else -> CardMatch.Mismatch
        }
    }
}

sealed interface CardMatch {
    object NoRequiredCard: CardMatch
    object Match : CardMatch
    object Mismatch : CardMatch
    object NoPan: CardMatch
}
