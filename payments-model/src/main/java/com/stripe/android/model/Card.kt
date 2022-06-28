package com.stripe.android.model

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.Size
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * A representation of a [Card API object](https://stripe.com/docs/api/cards/object).
 */
@Parcelize
data class Card
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    /**
     * Two-digit number representing the card’s expiration month.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-exp_month).
     */
    @get:IntRange(from = 1, to = 12)
    val expMonth: Int?,

    /**
     * Four-digit number representing the card’s expiration year.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-exp_year).
     */
    val expYear: Int?,

    /**
     * Cardholder name.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-name).
     */
    val name: String? = null,

    /**
     * Address line 1 (Street address/PO Box/Company name).
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_line1).
     */
    val addressLine1: String? = null,

    /**
     * If address_line1 was provided, results of the check: `pass`, `fail`, `unavailable`,
     * or `unchecked`.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_line1_check).
     */
    val addressLine1Check: String? = null,

    /**
     * Address line 2 (Apartment/Suite/Unit/Building).
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_line2).
     */
    val addressLine2: String? = null,

    /**
     * City/District/Suburb/Town/Village.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_city).
     */
    val addressCity: String? = null,

    /**
     * State/County/Province/Region.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_state).
     */
    val addressState: String? = null,

    /**
     * ZIP or postal code.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_zip).
     */
    val addressZip: String? = null,

    /**
     * If `address_zip` was provided, results of the check: `pass`, `fail`, `unavailable`,
     * or `unchecked`.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_zip_check).
     */
    val addressZipCheck: String? = null,

    /**
     * Billing address country, if provided when creating card.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_country).
     */
    val addressCountry: String? = null,

    /**
     * The last four digits of the card.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-last4).
     */
    @Size(4)
    val last4: String? = null,

    /**
     * Card brand. See [CardBrand].
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-brand).
     */
    val brand: CardBrand,

    /**
     * Card funding type. See [CardFunding].
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-funding).
     */
    val funding: CardFunding? = null,

    /**
     * Uniquely identifies this particular card number. You can use this attribute to check whether
     * two customers who’ve signed up with you are using the same card number, for example.
     * For payment methods that tokenize card information (Apple Pay, Google Pay), the tokenized
     * number might be provided instead of the underlying card number.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-fingerprint).
     */
    val fingerprint: String? = null,

    /**
     * Two-letter ISO code representing the country of the card. You could use this
     * attribute to get a sense of the international breakdown of cards you’ve collected.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-country).
     */
    val country: String? = null,

    /**
     * Three-letter [ISO code for currency](https://stripe.com/docs/payouts). Only
     * applicable on accounts (not customers or recipients). The card can be used as a transfer
     * destination for funds in this currency.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-currency).
     */
    val currency: String? = null,

    /**
     * The ID of the customer that this card belongs to.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-customer).
     */
    val customerId: String? = null,

    /**
     * If a CVC was provided, results of the check: `pass`, `fail`, `unavailable`,
     * or `unchecked`.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-cvc_check).
     */
    val cvcCheck: String? = null,

    /**
     * Unique identifier for the object.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-id).
     */
    override val id: String?,

    /**
     * If the card number is tokenized, this is the method that was used. See [TokenizationMethod].
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-tokenization_method).
     */
    val tokenizationMethod: TokenizationMethod? = null
) : StripeModel, StripePaymentSource {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * See https://stripe.com/docs/api/cards/object#card_object-brand for valid values.
         */
        @JvmSynthetic
        fun getCardBrand(brandName: String?): CardBrand {
            return when (brandName) {
                "American Express" -> CardBrand.AmericanExpress
                "Diners Club" -> CardBrand.DinersClub
                "Discover" -> CardBrand.Discover
                "JCB" -> CardBrand.JCB
                "MasterCard" -> CardBrand.MasterCard
                "UnionPay" -> CardBrand.UnionPay
                "Visa" -> CardBrand.Visa
                else -> CardBrand.Unknown
            }
        }
    }
}
