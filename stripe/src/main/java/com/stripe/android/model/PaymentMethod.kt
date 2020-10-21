package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.model.wallets.Wallet
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * [PaymentMethod](https://stripe.com/docs/api/payment_methods) objects represent your customer's
 * payment instruments. They can be used with
 * [PaymentIntents](https://stripe.com/docs/payments/payment-intents) to collect payments or saved
 * to Customer objects to store instrument details for future payments.
 *
 * Related guides: [Payment Methods](https://stripe.com/docs/payments/payment-methods) and
 * [More Payment Scenarios](https://stripe.com/docs/payments/more-payment-scenarios).
 *
 * See [PaymentMethodCreateParams] for PaymentMethod creation
 */
@Parcelize
data class PaymentMethod internal constructor(
    /**
     * Unique identifier for the object.
     *
     * [id](https://stripe.com/docs/api/payment_methods/object#payment_method_object-id)
     */
    @JvmField val id: String?,

    /**
     * Time at which the object was created. Measured in seconds since the Unix epoch.
     *
     * [created](https://stripe.com/docs/api/payment_methods/object#payment_method_object-created)
     */
    @JvmField val created: Long?,

    /**
     * Has the value `true` if the object exists in live mode or the value `false` if the object exists in test mode.
     *
     * [livemode](https://stripe.com/docs/api/payment_methods/object#payment_method_object-livemode)
     */
    @JvmField val liveMode: Boolean,

    /**
     * The type of the PaymentMethod. An additional hash is included on the PaymentMethod with a
     * name matching this value. It contains additional information specific to the
     * PaymentMethod type.
     *
     * [type](https://stripe.com/docs/api/payment_methods/object#payment_method_object-type)
     */
    @JvmField val type: Type?,

    /**
     * Billing information associated with the PaymentMethod that may be used or required by particular types of payment methods.
     *
     * [billing_details](https://stripe.com/docs/api/payment_methods/object#payment_method_object-billing_details)
     */
    @JvmField val billingDetails: BillingDetails? = null,

    /**
     * The ID of the Customer to which this PaymentMethod is saved. This will not be set when the
     * PaymentMethod has not been saved to a Customer.
     *
     * [customer](https://stripe.com/docs/api/payment_methods/object#payment_method_object-customer)
     */
    @JvmField val customerId: String? = null,

    /**
     * Set of key-value pairs that you can attach to an object. This can be useful for storing
     * additional information about the object in a structured format.
     *
     * [metadata](https://stripe.com/docs/api/payment_methods/object#payment_method_object-metadata)
     *
     * @deprecated Metadata is no longer returned to clients using publishable keys. Retrieve them on your server using your secret key instead.
     */
    @Deprecated("Metadata is no longer returned to clients using publishable keys. Retrieve them on your server using your secret key instead.")
    @JvmField val metadata: Map<String, String>? = null,

    /**
     * If this is a `card` PaymentMethod, this hash contains details about the card.
     *
     * [card](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card)
     */
    @JvmField val card: Card? = null,

    /**
     * If this is a `card_present` PaymentMethod, this hash contains details about the Card Present payment method.
     *
     * [card_present](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card_present)
     */
    @JvmField val cardPresent: CardPresent? = null,

    /**
     * If this is a `fpx` PaymentMethod, this hash contains details about the FPX payment method.
     */
    @JvmField val fpx: Fpx? = null,

    /**
     * If this is an `ideal` PaymentMethod, this hash contains details about the iDEAL payment method.
     *
     * [ideal](https://stripe.com/docs/api/payment_methods/object#payment_method_object-ideal)
     */
    @JvmField val ideal: Ideal? = null,

    /**
     * If this is a `sepa_debit` PaymentMethod, this hash contains details about the SEPA debit bank account.
     *
     * [sepa_debit](https://stripe.com/docs/api/payment_methods/object#payment_method_object-sepa_debit)
     */
    @JvmField val sepaDebit: SepaDebit? = null,

    @JvmField val auBecsDebit: AuBecsDebit? = null,

    @JvmField val bacsDebit: BacsDebit? = null,

    @JvmField val sofort: Sofort? = null,

    @JvmField val upi: Upi? = null
) : StripeModel {

    @Parcelize
    enum class Type constructor(
        @JvmField val code: String,
        @JvmField val isReusable: Boolean
    ) : Parcelable {
        Card("card", isReusable = true),
        CardPresent("card_present", isReusable = false),
        Fpx("fpx", isReusable = false),
        Ideal("ideal", isReusable = false),
        SepaDebit("sepa_debit", isReusable = false),
        AuBecsDebit("au_becs_debit", isReusable = true),
        BacsDebit("bacs_debit", isReusable = true),
        Sofort("sofort", isReusable = false),
        Upi("upi", isReusable = false),
        P24("p24", isReusable = false),
        Bancontact("bancontact", isReusable = false),
        Giropay("giropay", isReusable = false),
        Eps("eps", isReusable = false),
        Oxxo("oxxo", isReusable = false),
        Alipay("alipay", isReusable = false),
        GrabPay("grabpay", isReusable = false),
        PayPal("paypal", isReusable = false),
        AfterpayClearpay("afterpay_clearpay", isReusable = false);

        override fun toString(): String {
            return code
        }

        companion object {
            @JvmSynthetic
            internal fun fromCode(code: String?): Type? {
                return values().firstOrNull { it.code == code }
            }
        }
    }

    class Builder : ObjectBuilder<PaymentMethod> {
        private var id: String? = null
        private var created: Long? = null
        private var liveMode: Boolean = false
        private var type: Type? = null
        private var billingDetails: BillingDetails? = null
        private var metadata: Map<String, String>? = null
        private var customerId: String? = null
        private var card: Card? = null
        private var cardPresent: CardPresent? = null
        private var ideal: Ideal? = null
        private var fpx: Fpx? = null
        private var sepaDebit: SepaDebit? = null
        private var auBecsDebit: AuBecsDebit? = null
        private var bacsDebit: BacsDebit? = null
        private var sofort: Sofort? = null
        private var upi: Upi? = null

        fun setId(id: String?): Builder = apply {
            this.id = id
        }

        fun setCreated(created: Long?): Builder = apply {
            this.created = created
        }

        fun setLiveMode(liveMode: Boolean): Builder = apply {
            this.liveMode = liveMode
        }

        fun setMetadata(metadata: Map<String, String>?): Builder = apply {
            this.metadata = metadata
        }

        fun setType(type: Type?): Builder = apply {
            this.type = type
        }

        fun setBillingDetails(billingDetails: BillingDetails?): Builder = apply {
            this.billingDetails = billingDetails
        }

        fun setCard(card: Card?): Builder = apply {
            this.card = card
        }

        fun setCardPresent(cardPresent: CardPresent?): Builder = apply {
            this.cardPresent = cardPresent
        }

        fun setCustomerId(customerId: String?): Builder = apply {
            this.customerId = customerId
        }

        fun setIdeal(ideal: Ideal?): Builder = apply {
            this.ideal = ideal
        }

        fun setFpx(fpx: Fpx?): Builder = apply {
            this.fpx = fpx
        }

        fun setSepaDebit(sepaDebit: SepaDebit?): Builder = apply {
            this.sepaDebit = sepaDebit
        }

        fun setAuBecsDebit(auBecsDebit: AuBecsDebit?): Builder = apply {
            this.auBecsDebit = auBecsDebit
        }

        fun setBacsDebit(bacsDebit: BacsDebit?): Builder = apply {
            this.bacsDebit = bacsDebit
        }

        fun setSofort(sofort: Sofort?): Builder = apply {
            this.sofort = sofort
        }

        fun setUpi(upi: Upi?): Builder = apply {
            this.upi = upi
        }

        override fun build(): PaymentMethod {
            return PaymentMethod(
                id = id,
                created = created,
                liveMode = liveMode,
                type = type,
                billingDetails = billingDetails,
                customerId = customerId,
                card = card,
                cardPresent = cardPresent,
                fpx = fpx,
                ideal = ideal,
                sepaDebit = sepaDebit,
                auBecsDebit = auBecsDebit,
                bacsDebit = bacsDebit,
                sofort = sofort
            )
        }
    }

    /**
     * Billing information associated with the PaymentMethod that may be used or required by particular types of payment methods.
     *
     * [billing_details](https://stripe.com/docs/api/payment_methods/object#payment_method_object-billing_details)
     */
    @Parcelize
    data class BillingDetails @JvmOverloads constructor(
        /**
         * Billing address.
         *
         * [billing_details.address](https://stripe.com/docs/api/payment_methods/object#payment_method_object-billing_details-address)
         */
        @JvmField val address: Address? = null,

        /**
         * Email address.
         *
         * [billing_details.email](https://stripe.com/docs/api/payment_methods/object#payment_method_object-billing_details-email)
         */
        @JvmField val email: String? = null,

        /**
         * Full name.
         *
         * [billing_details.name](https://stripe.com/docs/api/payment_methods/object#payment_method_object-billing_details-name)
         */
        @JvmField val name: String? = null,

        /**
         * Billing phone number (including extension).
         *
         * [billing_details.phone](https://stripe.com/docs/api/payment_methods/object#payment_method_object-billing_details-phone)
         */
        @JvmField val phone: String? = null
    ) : StripeModel, StripeParamsModel {

        fun toBuilder(): Builder {
            return Builder()
                .setAddress(address)
                .setEmail(email)
                .setName(name)
                .setPhone(phone)
        }

        override fun toParamMap(): Map<String, Any> {
            return emptyMap<String, Any>()
                .plus(
                    address?.let {
                        mapOf(PARAM_ADDRESS to it.toParamMap())
                    }.orEmpty()
                )
                .plus(
                    email?.let {
                        mapOf(PARAM_EMAIL to it)
                    }.orEmpty()
                )
                .plus(
                    name?.let {
                        mapOf(PARAM_NAME to it)
                    }.orEmpty()
                )
                .plus(
                    phone?.let {
                        mapOf(PARAM_PHONE to it)
                    }.orEmpty()
                )
        }

        class Builder : ObjectBuilder<BillingDetails> {
            private var address: Address? = null
            private var email: String? = null
            private var name: String? = null
            private var phone: String? = null

            fun setAddress(address: Address?): Builder = apply {
                this.address = address
            }

            fun setEmail(email: String?): Builder = apply {
                this.email = email
            }

            fun setName(name: String?): Builder = apply {
                this.name = name
            }

            fun setPhone(phone: String?): Builder = apply {
                this.phone = phone
            }

            override fun build(): BillingDetails {
                return BillingDetails(address, email, name, phone)
            }
        }

        internal companion object {
            internal const val PARAM_ADDRESS = "address"
            internal const val PARAM_EMAIL = "email"
            internal const val PARAM_NAME = "name"
            internal const val PARAM_PHONE = "phone"

            fun create(shippingInformation: ShippingInformation): BillingDetails {
                return BillingDetails(
                    address = shippingInformation.address,
                    name = shippingInformation.name,
                    phone = shippingInformation.phone
                )
            }
        }
    }

    /**
     * If this is a `card` PaymentMethod, this hash contains details about the card.
     *
     * [card](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card)
     */
    @Parcelize
    data class Card internal constructor(
        /**
         * Card brand. Can be `amex`, `diners`, `discover`, `jcb`, `mastercard`, `unionpay`,
         * `visa`, or `unknown`.
         *
         * [card.brand](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-brand)
         */
        @JvmField val brand: CardBrand = CardBrand.Unknown,

        /**
         * Checks on Card address and CVC if provided
         *
         * [card.checks](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-checks)
         */
        @JvmField val checks: Checks? = null,

        /**
         * Two-letter ISO code representing the country of the card. You could use this attribute to get a sense of the international breakdown of cards you’ve collected.
         *
         * [card.country](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-country)
         */
        @JvmField val country: String? = null,

        /**
         * Two-digit number representing the card’s expiration month.
         *
         * [card.exp_month](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-exp_month)
         */
        @JvmField val expiryMonth: Int? = null,

        /**
         * Four-digit number representing the card’s expiration year.
         *
         * [card.exp_year](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-exp_year)
         */
        @JvmField val expiryYear: Int? = null,

        /**
         * Card funding type. Can be `credit`, `debit, `prepaid`, or `unknown`.
         *
         * [card.funding](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-funding)
         */
        @JvmField val funding: String? = null,

        /**
         * The last four digits of the card.
         *
         * [card.last4](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-last4)
         */
        @JvmField val last4: String? = null,

        /**
         * Contains details on how this Card maybe be used for 3D Secure authentication.
         *
         * [card.three_d_secure_usage](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-three_d_secure_usage)
         */
        @JvmField val threeDSecureUsage: ThreeDSecureUsage? = null,

        /**
         * If this Card is part of a card wallet, this contains the details of the card wallet.
         *
         * [card.wallet](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-wallet)
         */
        @JvmField val wallet: Wallet? = null,

        @JvmField
        internal val networks: Networks? = null
    ) : StripeModel {

        class Builder : ObjectBuilder<Card> {
            private var brand: CardBrand = CardBrand.Unknown
            private var checks: Checks? = null
            private var country: String? = null
            private var expiryMonth: Int? = null
            private var expiryYear: Int? = null
            private var funding: String? = null
            private var last4: String? = null
            private var threeDSecureUsage: ThreeDSecureUsage? = null
            private var wallet: Wallet? = null

            fun setBrand(brand: CardBrand): Builder = apply {
                this.brand = brand
            }

            fun setChecks(checks: Checks?): Builder = apply {
                this.checks = checks
            }

            fun setCountry(country: String?): Builder = apply {
                this.country = country
            }

            fun setExpiryMonth(expiryMonth: Int?): Builder = apply {
                this.expiryMonth = expiryMonth
            }

            fun setExpiryYear(expiryYear: Int?): Builder = apply {
                this.expiryYear = expiryYear
            }

            fun setFunding(funding: String?): Builder = apply {
                this.funding = funding
            }

            fun setLast4(last4: String?): Builder = apply {
                this.last4 = last4
            }

            fun setThreeDSecureUsage(threeDSecureUsage: ThreeDSecureUsage?): Builder = apply {
                this.threeDSecureUsage = threeDSecureUsage
            }

            fun setWallet(wallet: Wallet?): Builder = apply {
                this.wallet = wallet
            }

            override fun build(): Card {
                return Card(
                    brand,
                    checks,
                    country,
                    expiryMonth,
                    expiryYear,
                    funding,
                    last4,
                    threeDSecureUsage,
                    wallet
                )
            }
        }

        /**
         * Checks on Card address and CVC if provided
         *
         * [card.checks](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-checks)
         */
        @Parcelize
        data class Checks internal constructor(
            /**
             * If a address line1 was provided, results of the check, one of `pass`, `fail`, `unavailable`, or `unchecked`.
             *
             * [payment_method.card.checks.address_line1_check](https://stripe.com/docs/api/errors#errors-payment_method-card-checks-address_line1_check)
             */
            @JvmField val addressLine1Check: String?,

            /**
             * If a address postal code was provided, results of the check, one of `pass`, `fail`, `unavailable`, or `unchecked`.
             *
             * [payment_method.card.checks.address_postal_code_check](https://stripe.com/docs/api/errors#errors-payment_method-card-checks-address_postal_code_check)
             */
            @JvmField val addressPostalCodeCheck: String?,

            /**
             * If a CVC was provided, results of the check, one of `pass`, `fail`, `unavailable`, or `unchecked`.
             *
             * [payment_method.card.checks.cvc_check](https://stripe.com/docs/api/errors#errors-payment_method-card-checks-cvc_check)
             */
            @JvmField val cvcCheck: String?
        ) : StripeModel

        /**
         * Contains details on how this Card maybe be used for 3D Secure authentication.
         *
         * [card.three_d_secure_usage](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-three_d_secure_usage)
         */
        @Parcelize
        data class ThreeDSecureUsage internal constructor(
            /**
             * Whether 3D Secure is supported on this card.
             *
             * [payment_method.card.three_d_secure_usage.supported](https://stripe.com/docs/api/errors#errors-payment_method-card-three_d_secure_usage-supported)
             */
            @JvmField val isSupported: Boolean
        ) : StripeModel

        @Parcelize
        data class Networks(
            val available: Set<String> = emptySet(),
            val selectionMandatory: Boolean = false,
            val preferred: String? = null
        ) : StripeModel
    }

    /**
     * If this is a `card_present` PaymentMethod, this hash contains details about the Card Present payment method.
     *
     * [card_present](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card_present)
     */
    @Parcelize
    data class CardPresent internal constructor(
        private val ignore: Boolean = true
    ) : StripeModel {
        internal companion object {
            @JvmSynthetic
            internal val EMPTY: CardPresent = CardPresent()
        }
    }

    /**
     * If this is an `ideal` PaymentMethod, this hash contains details about the iDEAL payment method.
     *
     * [ideal](https://stripe.com/docs/api/payment_methods/object#payment_method_object-ideal)
     */
    @Parcelize
    data class Ideal internal constructor(
        /**
         * The customer’s bank, if provided. Can be one of `abn_amro`, `asn_bank`, `bunq`,
         * `handelsbanken`, `ing`, `knab`, `moneyou`, `rabobank`, `regiobank`, `sns_bank`,
         * `triodos_bank`, or `van_lanschot`.
         *
         * [ideal.bank](https://stripe.com/docs/api/payment_methods/object#payment_method_object-ideal-bank)
         */
        @JvmField val bank: String?,

        /**
         * The Bank Identifier Code of the customer’s bank, if the bank was provided.
         *
         * [ideal.bic](https://stripe.com/docs/api/payment_methods/object#payment_method_object-ideal-bic)
         */
        @JvmField val bankIdentifierCode: String?
    ) : StripeModel

    /**
     * Requires the FPX payment method enabled on your account via
     * https://dashboard.stripe.com/account/payments/settings.
     *
     * To obtain the FPX bank's display name and icon, see [com.stripe.android.view.FpxBank].
     */
    @Parcelize
    data class Fpx internal constructor(
        @JvmField val bank: String?,
        @JvmField val accountHolderType: String?
    ) : StripeModel

    /**
     * If this is a `sepa_debit` PaymentMethod, this hash contains details about the SEPA debit bank account.
     *
     * [sepa_debit](https://stripe.com/docs/api/payment_methods/object#payment_method_object-sepa_debit)
     */
    @Parcelize
    data class SepaDebit internal constructor(
        /**
         * Bank code of bank associated with the bank account.
         *
         * [sepa_debit.bank_code](https://stripe.com/docs/api/payment_methods/object#payment_method_object-sepa_debit-bank_code)
         */
        @JvmField val bankCode: String?,

        /**
         * Branch code of bank associated with the bank account.
         *
         * [sepa_debit.branch_code](https://stripe.com/docs/api/payment_methods/object#payment_method_object-sepa_debit-branch_code)
         */
        @JvmField val branchCode: String?,

        /**
         * Two-letter ISO code representing the country the bank account is located in.
         *
         * [sepa_debit.country](https://stripe.com/docs/api/payment_methods/object#payment_method_object-sepa_debit-country)
         */
        @JvmField val country: String?,

        /**
         * Uniquely identifies this particular bank account. You can use this attribute to check whether two bank accounts are the same.
         *
         * [sepa_debit.fingerprint](https://stripe.com/docs/api/payment_methods/object#payment_method_object-sepa_debit-fingerprint)
         */
        @JvmField val fingerprint: String?,

        /**
         * Last four characters of the IBAN.
         *
         * [sepa_debit.last4](https://stripe.com/docs/api/payment_methods/object#payment_method_object-sepa_debit-last4)
         */
        @JvmField val last4: String?
    ) : StripeModel

    @Parcelize
    data class AuBecsDebit internal constructor(
        @JvmField val bsbNumber: String?,
        @JvmField val fingerprint: String?,
        @JvmField val last4: String?
    ) : StripeModel

    @Parcelize
    data class BacsDebit internal constructor(
        @JvmField val fingerprint: String?,
        @JvmField val last4: String?,
        @JvmField val sortCode: String?
    ) : StripeModel

    @Parcelize
    data class Sofort internal constructor(
        @JvmField val country: String?
    ) : StripeModel

    @Parcelize
    data class Upi internal constructor(
        @JvmField val vpa: String?
    ) : StripeModel

    companion object {
        @JvmStatic
        fun fromJson(paymentMethod: JSONObject?): PaymentMethod? {
            return paymentMethod?.let {
                PaymentMethodJsonParser().parse(it)
            }
        }
    }
}
