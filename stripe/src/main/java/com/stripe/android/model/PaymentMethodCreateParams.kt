package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.ObjectBuilder
import com.stripe.android.Stripe
import kotlinx.android.parcel.Parcelize
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
@Parcelize
data class PaymentMethodCreateParams internal constructor(
    internal val type: Type,
    private val card: Card? = null,
    private val ideal: Ideal? = null,
    private val fpx: Fpx? = null,
    private val sepaDebit: SepaDebit? = null,
    private val auBecsDebit: AuBecsDebit? = null,
    private val bacsDebit: BacsDebit? = null,
    private val billingDetails: PaymentMethod.BillingDetails? = null,
    private val metadata: Map<String, String>? = null,
    private val productUsage: Set<String> = emptySet()
) : StripeParamsModel, Parcelable {

    val typeCode: String
        get() = type.code

    internal val attribution: Set<String>?
        @JvmSynthetic
        get() {
            return when (type) {
                Type.Card -> card?.attribution?.plus(productUsage)
                else -> productUsage.takeIf { it.isNotEmpty() }
            }
        }

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

    private constructor(
        auBecsDebit: AuBecsDebit,
        billingDetails: PaymentMethod.BillingDetails,
        metadata: Map<String, String>?
    ) : this(
        type = Type.AuBecsDebit,
        auBecsDebit = auBecsDebit,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        bacsDebit: BacsDebit,
        billingDetails: PaymentMethod.BillingDetails,
        metadata: Map<String, String>?
    ) : this(
        type = Type.BacsDebit,
        bacsDebit = bacsDebit,
        billingDetails = billingDetails,
        metadata = metadata
    )

    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_TYPE to type.code
        ).plus(
            billingDetails?.let {
                mapOf(PARAM_BILLING_DETAILS to it.toParamMap())
            }.orEmpty()
        ).plus(
            mapOf(type.code to
                when (type) {
                    Type.Card -> card?.toParamMap().orEmpty()
                    Type.Ideal -> ideal?.toParamMap().orEmpty()
                    Type.Fpx -> fpx?.toParamMap().orEmpty()
                    Type.SepaDebit -> sepaDebit?.toParamMap().orEmpty()
                    Type.AuBecsDebit -> auBecsDebit?.toParamMap().orEmpty()
                    Type.BacsDebit -> bacsDebit?.toParamMap().orEmpty()
                }
            )
        ).plus(
            metadata?.let {
                mapOf(PARAM_METADATA to it)
            }.orEmpty()
        )
    }

    internal enum class Type(internal val code: String, val hasMandate: Boolean = false) {
        Card("card"),
        Ideal("ideal"),
        Fpx("fpx"),
        SepaDebit("sepa_debit", true),
        AuBecsDebit("au_becs_debit", true),
        BacsDebit("bacs_debit", true)
    }

    @Parcelize
    data class Card internal constructor(
        private val number: String? = null,
        private val expiryMonth: Int? = null,
        private val expiryYear: Int? = null,
        private val cvc: String? = null,
        private val token: String? = null,

        internal val attribution: Set<String>? = null
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return listOf(
                PARAM_NUMBER to number,
                PARAM_EXP_MONTH to expiryMonth,
                PARAM_EXP_YEAR to expiryYear,
                PARAM_CVC to cvc,
                PARAM_TOKEN to token
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

            fun setNumber(number: String?): Builder = apply {
                this.number = number
            }

            fun setExpiryMonth(expiryMonth: Int?): Builder = apply {
                this.expiryMonth = expiryMonth
            }

            fun setExpiryYear(expiryYear: Int?): Builder = apply {
                this.expiryYear = expiryYear
            }

            fun setCvc(cvc: String?): Builder = apply {
                this.cvc = cvc
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
            private const val PARAM_NUMBER: String = "number"
            private const val PARAM_EXP_MONTH: String = "exp_month"
            private const val PARAM_EXP_YEAR: String = "exp_year"
            private const val PARAM_CVC: String = "cvc"
            private const val PARAM_TOKEN: String = "token"

            /**
             * Create a [Card] from a Card token.
             */
            @JvmStatic
            fun create(token: String): Card {
                return Card(token = token, number = null)
            }
        }
    }

    @Parcelize
    data class Ideal(
        private val bank: String?
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return bank?.let { mapOf(PARAM_BANK to it) }.orEmpty()
        }

        class Builder : ObjectBuilder<Ideal> {
            internal var bank: String? = null

            fun setBank(bank: String?): Builder = apply {
                this.bank = bank
            }

            override fun build(): Ideal {
                return Ideal(bank)
            }
        }

        private companion object {
            private const val PARAM_BANK: String = "bank"
        }
    }

    @Parcelize
    data class Fpx(
        private val bank: String?
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return bank?.let {
                mapOf(PARAM_BANK to it)
            }.orEmpty()
        }

        class Builder : ObjectBuilder<Fpx> {
            internal var bank: String? = null

            fun setBank(bank: String?): Builder = apply {
                this.bank = bank
            }

            override fun build(): Fpx {
                return Fpx(bank)
            }
        }

        private companion object {
            private const val PARAM_BANK: String = "bank"
        }
    }

    @Parcelize
    data class SepaDebit(
        private val iban: String?
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return iban?.let {
                mapOf(PARAM_IBAN to it)
            }.orEmpty()
        }

        class Builder : ObjectBuilder<SepaDebit> {
            private var iban: String? = null

            fun setIban(iban: String?): Builder = apply {
                this.iban = iban
            }

            override fun build(): SepaDebit {
                return SepaDebit(iban)
            }
        }

        private companion object {
            private const val PARAM_IBAN: String = "iban"
        }
    }

    @Parcelize
    data class AuBecsDebit(
        var bsbNumber: String,
        var accountNumber: String
    ) : StripeParamsModel, Parcelable {

        override fun toParamMap(): Map<String, Any> {
            return mapOf(
                PARAM_BSB_NUMBER to bsbNumber,
                PARAM_ACCOUNT_NUMBER to accountNumber
            )
        }

        private companion object {
            private const val PARAM_BSB_NUMBER: String = "bsb_number"
            private const val PARAM_ACCOUNT_NUMBER: String = "account_number"
        }
    }

    /**
     * BACS bank account details
     *
     * See [https://stripe.com/docs/api/payment_methods/create#create_payment_method-bacs_debit](https://stripe.com/docs/api/payment_methods/create#create_payment_method-bacs_debit)
     */
    @Parcelize
    data class BacsDebit(
        /**
         * The bank account number (e.g. 00012345)
         */
        var accountNumber: String,

        /**
         * The sort code of the bank account (e.g. 10-88-00)
         */
        var sortCode: String
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return mapOf(
                PARAM_ACCOUNT_NUMBER to accountNumber,
                PARAM_SORT_CODE to sortCode
            )
        }

        private companion object {
            private const val PARAM_ACCOUNT_NUMBER: String = "account_number"
            private const val PARAM_SORT_CODE: String = "sort_code"
        }
    }

    companion object {
        private const val PARAM_TYPE = "type"
        private const val PARAM_BILLING_DETAILS = "billing_details"
        private const val PARAM_METADATA = "metadata"

        /**
         * @return params for creating a [PaymentMethod.Type.Card] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            card: Card,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(card, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Ideal] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            ideal: Ideal,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(ideal, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Fpx] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            fpx: Fpx,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(fpx, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.SepaDebit] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            sepaDebit: SepaDebit,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(sepaDebit, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.AuBecsDebit] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            auBecsDebit: AuBecsDebit,
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(auBecsDebit, billingDetails, metadata)
        }

        /**
         * @return params for creating a `PaymentMethod.Type.BacsDebit` payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            bacsDebit: BacsDebit,
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(bacsDebit, billingDetails, metadata)
        }

        /**
         * @param googlePayPaymentData a [JSONObject] derived from Google Pay's
         * [PaymentData#toJson()](https://developers.google.com/pay/api/android/reference/client#tojson)
         */
        @Throws(JSONException::class)
        @JvmStatic
        fun createFromGooglePay(googlePayPaymentData: JSONObject): PaymentMethodCreateParams {
            val googlePayResult = GooglePayResult.fromJson(googlePayPaymentData)
            val tokenId = requireNotNull(googlePayResult.token?.id)

            return create(
                Card.create(tokenId),
                PaymentMethod.BillingDetails(
                    address = googlePayResult.address,
                    name = googlePayResult.name,
                    email = googlePayResult.email,
                    phone = googlePayResult.phoneNumber
                )
            )
        }
    }
}
