package com.stripe.android.elements

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Options to block certain card brands on the client
 */
sealed class CardBrandAcceptance : Parcelable {

    /**
     * Card brand categories that can be allowed or disallowed
     */
    @Parcelize
    enum class BrandCategory : Parcelable {
        /**
         * Visa branded cards
         */
        Visa,

        /**
         * Mastercard branded cards
         */
        Mastercard,

        /**
         * Amex branded cards
         */
        Amex,

        /**
         * Discover branded cards
         * **Note**: Encompasses all of Discover Global Network (Discover, Diners, JCB, UnionPay, Elo).
         */
        Discover
    }

    companion object {
        /**
         * Accept all card brands supported by Stripe
         */
        @JvmStatic
        fun all(): CardBrandAcceptance = All

        /**
         * Accept only the card brands specified in `brands`.
         * **Note**: Any card brands that do not map to a `BrandCategory` will be blocked when using an allow list.
         */
        @JvmStatic
        fun allowed(brands: List<BrandCategory>): CardBrandAcceptance =
            Allowed(brands)

        /**
         * Accept all card brands supported by Stripe except for those specified in `brands`.
         * **Note**: Any card brands that do not map to a `BrandCategory` will be accepted
         * when using a disallow list.
         */
        @JvmStatic
        fun disallowed(brands: List<BrandCategory>): CardBrandAcceptance =
            Disallowed(brands)
    }

    @Parcelize
    internal data object All : CardBrandAcceptance()

    @Parcelize
    internal data class Allowed(
        val brands: List<BrandCategory>
    ) : CardBrandAcceptance()

    @Parcelize
    internal data class Disallowed(
        val brands: List<BrandCategory>
    ) : CardBrandAcceptance()
}
