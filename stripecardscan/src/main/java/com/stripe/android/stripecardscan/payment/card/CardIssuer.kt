package com.stripe.android.stripecardscan.payment.card

/**
 * A list of supported card issuers.
 */
sealed class CardIssuer(open val displayName: String) {
    data object AmericanExpress : CardIssuer("American Express")
    data class Custom(override val displayName: String) : CardIssuer(displayName)
    data object DinersClub : CardIssuer("Diners Club")
    data object Discover : CardIssuer("Discover")
    data object JCB : CardIssuer("JCB")
    data object MasterCard : CardIssuer("MasterCard")
    data object UnionPay : CardIssuer("UnionPay")
    data object Unknown : CardIssuer("Unknown")
    data object Visa : CardIssuer("Visa")
}
