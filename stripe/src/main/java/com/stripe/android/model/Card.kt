package com.stripe.android.model

import androidx.annotation.IntRange
import androidx.annotation.Size
import com.stripe.android.CardUtils
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.parsers.CardJsonParser
import java.util.Calendar
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * A representation of a [Card API object](https://stripe.com/docs/api/cards/object).
 */
@Parcelize
data class Card internal constructor(
    /**
     * the [number] of this card
     */
    val number: String?,

    /**
     * the [cvc] for this card
     */
    val cvc: String?,

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
    val name: String?,

    /**
     * Address line 1 (Street address/PO Box/Company name).
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_line1).
     */
    val addressLine1: String?,

    /**
     * If address_line1 was provided, results of the check: `pass`, `fail`, `unavailable`,
     * or `unchecked`.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_line1_check).
     */
    val addressLine1Check: String?,

    /**
     * Address line 2 (Apartment/Suite/Unit/Building).
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_line2).
     */
    val addressLine2: String?,

    /**
     * City/District/Suburb/Town/Village.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_city).
     */
    val addressCity: String?,

    /**
     * State/County/Province/Region.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_state).
     */
    val addressState: String?,

    /**
     * ZIP or postal code.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_zip).
     */
    val addressZip: String?,

    /**
     * If `address_zip` was provided, results of the check: `pass`, `fail`, `unavailable`,
     * or `unchecked`.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_zip_check).
     */
    val addressZipCheck: String?,

    /**
     * Billing address country, if provided when creating card.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_country).
     */
    val addressCountry: String?,

    /**
     * The last four digits of the card.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-last4).
     */
    @Size(4)
    val last4: String?,

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
    val funding: CardFunding?,

    /**
     * Uniquely identifies this particular card number. You can use this attribute to check whether
     * two customers who’ve signed up with you are using the same card number, for example.
     * For payment methods that tokenize card information (Apple Pay, Google Pay), the tokenized
     * number might be provided instead of the underlying card number.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-fingerprint).
     */
    val fingerprint: String?,

    /**
     * Two-letter ISO code representing the country of the card. You could use this
     * attribute to get a sense of the international breakdown of cards you’ve collected.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-country).
     */
    val country: String?,

    /**
     * Three-letter [ISO code for currency](https://stripe.com/docs/payouts). Only
     * applicable on accounts (not customers or recipients). The card can be used as a transfer
     * destination for funds in this currency.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-currency).
     */
    val currency: String?,

    /**
     * The ID of the customer that this card belongs to.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-customer).
     */
    val customerId: String?,

    /**
     * If a CVC was provided, results of the check: `pass`, `fail`, `unavailable`,
     * or `unchecked`.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-cvc_check).
     */
    val cvcCheck: String?,

    /**
     * Unique identifier for the object.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-id).
     */
    override val id: String?,

    internal val loggingTokens: Set<String> = emptySet(),

    /**
     * If the card number is tokenized, this is the method that was used. See [TokenizationMethod].
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-tokenization_method).
     */
    val tokenizationMethod: TokenizationMethod? = null,

    /**
     * Set of key-value pairs that you can attach to an object. This can be useful fo
     * storing additional information about the object in a structured format.
     *
     * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-metadata).
     */
    val metadata: Map<String, String>?
) : StripeModel, StripePaymentSource, TokenParams(Token.Type.Card, loggingTokens) {

    fun toPaymentMethodsParams(): PaymentMethodCreateParams {
        return PaymentMethodCreateParams.create(
            card = toPaymentMethodParamsCard(),
            billingDetails = PaymentMethod.BillingDetails(
                name = name,
                address = Address(
                    line1 = addressLine1,
                    line2 = addressLine2,
                    city = addressCity,
                    state = addressState,
                    country = addressCountry,
                    postalCode = addressZip
                )
            )
        )
    }

    /**
     * Use [toPaymentMethodsParams] to include Billing Details
     */
    fun toPaymentMethodParamsCard(): PaymentMethodCreateParams.Card {
        return PaymentMethodCreateParams.Card(
            number = number,
            cvc = cvc,
            expiryMonth = expMonth,
            expiryYear = expYear
        )
    }

    /**
     * @return a [Card.Builder] populated with the fields of this [Card] instance
     */
    fun toBuilder(): Builder {
        return Builder(number, expMonth, expYear, cvc)
            .name(name)
            .addressLine1(addressLine1)
            .addressLine1Check(addressLine1Check)
            .addressLine2(addressLine2)
            .addressCity(addressCity)
            .addressState(addressState)
            .addressZip(addressZip)
            .addressZipCheck(addressZipCheck)
            .addressCountry(addressCountry)
            .brand(brand)
            .fingerprint(fingerprint)
            .funding(funding)
            .country(country)
            .currency(currency)
            .customer(customerId)
            .cvcCheck(cvcCheck)
            .last4(last4)
            .id(id)
            .tokenizationMethod(tokenizationMethod)
            .metadata(metadata)
            .loggingTokens(loggingTokens)
    }

    /**
     * Checks whether `this` represents a valid card.
     *
     * @return `true` if valid, `false` otherwise.
     */
    fun validateCard(): Boolean {
        return validateCard(Calendar.getInstance())
    }

    /**
     * Checks whether or not the [number] field is valid.
     *
     * @return `true` if valid, `false` otherwise.
     */
    fun validateNumber(): Boolean {
        return CardUtils.isValidCardNumber(number)
    }

    /**
     * Checks whether or not the [expMonth] and [expYear] fields represent a valid
     * expiry date.
     *
     * @return `true` if valid, `false` otherwise
     */
    fun validateExpiryDate(): Boolean {
        return validateExpiryDate(Calendar.getInstance())
    }

    /**
     * Checks whether or not the [cvc] field is valid.
     *
     * @return `true` if valid, `false` otherwise
     */
    fun validateCVC(): Boolean {
        if (cvc.isNullOrBlank()) {
            return false
        }
        val cvcValue = cvc.trim()
        val validLength = brand.isValidCvc(cvc)

        return ModelUtils.isWholePositiveNumber(cvcValue) && validLength
    }

    /**
     * Checks whether or not the [expMonth] field is valid.
     *
     * @return `true` if valid, `false` otherwise.
     */
    fun validateExpMonth(): Boolean {
        return expMonth?.let { expMonth -> expMonth in 1..12 } == true
    }

    /**
     * Checks whether or not the [expYear] field is valid.
     *
     * @return `true` if valid, `false` otherwise.
     */
    internal fun validateExpYear(now: Calendar): Boolean {
        return expYear?.let { !ModelUtils.hasYearPassed(it, now) } == true
    }

    internal fun validateCard(now: Calendar): Boolean {
        return if (cvc == null) {
            validateNumber() && validateExpiryDate(now)
        } else {
            validateNumber() && validateExpiryDate(now) && validateCVC()
        }
    }

    internal fun validateExpiryDate(now: Calendar): Boolean {
        val expMonth = this.expMonth
        if (expMonth == null || !validateExpMonth()) {
            return false
        }

        return if (expYear == null || !validateExpYear(now)) {
            false
        } else {
            !ModelUtils.hasMonthPassed(expYear, expMonth, now)
        }
    }

    override val typeDataParams: Map<String, Any>
        get() = CardParams(
            number = number.orEmpty(),
            expMonth = expMonth ?: 0,
            expYear = expYear ?: 0,
            cvc = cvc,
            name = name,
            currency = currency,
            address = Address(
                line1 = addressLine1,
                line2 = addressLine2,
                city = addressCity,
                state = addressState,
                postalCode = addressZip,
                country = addressCountry
            )
        ).typeDataParams

    /**
     * Builder class for a [Card] model.
     *
     * Constructor with most common [Card] fields.
     *
     * @param number the credit card number
     * @param expMonth the expiry month, as an integer value between 1 and 12
     * @param expYear the expiry year
     * @param cvc the card CVC number
     */
    class Builder(
        internal val number: String? = null,
        @param:IntRange(from = 1, to = 12) internal val expMonth: Int? = null,
        @param:IntRange(from = 0) internal val expYear: Int? = null,
        internal val cvc: String? = null
    ) : ObjectBuilder<Card> {
        private var name: String? = null
        private var addressLine1: String? = null
        private var addressLine1Check: String? = null
        private var addressLine2: String? = null
        private var addressCity: String? = null
        private var addressState: String? = null
        private var addressZip: String? = null
        private var addressZipCheck: String? = null
        private var addressCountry: String? = null
        private var brand: CardBrand? = null
        private var funding: CardFunding? = null
        @Size(4)
        private var last4: String? = null
        private var fingerprint: String? = null
        private var country: String? = null
        private var currency: String? = null
        private var customerId: String? = null
        private var cvcCheck: String? = null
        private var id: String? = null
        private var tokenizationMethod: TokenizationMethod? = null
        private var metadata: Map<String, String>? = null
        private var loggingTokens: Set<String>? = null

        fun name(name: String?): Builder = apply {
            this.name = name
        }

        fun addressLine1(address: String?): Builder = apply {
            this.addressLine1 = address
        }

        fun addressLine1Check(addressLine1Check: String?): Builder = apply {
            this.addressLine1Check = addressLine1Check
        }

        fun addressLine2(address: String?): Builder = apply {
            this.addressLine2 = address
        }

        fun addressCity(city: String?): Builder = apply {
            this.addressCity = city
        }

        fun addressState(state: String?): Builder = apply {
            this.addressState = state
        }

        fun addressZip(zip: String?): Builder = apply {
            this.addressZip = zip
        }

        fun addressZipCheck(zipCheck: String?): Builder = apply {
            this.addressZipCheck = zipCheck
        }

        fun addressCountry(country: String?): Builder = apply {
            this.addressCountry = country
        }

        fun brand(brand: CardBrand?): Builder = apply {
            this.brand = brand
        }

        fun fingerprint(fingerprint: String?): Builder = apply {
            this.fingerprint = fingerprint
            return this
        }

        fun funding(funding: CardFunding?): Builder = apply {
            this.funding = funding
        }

        fun country(country: String?): Builder = apply {
            this.country = country
        }

        fun currency(currency: String?): Builder = apply {
            this.currency = currency
        }

        fun customer(customerId: String?): Builder = apply {
            this.customerId = customerId
        }

        fun cvcCheck(cvcCheck: String?): Builder = apply {
            this.cvcCheck = cvcCheck
        }

        fun last4(last4: String?): Builder = apply {
            this.last4 = last4
        }

        fun id(id: String?): Builder = apply {
            this.id = id
        }

        fun tokenizationMethod(tokenizationMethod: TokenizationMethod?): Builder = apply {
            this.tokenizationMethod = tokenizationMethod
        }

        fun metadata(metadata: Map<String, String>?): Builder = apply {
            this.metadata = metadata
        }

        fun loggingTokens(loggingTokens: Set<String>): Builder = apply {
            this.loggingTokens = loggingTokens
        }

        /**
         * Generate a new [Card] object based on the arguments held by this Builder.
         *
         * @return the newly created [Card] object
         */
        override fun build(): Card {
            val number = normalizeCardNumber(number).takeUnless { it.isNullOrBlank() }
            val last4 = last4.takeUnless { it.isNullOrBlank() } ?: calculateLast4(number)
            return Card(
                number = number,
                expMonth = expMonth,
                expYear = expYear,
                cvc = cvc.takeUnless { it.isNullOrBlank() },
                name = name.takeUnless { it.isNullOrBlank() },
                addressLine1 = addressLine1.takeUnless { it.isNullOrBlank() },
                addressLine1Check = addressLine1Check.takeUnless { it.isNullOrBlank() },
                addressLine2 = addressLine2.takeUnless { it.isNullOrBlank() },
                addressCity = addressCity.takeUnless { it.isNullOrBlank() },
                addressState = addressState.takeUnless { it.isNullOrBlank() },
                addressZip = addressZip.takeUnless { it.isNullOrBlank() },
                addressZipCheck = addressZipCheck.takeUnless { it.isNullOrBlank() },
                addressCountry = addressCountry.takeUnless { it.isNullOrBlank() },
                last4 = last4,
                brand = brand ?: CardUtils.getPossibleCardBrand(number),
                fingerprint = fingerprint.takeUnless { it.isNullOrBlank() },
                funding = funding,
                country = country.takeUnless { it.isNullOrBlank() },
                currency = currency.takeUnless { it.isNullOrBlank() },
                customerId = customerId.takeUnless { it.isNullOrBlank() },
                cvcCheck = cvcCheck.takeUnless { it.isNullOrBlank() },
                id = id.takeUnless { it.isNullOrBlank() },
                tokenizationMethod = tokenizationMethod,
                metadata = metadata,
                loggingTokens = loggingTokens.orEmpty()
            )
        }

        private fun normalizeCardNumber(number: String?): String? {
            return number?.trim()?.replace("\\s+|-".toRegex(), "")
        }

        private fun calculateLast4(number: String?): String? {
            return if (number != null && number.length > 4) {
                number.substring(number.length - 4)
            } else {
                null
            }
        }
    }

    companion object {
        /**
         * Create a Card object from a raw JSON string.
         *
         * @param jsonString the JSON string representing the potential Card
         * @return A Card if one can be made from the JSON, or `null` if one cannot be made
         * or the JSON is invalid.
         */
        @JvmStatic
        fun fromString(jsonString: String): Card? {
            return runCatching {
                JSONObject(jsonString)
            }.getOrNull()?.let {
                fromJson(it)
            }
        }

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): Card? {
            return jsonObject?.let {
                CardJsonParser().parse(it)
            }
        }

        /**
         * Convenience constructor for a Card object with a minimum number of inputs.
         *
         * @param number the card number
         * @param expMonth the expiry month
         * @param expYear the expiry year
         * @param cvc the CVC code
         */
        @JvmStatic
        fun create(
            number: String? = null,
            expMonth: Int? = null,
            expYear: Int? = null,
            cvc: String? = null
        ): Card {
            return Builder(number, expMonth, expYear, cvc)
                .build()
        }
    }
}
