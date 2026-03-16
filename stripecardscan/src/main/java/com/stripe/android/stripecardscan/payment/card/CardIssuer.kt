package com.stripe.android.stripecardscan.payment.card

import androidx.annotation.RestrictTo

/**
 * A list of supported card issuers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CardIssuer(open val displayName: String) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object AmericanExpress : CardIssuer("American Express")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Custom(override val displayName: String) : CardIssuer(displayName)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object DinersClub : CardIssuer("Diners Club")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Discover : CardIssuer("Discover")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object JCB : CardIssuer("JCB")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object MasterCard : CardIssuer("MasterCard")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object UnionPay : CardIssuer("UnionPay")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Unknown : CardIssuer("Unknown")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Visa : CardIssuer("Visa")
}
