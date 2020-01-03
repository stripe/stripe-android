package com.stripe.android.model

import androidx.annotation.IntRange
import androidx.annotation.Size
import androidx.annotation.StringDef
import com.stripe.android.CardUtils
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.parsers.CardJsonParser
import java.util.Calendar
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * A representation of a [Card API object](https://stripe.com/docs/api/cards/object).
 */
@Parcelize
data class Card internal constructor(

    /**
     * @return the [number] of this card
     */
    val number: String?,

    /**
     * @return the [cvc] for this card
     */
    val cvc: String?,

    /**
     * @return Two-digit number representing the card’s expiration month.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-exp_month)
     */
    @get:IntRange(from = 1, to = 12)
    val expMonth: Int?,

    /**
     * @return Four-digit number representing the card’s expiration year.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-exp_year)
     */
    val expYear: Int?,

    /**
     * @return Cardholder name.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-name)
     */
    val name: String?,

    /**
     * @return Address line 1 (Street address/PO Box/Company name).
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_line1)
     */
    val addressLine1: String?,

    /**
     * @return If address_line1 was provided, results of the check: `pass`, `fail`, `unavailable`,
     * or `unchecked`.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_line1_check)
     */
    val addressLine1Check: String?,

    /**
     * @return Address line 2 (Apartment/Suite/Unit/Building).
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_line2)
     */
    val addressLine2: String?,

    /**
     * @return City/District/Suburb/Town/Village.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_city)
     */
    val addressCity: String?,

    /**
     * @return State/County/Province/Region.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_state)
     */
    val addressState: String?,

    /**
     * @return ZIP or postal code.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_zip)
     */
    val addressZip: String?,

    /**
     * @return If `address_zip` was provided, results of the check: `pass`, `fail`, `unavailable`,
     * or `unchecked`.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_zip_check)
     */
    val addressZipCheck: String?,

    /**
     * @return Billing address country, if provided when creating card.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-address_country)
     */
    val addressCountry: String?,

    /**
     * @return The last four digits of the card.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-last4)
     */
    @Size(4)
    val last4: String?,

    /**
     * @return Card brand. Can be `"American Express"`, `"Diners Club"`, `"Discover"`, `"JCB"`,
     * `"MasterCard"`, `"UnionPay"`, `"Visa"`, or `"Unknown"`.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-brand)
     */
    val brand: CardBrand,

    /**
     * @return Card funding type. Can be `credit`, `debit`, `prepaid`, or `unknown`.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-funding)
     */
    @FundingType
    @get:FundingType
    val funding: String?,

    /**
     * @return Uniquely identifies this particular card number. You can use this attribute to
     * check whether two customers who’ve signed up with you are using the same card number,
     * for example.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-fingerprint)
     */
    val fingerprint: String?,

    /**
     * @return Two-letter ISO code representing the country of the card. You could use this
     * attribute to get a sense of the international breakdown of cards you’ve collected.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-country)
     */
    val country: String?,

    /**
     * @return Three-letter [ISO code for currency](https://stripe.com/docs/payouts). Only
     * applicable on accounts (not customers or recipients). The card can be used as a transfer
     * destination for funds in this currency.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-currency)
     */
    val currency: String?,

    /**
     * @return The ID of the customer that this card belongs to.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-customer)
     */
    val customerId: String?,

    /**
     * @return If a CVC was provided, results of the check: `pass`, `fail`, `unavailable`,
     * or `unchecked`.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-cvc_check)
     */
    val cvcCheck: String?,

    /**
     * @return Unique identifier for the object.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-id)
     */
    override val id: String?,

    internal val loggingTokens: MutableList<String> = mutableListOf(),

    /**
     * @return If the card number is tokenized, this is the method that was used.
     * Can be `apple_pay` or `google_pay`.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-tokenization_method)
     */
    val tokenizationMethod: TokenizationMethod? = null,

    /**
     * @return Set of key-value pairs that you can attach to an object. This can be useful fo
     * storing additional information about the object in a structured format.
     *
     * [API Reference](https://stripe.com/docs/api/cards/object#card_object-metadata)
     */
    val metadata: Map<String, String>?
) : StripeModel, StripePaymentSource, StripeParamsModel {

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(FundingType.CREDIT, FundingType.DEBIT, FundingType.PREPAID, FundingType.UNKNOWN)
    annotation class FundingType {
        companion object {
            const val CREDIT: String = "credit"
            const val DEBIT: String = "debit"
            const val PREPAID: String = "prepaid"
            const val UNKNOWN: String = "unknown"
        }
    }

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

    override fun toParamMap(): Map<String, Any> {
        return mapOf(Token.TokenType.CARD to createCardParams())
    }

    private fun createCardParams(): Map<String, Any?> {
        return mapOf(
            "number" to number.takeUnless { it.isNullOrBlank() },
            "cvc" to cvc.takeUnless { it.isNullOrBlank() },
            "exp_month" to expMonth,
            "exp_year" to expYear,
            "name" to name.takeUnless { it.isNullOrBlank() },
            "currency" to currency.takeUnless { it.isNullOrBlank() },
            "address_line1" to addressLine1.takeUnless { it.isNullOrBlank() },
            "address_line2" to addressLine2.takeUnless { it.isNullOrBlank() },
            "address_city" to addressCity.takeUnless { it.isNullOrBlank() },
            "address_zip" to addressZip.takeUnless { it.isNullOrBlank() },
            "address_state" to addressState.takeUnless { it.isNullOrBlank() },
            "address_country" to addressCountry.takeUnless { it.isNullOrBlank() }
        )
    }

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
        @FundingType
        private var funding: String? = null
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
        private var loggingTokens: List<String>? = null

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

        fun funding(@FundingType funding: String?): Builder = apply {
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

        fun loggingTokens(loggingTokens: List<String>): Builder = apply {
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
                brand = brand ?: CardUtils.getPossibleCardType(number),
                fingerprint = fingerprint.takeUnless { it.isNullOrBlank() },
                funding = asFundingType(funding).takeUnless { it.isNullOrBlank() },
                country = country.takeUnless { it.isNullOrBlank() },
                currency = currency.takeUnless { it.isNullOrBlank() },
                customerId = customerId.takeUnless { it.isNullOrBlank() },
                cvcCheck = cvcCheck.takeUnless { it.isNullOrBlank() },
                id = id.takeUnless { it.isNullOrBlank() },
                tokenizationMethod = tokenizationMethod,
                metadata = metadata,
                loggingTokens = loggingTokens.orEmpty().toMutableList()
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
        const val CVC_LENGTH_AMERICAN_EXPRESS: Int = 4
        const val CVC_LENGTH_COMMON: Int = 3

        /**
         * Based on [Issuer identification number table](http://en.wikipedia.org/wiki/Bank_card_number#Issuer_identification_number_.28IIN.29)
         */
        @Deprecated("Use CardBrand.AmericanExpress.prefixes", ReplaceWith("CardBrand.AmericanExpress.prefixes"))
        val PREFIXES_AMERICAN_EXPRESS: Array<String> = CardBrand.AmericanExpress.prefixes.toTypedArray()
        @Deprecated("Use CardBrand.Discover.prefixes", ReplaceWith("CardBrand.Discover.prefixes"))
        val PREFIXES_DISCOVER: Array<String> = CardBrand.Discover.prefixes.toTypedArray()
        @Deprecated("Use CardBrand.JCB.prefixes", ReplaceWith("CardBrand.JCB.prefixes"))
        val PREFIXES_JCB: Array<String> = CardBrand.JCB.prefixes.toTypedArray()
        @Deprecated("Use CardBrand.DinersClub.prefixes", ReplaceWith("CardBrand.DinersClub.prefixes"))
        val PREFIXES_DINERS_CLUB: Array<String> = CardBrand.DinersClub.prefixes.toTypedArray()
        @Deprecated("Use CardBrand.Visa.prefixes", ReplaceWith("CardBrand.Visa.prefixes"))
        val PREFIXES_VISA: Array<String> = CardBrand.Visa.prefixes.toTypedArray()
        @Deprecated("Use CardBrand.MasterCard.prefixes", ReplaceWith("CardBrand.MasterCard.prefixes"))
        val PREFIXES_MASTERCARD: Array<String> = CardBrand.MasterCard.prefixes.toTypedArray()
        @Deprecated("Use CardBrand.UnionPay.prefixes", ReplaceWith("CardBrand.AmericanExpress.prefixes"))
        val PREFIXES_UNIONPAY: Array<String> = CardBrand.UnionPay.prefixes.toTypedArray()

        const val MAX_LENGTH_STANDARD: Int = 16
        const val MAX_LENGTH_AMERICAN_EXPRESS: Int = 15
        const val MAX_LENGTH_DINERS_CLUB: Int = 14

        internal const val OBJECT_TYPE = "card"

        /**
         * Converts an unchecked String value to a [FundingType] or `null`.
         *
         * @param possibleFundingType a String that might match a [FundingType] or be empty
         * @return `null` if the input is blank, else the appropriate [FundingType]
         */
        @JvmStatic
        @FundingType
        fun asFundingType(possibleFundingType: String?): String? {
            if (possibleFundingType.isNullOrBlank()) {
                return null
            }

            return when {
                FundingType.CREDIT.equals(possibleFundingType, ignoreCase = true) ->
                    FundingType.CREDIT
                FundingType.DEBIT.equals(possibleFundingType, ignoreCase = true) ->
                    FundingType.DEBIT
                FundingType.PREPAID.equals(possibleFundingType, ignoreCase = true) ->
                    FundingType.PREPAID
                else -> FundingType.UNKNOWN
            }
        }

        /**
         * Create a Card object from a raw JSON string.
         *
         * @param jsonString the JSON string representing the potential Card
         * @return A Card if one can be made from the JSON, or `null` if one cannot be made
         * or the JSON is invalid.
         */
        @JvmStatic
        fun fromString(jsonString: String): Card? {
            return try {
                val jsonObject = JSONObject(jsonString)
                fromJson(jsonObject)
            } catch (ignored: JSONException) {
                null
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
