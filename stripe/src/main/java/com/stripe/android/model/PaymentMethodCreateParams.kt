package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.ObjectBuilder
import com.stripe.android.Stripe
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

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
    private val sofort: Sofort? = null,
    private val upi: Upi? = null,

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

    private constructor(
        sofort: Sofort,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = Type.Sofort,
        sofort = sofort,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        upi: Upi,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = Type.Upi,
        upi = upi,
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
        ).plus(typeParams).plus(
            metadata?.let {
                mapOf(PARAM_METADATA to it)
            }.orEmpty()
        )
    }

    private val typeParams: Map<String, Any>
        get() {
            return when (type) {
                Type.Card -> card?.toParamMap()
                Type.Ideal -> ideal?.toParamMap()
                Type.Fpx -> fpx?.toParamMap()
                Type.SepaDebit -> sepaDebit?.toParamMap()
                Type.AuBecsDebit -> auBecsDebit?.toParamMap()
                Type.BacsDebit -> bacsDebit?.toParamMap()
                Type.Sofort -> sofort?.toParamMap()
                Type.Upi -> upi?.toParamMap()
                else -> null
            }.takeUnless { it.isNullOrEmpty() }?.let {
                mapOf(type.code to it)
            }.orEmpty()
        }

    internal enum class Type(internal val code: String, val hasMandate: Boolean = false) {
        Card("card"),
        Ideal("ideal"),
        Fpx("fpx"),
        SepaDebit("sepa_debit", true),
        AuBecsDebit("au_becs_debit", true),
        BacsDebit("bacs_debit", true),
        Sofort("sofort"),
        P24("p24"),
        Bancontact("bancontact"),
        Giropay("giropay"),
        Eps("eps"),
        Oxxo("oxxo"),
        Alipay("alipay"),
        GrabPay("grabpay"),
        PayPal("paypal"),
        AfterpayClearpay("afterpay_clearpay"),
        Upi("upi"),
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
    data class Upi(
        private val vpa: String?
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return vpa?.let {
                mapOf(PARAM_VPA to it)
            }.orEmpty()
        }

        class Builder : ObjectBuilder<Upi> {
            internal var vpa: String? = null

            fun setVpa(vpa: String?): Builder = apply {
                this.vpa = vpa
            }

            override fun build(): Upi {
                return Upi(vpa)
            }
        }

        private companion object {
            private const val PARAM_VPA: String = "vpa"
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

    @Parcelize
    data class Sofort(
        internal var country: String
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return mapOf(
                PARAM_COUNTRY to country.toUpperCase(Locale.ROOT)
            )
        }

        private companion object {
            private const val PARAM_COUNTRY = "country"
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
        fun createCard(
            cardParams: CardParams
        ): PaymentMethodCreateParams {
            return create(
                card = Card(
                    number = cardParams.number,
                    expiryMonth = cardParams.expMonth,
                    expiryYear = cardParams.expYear,
                    cvc = cardParams.cvc,
                    attribution = cardParams.attribution
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = cardParams.name,
                    address = cardParams.address
                ),
                metadata = cardParams.metadata
            )
        }

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
         * @return params for creating a [PaymentMethod.Type.BacsDebit] payment method
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
         * @return params for creating a [PaymentMethod.Type.Sofort] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            sofort: Sofort,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(sofort, billingDetails, metadata)
        }

        @JvmSynthetic
        @JvmOverloads
        fun create(
            upi: Upi,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(upi, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.P24] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun createP24(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = Type.P24,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Bancontact] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun createBancontact(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = Type.Bancontact,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Giropay] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun createGiropay(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = Type.Giropay,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        /**
         * @return params for creating a [PaymentMethod.Type.GrabPay] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun createGrabPay(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = Type.GrabPay,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Eps] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun createEps(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = Type.Eps,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        @JvmSynthetic
        @JvmOverloads
        fun createOxxo(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = Type.Oxxo,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        @JvmSynthetic
        @JvmOverloads
        fun createAlipay(
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = Type.Alipay,
                metadata = metadata
            )
        }

        @JvmSynthetic
        @JvmOverloads
        fun createPayPal(
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = Type.PayPal,
                metadata = metadata
            )
        }

        @JvmSynthetic
        @JvmOverloads
        internal fun createAfterpayClearpay(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = Type.AfterpayClearpay,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        /**
         * @param googlePayPaymentData a [JSONObject] derived from Google Pay's
         * [PaymentData#toJson()](https://developers.google.com/pay/api/android/reference/client#tojson)
         */
        @Throws(JSONException::class)
        @JvmStatic
        fun createFromGooglePay(googlePayPaymentData: JSONObject): PaymentMethodCreateParams {
            val googlePayResult = GooglePayResult.fromJson(googlePayPaymentData)
            val token = googlePayResult.token
            val tokenId = token?.id.orEmpty()

            return create(
                Card(
                    token = tokenId,
                    attribution = setOfNotNull(token?.card?.tokenizationMethod?.toString())
                ),
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
