package com.stripe.android.stripecardscan.payment.card

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
    fun compareToRequiredCard(pan: String?): CardMatchResult {
        /*
         * TODO: Use contracts once they're supported. [CardMatch.Match], [CardMatch.Mismatch], and
         * [CardMatch.NoRequiredCard] guarantees that pan != null
         */
        if (pan.isNullOrEmpty()) return CardMatchResult.NoPan
        if (requiredLastFour == null && requiredCardIssuer == null) {
            return CardMatchResult.NoRequiredCard
        }

        val lastFourMatches = requiredLastFour != null && pan.lastFour() == requiredLastFour
        val cardIssuerMatches =
            requiredCardIssuer != null && getCardIssuer(pan) == requiredCardIssuer

        return when {
            lastFourMatches && cardIssuerMatches -> CardMatchResult.Match
            else -> CardMatchResult.Mismatch
        }
    }
}

internal sealed interface CardMatchResult {
    object NoRequiredCard : CardMatchResult
    object Match : CardMatchResult
    object Mismatch : CardMatchResult
    object NoPan : CardMatchResult
}
