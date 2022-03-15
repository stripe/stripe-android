package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.CardUtils
import com.stripe.android.ObjectBuilder
import com.stripe.android.Stripe
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for PaymentMethod creation parameters.
 *
 * Used by [Stripe.createPaymentMethod] and [Stripe.createPaymentMethodSynchronous].
 *
 * See [Create a PaymentMethod](https://stripe.com/docs/api/payment_methods/create).
 *
 * See [PaymentMethod] for API object.
 */
@Parcelize
data class PaymentMethodCreateParams internal constructor(
    internal val type: PaymentMethod.Type,
    val card: Card? = null,
    private val ideal: Ideal? = null,
    private val fpx: Fpx? = null,
    private val sepaDebit: SepaDebit? = null,
    private val auBecsDebit: AuBecsDebit? = null,
    private val bacsDebit: BacsDebit? = null,
    private val sofort: Sofort? = null,
    private val upi: Upi? = null,
    private val netbanking: Netbanking? = null,
    val billingDetails: PaymentMethod.BillingDetails? = null,
    private val metadata: Map<String, String>? = null,
    private val productUsage: Set<String> = emptySet(),

    /**
     * If provided, will be used as the representation of this object when calling the Stripe API,
     * instead of generating the map from its content.
     *
     * The map should be valid according to the
     * [PaymentMethod creation API](https://stripe.com/docs/api/payment_methods/create)
     * documentation, including a required `type` entry.
     *
     * The values of the map must be any of the types supported by [android.os.Parcel.writeValue].
     */
    private val overrideParamMap: Map<String, @RawValue Any>? = null,
) : StripeParamsModel, Parcelable {

    val typeCode: String
        get() = type.code

    internal val attribution: Set<String>
        @JvmSynthetic
        get() {
            return when (type) {
                PaymentMethod.Type.Card -> (card?.attribution ?: emptySet()).plus(productUsage)
                else -> productUsage
            }
        }

    private constructor(
        card: Card,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Card,
        card = card,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        ideal: Ideal,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Ideal,
        ideal = ideal,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        fpx: Fpx,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Fpx,
        fpx = fpx,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        sepaDebit: SepaDebit,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.SepaDebit,
        sepaDebit = sepaDebit,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        auBecsDebit: AuBecsDebit,
        billingDetails: PaymentMethod.BillingDetails,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.AuBecsDebit,
        auBecsDebit = auBecsDebit,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        bacsDebit: BacsDebit,
        billingDetails: PaymentMethod.BillingDetails,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.BacsDebit,
        bacsDebit = bacsDebit,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        sofort: Sofort,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Sofort,
        sofort = sofort,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        upi: Upi,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Upi,
        upi = upi,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        netbanking: Netbanking,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Netbanking,
        netbanking = netbanking,
        billingDetails = billingDetails,
        metadata = metadata
    )

    override fun toParamMap(): Map<String, Any> {
        return overrideParamMap
            ?: mapOf(
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
                PaymentMethod.Type.Card -> card?.toParamMap()
                PaymentMethod.Type.Ideal -> ideal?.toParamMap()
                PaymentMethod.Type.Fpx -> fpx?.toParamMap()
                PaymentMethod.Type.SepaDebit -> sepaDebit?.toParamMap()
                PaymentMethod.Type.AuBecsDebit -> auBecsDebit?.toParamMap()
                PaymentMethod.Type.BacsDebit -> bacsDebit?.toParamMap()
                PaymentMethod.Type.Sofort -> sofort?.toParamMap()
                PaymentMethod.Type.Upi -> upi?.toParamMap()
                PaymentMethod.Type.Netbanking -> netbanking?.toParamMap()
                else -> null
            }.takeUnless { it.isNullOrEmpty() }?.let {
                mapOf(type.code to it)
            }.orEmpty()
        }

    @Parcelize
    data class Card
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        internal val number: String? = null,
        internal val expiryMonth: Int? = null,
        internal val expiryYear: Int? = null,
        internal val cvc: String? = null,
        private val token: String? = null,
        internal val attribution: Set<String>? = null
    ) : StripeParamsModel, Parcelable {
        internal val brand: CardBrand get() = CardUtils.getPossibleCardBrand(number)
        internal val last4: String? get() = number?.takeLast(4)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
        fun getLast4() = last4

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
        var bank: String?
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return bank?.let { mapOf(PARAM_BANK to it) }.orEmpty()
        }

        private companion object {
            private const val PARAM_BANK: String = "bank"
        }
    }

    @Parcelize
    data class Fpx(
        var bank: String?
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return bank?.let {
                mapOf(PARAM_BANK to it)
            }.orEmpty()
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

        private companion object {
            private const val PARAM_VPA: String = "vpa"
        }
    }

    @Parcelize
    data class SepaDebit(
        var iban: String?
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return iban?.let {
                mapOf(PARAM_IBAN to it)
            }.orEmpty()
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
                PARAM_COUNTRY to country.uppercase()
            )
        }

        private companion object {
            private const val PARAM_COUNTRY = "country"
        }
    }

    @Parcelize
    data class Netbanking(
        internal var bank: String
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return mapOf(
                PARAM_BANK to bank.lowercase()
            )
        }

        private companion object {
            private const val PARAM_BANK = "bank"
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

        @JvmStatic
        @JvmOverloads
        fun create(
            upi: Upi,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(upi, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Netbanking] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            netbanking: Netbanking,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(netbanking, billingDetails, metadata)
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
                type = PaymentMethod.Type.P24,
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
                type = PaymentMethod.Type.Bancontact,
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
                type = PaymentMethod.Type.Giropay,
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
                type = PaymentMethod.Type.GrabPay,
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
                type = PaymentMethod.Type.Eps,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createOxxo(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Oxxo,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createAlipay(
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Alipay,
                metadata = metadata
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createPayPal(
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.PayPal,
                metadata = metadata
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createAfterpayClearpay(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.AfterpayClearpay,
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

        @JvmStatic
        @JvmOverloads
        fun createBlik(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Blik,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createWeChatPay(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.WeChatPay,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createKlarna(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Klarna,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createAffirm(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Affirm,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        @JvmStatic
        @JvmOverloads
        internal fun createUsBankAccount(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.USBankAccount,
                billingDetails = billingDetails,
                metadata = metadata
            )
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
        fun createWithOverride(
            type: PaymentMethod.Type,
            overrideParamMap: Map<String, @RawValue Any>?,
            productUsage: Set<String>
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = type,
                overrideParamMap = overrideParamMap,
                productUsage = productUsage
            )
        }
    }
}
