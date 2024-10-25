package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.Stripe
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.json.JSONException
import org.json.JSONObject
import java.util.Objects

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
data class PaymentMethodCreateParams
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    internal val code: PaymentMethodCode,
    internal val requiresMandate: Boolean,
    val card: Card? = null,
    private val ideal: Ideal? = null,
    private val fpx: Fpx? = null,
    private val sepaDebit: SepaDebit? = null,
    private val auBecsDebit: AuBecsDebit? = null,
    private val bacsDebit: BacsDebit? = null,
    private val sofort: Sofort? = null,
    private val upi: Upi? = null,
    private val netbanking: Netbanking? = null,
    private val usBankAccount: USBankAccount? = null,
    private val link: Link? = null,
    private val cashAppPay: CashAppPay? = null,
    private val swish: Swish? = null,
    val billingDetails: PaymentMethod.BillingDetails? = null,
    private val allowRedisplay: PaymentMethod.AllowRedisplay? = null,
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
    private val overrideParamMap: Map<String, @RawValue Any>? = null
) : StripeParamsModel, Parcelable {

    internal constructor(
        type: PaymentMethod.Type,
        card: Card? = null,
        ideal: Ideal? = null,
        fpx: Fpx? = null,
        sepaDebit: SepaDebit? = null,
        auBecsDebit: AuBecsDebit? = null,
        bacsDebit: BacsDebit? = null,
        sofort: Sofort? = null,
        upi: Upi? = null,
        netbanking: Netbanking? = null,
        usBankAccount: USBankAccount? = null,
        link: Link? = null,
        cashAppPay: CashAppPay? = null,
        swish: Swish? = null,
        billingDetails: PaymentMethod.BillingDetails? = null,
        allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        metadata: Map<String, String>? = null,
        productUsage: Set<String> = emptySet(),
        overrideParamMap: Map<String, @RawValue Any>? = null
    ) : this(
        type.code,
        type.requiresMandate,
        card,
        ideal,
        fpx,
        sepaDebit,
        auBecsDebit,
        bacsDebit,
        sofort,
        upi,
        netbanking,
        usBankAccount,
        link,
        cashAppPay,
        swish,
        billingDetails,
        allowRedisplay,
        metadata,
        productUsage,
        overrideParamMap
    )

    val typeCode: String
        get() = code

    val attribution: Set<String>
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmSynthetic
        get() {
            return when (code) {
                PaymentMethod.Type.Card.code -> (card?.attribution ?: emptySet()).plus(productUsage)
                else -> productUsage
            }
        }

    private constructor(
        card: Card,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Card,
        card = card,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        ideal: Ideal,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Ideal,
        ideal = ideal,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        fpx: Fpx,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Fpx,
        fpx = fpx,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        sepaDebit: SepaDebit,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.SepaDebit,
        sepaDebit = sepaDebit,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        auBecsDebit: AuBecsDebit,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.AuBecsDebit,
        auBecsDebit = auBecsDebit,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        bacsDebit: BacsDebit,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.BacsDebit,
        bacsDebit = bacsDebit,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        sofort: Sofort,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Sofort,
        sofort = sofort,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        upi: Upi,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Upi,
        upi = upi,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        netbanking: Netbanking,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.Netbanking,
        netbanking = netbanking,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        usBankAccount: USBankAccount,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?
    ) : this(
        type = PaymentMethod.Type.USBankAccount,
        usBankAccount = usBankAccount,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata
    )

    private constructor(
        cashAppPay: CashAppPay,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?,
    ) : this(
        type = PaymentMethod.Type.CashAppPay,
        cashAppPay = cashAppPay,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata,
    )

    private constructor(
        swish: Swish,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        billingDetails: PaymentMethod.BillingDetails?,
        metadata: Map<String, String>?,
    ) : this(
        type = PaymentMethod.Type.Swish,
        swish = swish,
        allowRedisplay = allowRedisplay,
        billingDetails = billingDetails,
        metadata = metadata,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun requiresMandate(): Boolean {
        return requiresMandate
    }

    override fun toParamMap(): Map<String, Any> {
        val params = overrideParamMap
            ?: mapOf(
                PARAM_TYPE to code
            ).plus(
                billingDetails?.let {
                    mapOf(PARAM_BILLING_DETAILS to it.toParamMap())
                }.orEmpty()
            ).plus(typeParams).plus(
                metadata?.let {
                    mapOf(PARAM_METADATA to it)
                }.orEmpty()
            )

        return params.plus(
            allowRedisplay?.let {
                mapOf(PARAM_ALLOW_REDISPLAY to allowRedisplay.value)
            }.orEmpty()
        )
    }

    private val typeParams: Map<String, Any>
        get() {
            return when (code) {
                PaymentMethod.Type.Card.code -> card?.toParamMap()
                PaymentMethod.Type.Ideal.code -> ideal?.toParamMap()
                PaymentMethod.Type.Fpx.code -> fpx?.toParamMap()
                PaymentMethod.Type.SepaDebit.code -> sepaDebit?.toParamMap()
                PaymentMethod.Type.AuBecsDebit.code -> auBecsDebit?.toParamMap()
                PaymentMethod.Type.BacsDebit.code -> bacsDebit?.toParamMap()
                PaymentMethod.Type.Sofort.code -> sofort?.toParamMap()
                PaymentMethod.Type.Upi.code -> upi?.toParamMap()
                PaymentMethod.Type.Netbanking.code -> netbanking?.toParamMap()
                PaymentMethod.Type.USBankAccount.code -> usBankAccount?.toParamMap()
                PaymentMethod.Type.Link.code -> link?.toParamMap()
                else -> null
            }.takeUnless { it.isNullOrEmpty() }?.let {
                mapOf(code to it)
            }.orEmpty()
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun cardLast4(): String? {
        return ((toParamMap()["card"] as? Map<*, *>?)?.get("number") as? String)?.takeLast(4)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun linkBankPaymentMethodId(): String? {
        val linkParams = (toParamMap()["link"] as? Map<*, *>) ?: return null
        return linkParams["payment_method_id"] as? String
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
        internal val attribution: Set<String>? = null,
        internal val networks: Networks? = null,
    ) : StripeParamsModel, Parcelable {

        @Parcelize
        class Networks(
            val preferred: String? = null,
        ) : StripeParamsModel, Parcelable {

            override fun toParamMap(): Map<String, Any> {
                return if (preferred != null) {
                    mapOf(PARAM_PREFERRED to preferred)
                } else {
                    emptyMap()
                }
            }

            override fun equals(other: Any?): Boolean {
                return other is Networks && other.preferred == preferred
            }

            override fun hashCode(): Int {
                return Objects.hash(preferred)
            }

            override fun toString(): String {
                return "PaymentMethodCreateParams.Card.Networks(preferred=$preferred)"
            }

            private companion object {
                const val PARAM_PREFERRED = "preferred"
            }
        }

        override fun toParamMap(): Map<String, Any> {
            return listOf(
                PARAM_NUMBER to number,
                PARAM_EXP_MONTH to expiryMonth,
                PARAM_EXP_YEAR to expiryYear,
                PARAM_CVC to cvc,
                PARAM_TOKEN to token,
                PARAM_NETWORKS to networks?.toParamMap(),
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
        class Builder {
            private var number: String? = null
            private var expiryMonth: Int? = null
            private var expiryYear: Int? = null
            private var cvc: String? = null
            private var networks: Networks? = null

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

            fun setNetworks(networks: Networks?): Builder = apply {
                this.networks = networks
            }

            fun build(): Card {
                return Card(
                    number = number,
                    expiryMonth = expiryMonth,
                    expiryYear = expiryYear,
                    cvc = cvc,
                    networks = networks,
                )
            }
        }

        companion object {
            private const val PARAM_NUMBER: String = "number"
            private const val PARAM_EXP_MONTH: String = "exp_month"
            private const val PARAM_EXP_YEAR: String = "exp_year"
            private const val PARAM_CVC: String = "cvc"
            private const val PARAM_TOKEN: String = "token"
            private const val PARAM_NETWORKS: String = "networks"

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

        internal companion object {
            private const val PARAM_ACCOUNT_NUMBER: String = "account_number"
            private const val PARAM_SORT_CODE: String = "sort_code"

            internal fun fromParams(params: PaymentMethodCreateParams): BacsDebit? {
                val code = PaymentMethod.Type.BacsDebit.code

                val bacsParams = params.toParamMap()[code] as? Map<*, *>

                val accountNumber = bacsParams?.get(
                    PARAM_ACCOUNT_NUMBER
                ) as? String

                val sortCode = bacsParams?.get(
                    PARAM_SORT_CODE
                ) as? String

                return when {
                    accountNumber != null && sortCode != null -> BacsDebit(
                        accountNumber = accountNumber,
                        sortCode = sortCode
                    )
                    else -> null
                }
            }
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

    /**
     * Encapsulates parameters used to create [PaymentMethodCreateParams] when using Cash App Pay.
     */
    @Parcelize
    class CashAppPay : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> = emptyMap()
    }

    /**
     * Encapsulates parameters used to create [PaymentMethodCreateParams] when using Swish.
     */
    @Parcelize
    class Swish : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> = emptyMap()
    }

    @Parcelize
    @Suppress("DataClassPrivateConstructor")
    data class USBankAccount private constructor(
        internal var linkAccountSessionId: String? = null,
        internal var accountNumber: String? = null,
        internal var routingNumber: String? = null,
        internal var accountType: PaymentMethod.USBankAccount.USBankAccountType? = null,
        internal var accountHolderType: PaymentMethod.USBankAccount.USBankAccountHolderType? = null
    ) : StripeParamsModel, Parcelable {
        constructor(
            linkAccountSessionId: String
        ) : this(
            linkAccountSessionId,
            null,
            null,
            null,
            null
        )

        constructor(
            accountNumber: String,
            routingNumber: String,
            accountType: PaymentMethod.USBankAccount.USBankAccountType,
            accountHolderType: PaymentMethod.USBankAccount.USBankAccountHolderType
        ) : this(
            linkAccountSessionId = null,
            accountNumber = accountNumber,
            routingNumber = routingNumber,
            accountType = accountType,
            accountHolderType = accountHolderType
        )

        override fun toParamMap(): Map<String, Any> {
            return if (linkAccountSessionId != null) {
                mapOf(
                    PARAM_LINKED_ACCOUNT_SESSION_ID to linkAccountSessionId!!
                )
            } else {
                mapOf(
                    PARAM_ACCOUNT_NUMBER to accountNumber!!,
                    PARAM_ROUTING_NUMBER to routingNumber!!,
                    PARAM_ACCOUNT_TYPE to accountType!!.value,
                    PARAM_ACCOUNT_HOLDER_TYPE to accountHolderType!!.value
                )
            }
        }

        private companion object {
            private const val PARAM_LINKED_ACCOUNT_SESSION_ID = "link_account_session"
            private const val PARAM_ACCOUNT_NUMBER = "account_number"
            private const val PARAM_ROUTING_NUMBER = "routing_number"
            private const val PARAM_ACCOUNT_TYPE = "account_type"
            private const val PARAM_ACCOUNT_HOLDER_TYPE = "account_holder_type"
        }
    }

    @Parcelize
    data class Link(
        internal var paymentDetailsId: String,
        internal var consumerSessionClientSecret: String,
        internal var extraParams: Map<String, @RawValue Any>? = null
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return mapOf(
                PARAM_PAYMENT_DETAILS_ID to paymentDetailsId,
                PARAM_CREDENTIALS to mapOf(
                    PARAM_CONSUMER_SESSION_CLIENT_SECRET to consumerSessionClientSecret
                )
            ).plus(
                extraParams ?: emptyMap()
            )
        }

        private companion object {
            private const val PARAM_PAYMENT_DETAILS_ID = "payment_details_id"
            private const val PARAM_CREDENTIALS = "credentials"
            private const val PARAM_CONSUMER_SESSION_CLIENT_SECRET =
                "consumer_session_client_secret"
        }
    }

    companion object {
        private const val PARAM_TYPE = "type"
        private const val PARAM_BILLING_DETAILS = "billing_details"
        private const val PARAM_ALLOW_REDISPLAY = "allow_redisplay"
        private const val PARAM_METADATA = "metadata"

        /**
         * @return params for creating a [PaymentMethod.Type.Card] payment method
         */
        @JvmStatic
        fun createCard(
            cardParams: CardParams
        ): PaymentMethodCreateParams {
            @OptIn(DelicateCardDetailsApi::class)
            return create(
                card = Card(
                    number = cardParams.number,
                    expiryMonth = cardParams.expMonth,
                    expiryYear = cardParams.expYear,
                    cvc = cardParams.cvc,
                    attribution = cardParams.attribution,
                    networks = cardParams.networks?.preferred?.let {
                        Card.Networks(
                            preferred = it
                        )
                    }
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
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(card, allowRedisplay, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Ideal] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            ideal: Ideal,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(ideal, allowRedisplay, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Fpx] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            fpx: Fpx,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(fpx, allowRedisplay, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.SepaDebit] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            sepaDebit: SepaDebit,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(sepaDebit, allowRedisplay, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.AuBecsDebit] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            auBecsDebit: AuBecsDebit,
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(auBecsDebit, allowRedisplay, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.BacsDebit] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            bacsDebit: BacsDebit,
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(bacsDebit, allowRedisplay, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Sofort] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            sofort: Sofort,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(sofort, allowRedisplay, billingDetails, metadata)
        }

        @JvmStatic
        @JvmOverloads
        fun create(
            upi: Upi,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(upi, allowRedisplay, billingDetails, metadata)
        }

        @JvmStatic
        @JvmOverloads
        fun create(
            usBankAccount: USBankAccount,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(usBankAccount, allowRedisplay, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Netbanking] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            netbanking: Netbanking,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(netbanking, allowRedisplay, billingDetails, metadata)
        }

        /**
         * @return params for creating a [PaymentMethod.Type.P24] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun createP24(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.P24,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Bancontact] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun createBancontact(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Bancontact,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Giropay] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun createGiropay(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Giropay,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        /**
         * @return params for creating a [PaymentMethod.Type.GrabPay] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun createGrabPay(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.GrabPay,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        /**
         * @return params for creating a [PaymentMethod.Type.Eps] payment method
         */
        @JvmStatic
        @JvmOverloads
        fun createEps(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Eps,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createOxxo(
            billingDetails: PaymentMethod.BillingDetails,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Oxxo,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createAlipay(
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Alipay,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createPayPal(
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.PayPal,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createAfterpayClearpay(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.AfterpayClearpay,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
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
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Blik,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createWeChatPay(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.WeChatPay,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createKlarna(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Klarna,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createAffirm(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Affirm,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createUSBankAccount(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.USBankAccount,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        /**
         * Helper method to create [PaymentMethodCreateParams] with [CashAppPay] as the payment
         * method type.
         */
        @JvmStatic
        @JvmOverloads
        fun createCashAppPay(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                cashAppPay = CashAppPay(),
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,

            )
        }

        /**
         * Helper method to create [PaymentMethodCreateParams] with [PaymentMethod.Type.AmazonPay] as the payment
         * method type.
         */
        @JvmStatic
        @JvmOverloads
        fun createAmazonPay(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.AmazonPay,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        /**
         * Helper method to create [PaymentMethodCreateParams] with [PaymentMethod.Type.Multibanco] as the payment
         * method type.
         */
        @JvmStatic
        @JvmOverloads
        fun createMultibanco(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Multibanco,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        /**
         * Helper method to create [PaymentMethodCreateParams] with [PaymentMethod.Type.Alma] as the payment
         * method type
         */
        @JvmStatic
        @JvmOverloads
        fun createAlma(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Alma,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        /**
         * Helper method to create [PaymentMethodCreateParams] with [PaymentMethod.Type.Sunbit] as the payment
         * method type
         */
        @JvmStatic
        @JvmOverloads
        fun createSunbit(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Sunbit,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        /**
         * Helper method to create [PaymentMethodCreateParams] with [PaymentMethod.Type.Billie] as the payment
         * method type
         */
        @JvmStatic
        @JvmOverloads
        fun createBillie(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Billie,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        /**
         * Helper method to create [PaymentMethodCreateParams] with [PaymentMethod.Type.Satispay] as the payment
         * method type
         */
        @JvmStatic
        @JvmOverloads
        fun createSatispay(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Satispay,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        /**
         * Helper method to create [PaymentMethodCreateParams] with [Swish] as the payment
         * method type.
         */
        @JvmStatic
        @JvmOverloads
        fun createSwish(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(Swish(), allowRedisplay, billingDetails, metadata)
        }

        @JvmStatic
        @JvmOverloads
        fun createRevolutPay(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.RevolutPay,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        @JvmStatic
        @JvmOverloads
        fun createMobilePay(
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.MobilePay,
                billingDetails = billingDetails,
                metadata = metadata,
                allowRedisplay = allowRedisplay,
            )
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun createLink(
            paymentDetailsId: String,
            consumerSessionClientSecret: String,
            extraParams: Map<String, @RawValue Any>? = null
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                type = PaymentMethod.Type.Link,
                link = Link(
                    paymentDetailsId,
                    consumerSessionClientSecret,
                    extraParams
                )
            )
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
        fun createInstantDebits(
            paymentMethodId: String,
            requiresMandate: Boolean,
            productUsage: Set<String>,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                code = PaymentMethod.Type.Link.code,
                requiresMandate = requiresMandate,
                overrideParamMap = mapOf(
                    "link" to mapOf(
                        "payment_method_id" to paymentMethodId,
                    ),
                ),
                allowRedisplay = allowRedisplay,
                productUsage = productUsage,
            )
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
        fun createWithOverride(
            code: PaymentMethodCode,
            billingDetails: PaymentMethod.BillingDetails?,
            requiresMandate: Boolean,
            overrideParamMap: Map<String, @RawValue Any>?,
            productUsage: Set<String>,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            return PaymentMethodCreateParams(
                code = code,
                billingDetails = billingDetails,
                requiresMandate = requiresMandate,
                allowRedisplay = allowRedisplay,
                overrideParamMap = overrideParamMap,
                productUsage = productUsage
            )
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun createBacsFromParams(
            params: PaymentMethodCreateParams
        ): BacsDebit? {
            return BacsDebit.fromParams(params)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun getNameFromParams(
            params: PaymentMethodCreateParams
        ): String? {
            return params.billingDetails?.name ?: getBillingDetailsValueFromOverrideParams(
                params,
                PaymentMethod.BillingDetails.PARAM_NAME
            )
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun getEmailFromParams(
            params: PaymentMethodCreateParams
        ): String? {
            return params.billingDetails?.email ?: getBillingDetailsValueFromOverrideParams(
                params,
                PaymentMethod.BillingDetails.PARAM_EMAIL
            )
        }

        private fun getBillingDetailsValueFromOverrideParams(
            params: PaymentMethodCreateParams,
            key: String
        ): String? {
            val billingDetailsParams = params.overrideParamMap
                ?.get(PARAM_BILLING_DETAILS) as? Map<*, *>

            return billingDetailsParams?.get(key) as? String
        }
    }
}
