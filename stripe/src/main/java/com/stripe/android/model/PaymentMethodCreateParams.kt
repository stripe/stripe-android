package com.stripe.android.model

import com.stripe.android.ObjectBuilder
import com.stripe.android.Stripe
import java.util.Objects
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for PaymentMethod creation parameters.
 *
 * Used by [Stripe.createPaymentMethodSynchronous]
 *
 * See [Create a PaymentMethod](https://stripe.com/docs/api/payment_methods/create).
 *
 * See [PaymentMethod] for API object.
 */
class PaymentMethodCreateParams private constructor(
    internal val type: Type,
    private val card: Card? = null,
    private val ideal: Ideal? = null,
    private val fpx: Fpx? = null,
    private val sepaDebit: SepaDebit? = null,
    private val billingDetails: PaymentMethod.BillingDetails? = null,
    private val metadata: Map<String, String>? = null
) : StripeParamsModel {

    val typeCode: String
        get() = type.code

    private constructor(
        card: Card,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = Type.Card,
        card = card,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        ideal: Ideal,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = Type.Ideal,
        ideal = ideal,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        fpx: Fpx,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = Type.Fpx,
        fpx = fpx,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        sepaDebit: SepaDebit,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = Type.SepaDebit,
        sepaDebit = sepaDebit,
        billingDetails = billingDetails,
        metadata = metadata
    )

    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            FIELD_TYPE to type.code
        ).plus(
            billingDetails?.let {
                mapOf(FIELD_BILLING_DETAILS to it.toParamMap())
            }.orEmpty()
        ).plus(
            when (type) {
                Type.Card -> {
                    mapOf(FIELD_CARD to card?.toParamMap().orEmpty())
                }
                Type.Ideal -> {
                    mapOf(FIELD_IDEAL to ideal?.toParamMap().orEmpty())
                }
                Type.Fpx -> {
                    mapOf(FIELD_FPX to fpx?.toParamMap().orEmpty())
                }
                Type.SepaDebit -> {
                    mapOf(FIELD_SEPA_DEBIT to sepaDebit?.toParamMap().orEmpty())
                }
            }
        ).plus(
            metadata?.let {
                mapOf(FIELD_METADATA to it)
            }.orEmpty()
        )
    }

    override fun hashCode(): Int {
        return Objects.hash(type, card, fpx, ideal, billingDetails, metadata)
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is PaymentMethodCreateParams -> typedEquals(other)
            else -> false
        }
    }

    private fun typedEquals(params: PaymentMethodCreateParams): Boolean {
        return (type == params.type &&
            card == params.card &&
            fpx == params.fpx &&
            ideal == params.ideal &&
            billingDetails == params.billingDetails &&
            metadata == params.metadata)
    }

    internal enum class Type(internal val code: String, val hasMandate: Boolean = false) {
        Card("card"),
        Ideal("ideal"),
        Fpx("fpx"),
        SepaDebit("sepa_debit", true)
    }

    class Card private constructor(
        private val number: String? = null,
        private val expiryMonth: Int? = null,
        private val expiryYear: Int? = null,
        private val cvc: String? = null,
        private val token: String? = null
    ) : StripeParamsModel {
        override fun hashCode(): Int {
            return Objects.hash(number, expiryMonth, expiryYear, cvc, token)
        }

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other is Card -> typedEquals(other)
                else -> false
            }
        }

        private fun typedEquals(card: Card): Boolean {
            return number == card.number &&
                cvc == card.cvc &&
                expiryMonth == card.expiryMonth &&
                expiryYear == card.expiryYear &&
                token == card.token
        }

        override fun toParamMap(): Map<String, Any> {
            return listOf(
                FIELD_NUMBER to number,
                FIELD_EXP_MONTH to expiryMonth,
                FIELD_EXP_YEAR to expiryYear,
                FIELD_CVC to cvc,
                FIELD_TOKEN to token
            ).mapNotNull {
                it.second?.let { value ->
                    it.first to value
                }
            }.toMap()
        }

        /**
         * Used to create a [Card] object with the user's card details. To create a
         * [Card] with a Stripe token (e.g. for Google Pay), use [Card.create].
         */
        class Builder : ObjectBuilder<Card> {
            private var number: String? = null
            private var expiryMonth: Int? = null
            private var expiryYear: Int? = null
            private var cvc: String? = null

            fun setNumber(number: String?): Builder {
                this.number = number
                return this
            }

            fun setExpiryMonth(expiryMonth: Int?): Builder {
                this.expiryMonth = expiryMonth
                return this
            }

            fun setExpiryYear(expiryYear: Int?): Builder {
                this.expiryYear = expiryYear
                return this
            }

            fun setCvc(cvc: String?): Builder {
                this.cvc = cvc
                return this
            }

            override fun build(): Card {
                return Card(
                    number = number,
                    expiryMonth = expiryMonth,
                    expiryYear = expiryYear,
                    cvc = cvc
                )
            }
        }

        companion object {
            private const val FIELD_NUMBER: String = "number"
            private const val FIELD_EXP_MONTH: String = "exp_month"
            private const val FIELD_EXP_YEAR: String = "exp_year"
            private const val FIELD_CVC: String = "cvc"
            private const val FIELD_TOKEN: String = "token"

            @JvmStatic
            fun create(token: String): Card {
                return Card(token = token, number = null)
            }
        }
    }

    class Ideal private constructor(private val bank: String?) : StripeParamsModel {
        override fun toParamMap(): Map<String, Any> {
            return bank?.let { mapOf(FIELD_BANK to it) }.orEmpty()
        }

        override fun hashCode(): Int {
            return Objects.hash(bank)
        }

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other is Ideal -> typedEquals(other)
                else -> false
            }
        }

        private fun typedEquals(ideal: Ideal): Boolean {
            return bank == ideal.bank
        }

        class Builder : ObjectBuilder<Ideal> {
            internal var bank: String? = null

            fun setBank(bank: String?): Builder {
                this.bank = bank
                return this
            }

            override fun build(): Ideal {
                return Ideal(bank)
            }
        }

        companion object {
            private const val FIELD_BANK: String = "bank"
        }
    }

    class Fpx private constructor(private val bank: String?) : StripeParamsModel {
        override fun toParamMap(): Map<String, Any> {
            return bank?.let {
                mapOf(FIELD_BANK to it)
            }.orEmpty()
        }

        override fun hashCode(): Int {
            return Objects.hash(bank)
        }

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other is Fpx -> typedEquals(other)
                else -> false
            }
        }

        private fun typedEquals(fpx: Fpx): Boolean {
            return bank == fpx.bank
        }

        class Builder : ObjectBuilder<Fpx> {
            internal var bank: String? = null

            fun setBank(bank: String?): Builder {
                this.bank = bank
                return this
            }

            override fun build(): Fpx {
                return Fpx(bank)
            }
        }

        companion object {
            private const val FIELD_BANK: String = "bank"
        }
    }

    // TODO(mshafrir-stripe): make public
    internal class SepaDebit private constructor(private val iban: String?) : StripeParamsModel {
        override fun toParamMap(): Map<String, Any> {
            return iban?.let {
                mapOf(FIELD_IBAN to it)
            }.orEmpty()
        }

        override fun hashCode(): Int {
            return Objects.hash(iban)
        }

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other is SepaDebit -> typedEquals(other)
                else -> false
            }
        }

        private fun typedEquals(sepaDebit: SepaDebit): Boolean {
            return iban == sepaDebit.iban
        }

        class Builder : ObjectBuilder<SepaDebit> {
            private var iban: String? = null

            fun setIban(iban: String?): Builder {
                this.iban = iban
                return this
            }

            override fun build(): SepaDebit {
                return SepaDebit(iban)
            }
        }

        companion object {
            private const val FIELD_IBAN: String = "iban"
        }
    }

    companion object {
        private const val FIELD_TYPE = "type"
        private const val FIELD_CARD = "card"
        private const val FIELD_FPX = "fpx"
        private const val FIELD_IDEAL = "ideal"
        private const val FIELD_SEPA_DEBIT = "sepa_debit"

        private const val FIELD_BILLING_DETAILS = "billing_details"
        private const val FIELD_METADATA = "metadata"

        @JvmStatic
        @JvmOverloads
        fun create(
            card: Card,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(card, billingDetails, metadata)
        }

        @JvmStatic
        @JvmOverloads
        fun create(
            ideal: Ideal,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(ideal, billingDetails, metadata)
        }

        @JvmStatic
        @JvmOverloads
        fun create(
            fpx: Fpx,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(fpx, billingDetails, metadata)
        }

        // TODO(mshafrir-stripe): make public
        @JvmStatic
        @JvmOverloads
        internal fun create(
            sepaDebit: SepaDebit,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(sepaDebit, billingDetails, metadata)
        }

        /**
         * @param googlePayPaymentData a [JSONObject] derived from Google Pay's
         * [PaymentData#toJson()](https://developers.google.com/pay/api/android/reference/client#tojson)
         */
        @Throws(JSONException::class)
        @JvmStatic
        fun createFromGooglePay(googlePayPaymentData: JSONObject): PaymentMethodCreateParams {
            val paymentMethodData = googlePayPaymentData
                .getJSONObject("paymentMethodData")
            val googlePayBillingAddress = paymentMethodData
                .getJSONObject("info")
                .optJSONObject("billingAddress")
            val paymentToken = paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("token")
            val stripeToken = Token.fromJson(JSONObject(paymentToken))
            val stripeTokenId = requireNotNull(stripeToken).id

            val email = googlePayPaymentData.optString("email")
            val billingDetails = if (googlePayBillingAddress != null) {
                PaymentMethod.BillingDetails.Builder()
                    .setAddress(Address.Builder()
                        .setLine1(googlePayBillingAddress.optString("address1"))
                        .setLine2(googlePayBillingAddress.optString("address2"))
                        .setCity(googlePayBillingAddress.optString("locality"))
                        .setState(googlePayBillingAddress.optString("administrativeArea"))
                        .setCountry(googlePayBillingAddress.optString("countryCode"))
                        .setPostalCode(googlePayBillingAddress.optString("postalCode"))
                        .build())
                    .setName(googlePayBillingAddress.optString("name"))
                    .setEmail(email)
                    .setPhone(googlePayBillingAddress.optString("phoneNumber"))
                    .build()
            } else {
                PaymentMethod.BillingDetails.Builder()
                    .setEmail(email)
                    .build()
            }

            return create(
                Card.create(stripeTokenId),
                billingDetails
            )
        }
    }
}
