package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.payments.PaymentFlowResultProcessor.Companion.MAX_RETRIES
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

typealias PaymentMethodCode = String

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
data class PaymentMethod
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
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
     * The code of the PaymentMethod. This is useful when the PaymentMethodType is not
     * hard coded in the SDK.
     *
     * [livemode](https://stripe.com/docs/api/payment_methods/object#payment_method_object-type)
     */
    @JvmField internal val code: PaymentMethodCode?,

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

    /**
     * If this is an `au_becs_debit` PaymentMethod, this hash contains details about the bank account.
     *
     * [au_becs_debit](https://stripe.com/docs/api/payment_methods/object#payment_method_object-au_becs_debit)
     */
    @JvmField val auBecsDebit: AuBecsDebit? = null,

    /**
     * If this is a `bacs_debit` PaymentMethod, this hash contains details about the Bacs Direct Debit bank account.
     *
     * [bacs_debit](https://stripe.com/docs/api/payment_methods/object#payment_method_object-bacs_debit)
     */
    @JvmField val bacsDebit: BacsDebit? = null,

    /**
     * If this is a `sofort` PaymentMethod, this hash contains details about the SOFORT payment method.
     *
     * [sofort](https://stripe.com/docs/api/payment_methods/object#payment_method_object-sofort)
     */
    @JvmField val sofort: Sofort? = null,

    @JvmField val upi: Upi? = null,

    @JvmField val netbanking: Netbanking? = null,

    /**
     * If this is an `us_bank_account` PaymentMethod, this hash contains details about the bank account.
     *
     * [us_bank_account](https://stripe.com/docs/api/payment_methods/object#payment_method_object-us_bank_account)
     */
    @JvmField val usBankAccount: USBankAccount? = null,

    /**
     * Indicates whether this payment method can be shown again to its customer in a checkout flow. Stripe products
     * such as Checkout and Elements use this field to determine whether a payment method can be shown as a saved
     * payment method in a checkout flow. The field defaults to "unspecified".
     *
     * [allow_redisplay](https://docs.stripe.com/api/payment_methods/object#payment_method_object-allow_redisplay)
     */
    @JvmField val allowRedisplay: AllowRedisplay? = null,
) : StripeModel {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun hasExpectedDetails(): Boolean =
        when (type) {
            Type.Card -> card != null
            Type.CardPresent -> cardPresent != null
            Type.Fpx -> fpx != null
            Type.Ideal -> ideal != null
            Type.SepaDebit -> sepaDebit != null
            Type.AuBecsDebit -> auBecsDebit != null
            Type.BacsDebit -> bacsDebit != null
            Type.Sofort -> sofort != null
            Type.USBankAccount -> usBankAccount != null
            else -> true
        }

    @Parcelize
    enum class Type(
        @JvmField val code: String,
        @JvmField val isReusable: Boolean,
        @JvmField val isVoucher: Boolean,
        @JvmField val requiresMandate: Boolean,
        private val hasDelayedSettlement: Boolean,
        internal val afterRedirectAction: AfterRedirectAction = AfterRedirectAction.None,
    ) : Parcelable {
        Link(
            "link",
            isReusable = false,
            isVoucher = false,
            requiresMandate = true,
            hasDelayedSettlement = false,
        ),
        Card(
            "card",
            isReusable = true,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        CardPresent(
            "card_present",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        Fpx(
            "fpx",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        Ideal(
            "ideal",
            isReusable = false,
            isVoucher = false,
            requiresMandate = true,
            hasDelayedSettlement = false,
        ),
        SepaDebit(
            "sepa_debit",
            isReusable = false,
            isVoucher = false,
            requiresMandate = true,
            hasDelayedSettlement = true,
        ),
        AuBecsDebit(
            "au_becs_debit",
            isReusable = true,
            isVoucher = false,
            requiresMandate = true,
            hasDelayedSettlement = true,
        ),
        BacsDebit(
            "bacs_debit",
            isReusable = true,
            isVoucher = false,
            requiresMandate = true,
            hasDelayedSettlement = true,
        ),
        Sofort(
            "sofort",
            isReusable = false,
            isVoucher = false,
            requiresMandate = true,
            hasDelayedSettlement = true,
        ),
        Upi(
            "upi",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
            afterRedirectAction = AfterRedirectAction.Refresh(),
        ),
        P24(
            "p24",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
            // We are intentionally polling for P24 even though it uses the redirect trampoline.
            // About 20% of the time, the intent is still in `requires_action` status
            // after redirecting following a successful payment.
            // This allows time for the intent to transition to its terminal state.
            afterRedirectAction = AfterRedirectAction.Poll(),
        ),
        Bancontact(
            "bancontact",
            isReusable = false,
            isVoucher = false,
            requiresMandate = true,
            hasDelayedSettlement = false,
        ),
        Giropay(
            "giropay",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        Eps(
            "eps",
            isReusable = false,
            isVoucher = false,
            requiresMandate = true,
            hasDelayedSettlement = false,
        ),
        Oxxo(
            "oxxo",
            isReusable = false,
            isVoucher = true,
            requiresMandate = false,
            hasDelayedSettlement = true,
        ),
        Alipay(
            "alipay",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        GrabPay(
            "grabpay",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        PayPal(
            "paypal",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        AfterpayClearpay(
            "afterpay_clearpay",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        Netbanking(
            "netbanking",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        Blik(
            "blik",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        WeChatPay(
            "wechat_pay",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
            afterRedirectAction = AfterRedirectAction.Refresh(retryCount = MAX_RETRIES),
        ),
        Klarna(
            "klarna",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        Affirm(
            "affirm",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        RevolutPay(
            "revolut_pay",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
            afterRedirectAction = AfterRedirectAction.Poll(),
        ),
        Sunbit(
            "sunbit",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        Billie(
            "billie",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        Satispay(
            "satispay",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        AmazonPay(
            "amazon_pay",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
            afterRedirectAction = AfterRedirectAction.Poll(),
        ),
        Alma(
            "alma",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        MobilePay(
            "mobilepay",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        Multibanco(
            "multibanco",
            isReusable = false,
            isVoucher = true,
            requiresMandate = false,
            hasDelayedSettlement = true,
        ),
        Zip(
            "zip",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
        ),
        USBankAccount(
            code = "us_bank_account",
            isReusable = true,
            isVoucher = false,
            requiresMandate = true,
            hasDelayedSettlement = true,
        ),
        CashAppPay(
            code = "cashapp",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
            afterRedirectAction = AfterRedirectAction.Refresh(),
        ),
        Boleto(
            code = "boleto",
            isReusable = false,
            isVoucher = true,
            requiresMandate = false,
            hasDelayedSettlement = true,
        ),
        Konbini(
            code = "konbini",
            isReusable = false,
            isVoucher = true,
            requiresMandate = false,
            hasDelayedSettlement = true,
        ),
        Swish(
            code = "swish",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
            // We are intentionally polling for Swish even though it uses the redirect trampoline.
            // About 50% of the time, the intent is still in `requires_action` status
            // after redirecting following a successful payment.
            // This allows time for the intent to transition to its terminal state.
            afterRedirectAction = AfterRedirectAction.Poll(),
        ),
        Twint(
            code = "twint",
            isReusable = false,
            isVoucher = false,
            requiresMandate = false,
            hasDelayedSettlement = false,
            // We are intentionally polling for Twint even though it uses the redirect trampoline.
            // About 50% of the time, the intent is still in `requires_action` status
            // after redirecting following a successful payment.
            // This allows time for the intent to transition to its terminal state.
            afterRedirectAction = AfterRedirectAction.Poll(),
        );

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
        fun hasDelayedSettlement(): Boolean = hasDelayedSettlement

        override fun toString(): String {
            return code
        }

        companion object {
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            // For paymentsheet
            @JvmSynthetic
            fun fromCode(code: String?): Type? {
                return entries.firstOrNull { it.code == code }
            }
        }
    }

    internal sealed interface AfterRedirectAction : Parcelable {
        val shouldRefresh: Boolean
        val retryCount: Int

        @Parcelize
        data object None : AfterRedirectAction {
            @IgnoredOnParcel
            override val shouldRefresh: Boolean = false

            @IgnoredOnParcel
            override val retryCount: Int = MAX_RETRIES
        }

        @Parcelize
        data class Poll(override val retryCount: Int = MAX_RETRIES) : AfterRedirectAction {
            @IgnoredOnParcel
            override val shouldRefresh: Boolean = true
        }

        @Parcelize
        data class Refresh(override val retryCount: Int = 1) : AfterRedirectAction {
            @IgnoredOnParcel
            override val shouldRefresh: Boolean = true
        }
    }

    class Builder {
        private var id: String? = null
        private var created: Long? = null
        private var liveMode: Boolean = false
        private var type: Type? = null
        private var code: PaymentMethodCode? = null
        private var billingDetails: BillingDetails? = null
        private var allowRedisplay: AllowRedisplay? = null
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
        private var netbanking: Netbanking? = null
        private var usBankAccount: USBankAccount? = null
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

        fun setAllowRedisplay(allowRedisplay: AllowRedisplay?): Builder = apply {
            this.allowRedisplay = allowRedisplay
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

        fun setNetbanking(netbanking: Netbanking?): Builder = apply {
            this.netbanking = netbanking
        }

        fun setUSBankAccount(usBankAccount: USBankAccount?): Builder = apply {
            this.usBankAccount = usBankAccount
        }

        fun setUpi(upi: Upi?): Builder = apply {
            this.upi = upi
        }

        fun setCode(code: String?): Builder = apply {
            this.code = code
        }

        fun build(): PaymentMethod {
            return PaymentMethod(
                id = id,
                created = created,
                liveMode = liveMode,
                type = type,
                code = code,
                billingDetails = billingDetails,
                allowRedisplay = allowRedisplay,
                customerId = customerId,
                card = card,
                cardPresent = cardPresent,
                fpx = fpx,
                ideal = ideal,
                sepaDebit = sepaDebit,
                auBecsDebit = auBecsDebit,
                bacsDebit = bacsDebit,
                sofort = sofort,
                netbanking = netbanking,
                usBankAccount = usBankAccount
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun isFilledOut(): Boolean {
            return (address != null && address.isFilledOut()) || email != null || name != null || phone != null
        }

        class Builder {
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

            fun build(): BillingDetails {
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

    @Parcelize
    enum class AllowRedisplay(internal val value: String) : StripeModel {
        // Default value for payment methods where `allow_redisplay` was not set.
        UNSPECIFIED("unspecified"),

        /*
         * Indicates that the payment method can’t always be shown to a customer in a checkout flow. For example,
         * it can only be shown in the context of a specific subscription.
         */
        LIMITED("limited"),

        // Indicates that the payment method can always be shown to a customer in a checkout flow.
        ALWAYS("always"),
    }

    sealed class TypeData : StripeModel {
        abstract val type: Type
    }

    /**
     * If this is a `card` PaymentMethod, this hash contains details about the card.
     *
     * [card](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card)
     */
    @Parcelize
    data class Card
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
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
         * Uniquely identifies this particular card number. You can use this attribute to check whether two customers who’ve signed up with you are using the same card number, for example. For payment methods that tokenize card information (Apple Pay, Google Pay), the tokenized number might be provided instead of the underlying card number.
         *
         * [card.fingerprint](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-fingerprint)
         */
        @JvmField val fingerprint: String? = null,

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
        val networks: Networks? = null,

        @JvmField
        @field:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val displayBrand: String? = null
    ) : TypeData() {
        override val type: Type get() = Type.Card

        /**
         * Checks on Card address and CVC if provided
         *
         * [card.checks](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-checks)
         */
        @Parcelize
        data class Checks
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        constructor(
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
        data class ThreeDSecureUsage
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        constructor(
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
            @Deprecated("This field is deprecated and will be removed in a future release.")
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
    ) : TypeData() {
        override val type: Type get() = Type.CardPresent

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
    ) : TypeData() {
        override val type: Type get() = Type.Ideal
    }

    /**
     * Requires the FPX payment method enabled on your account via
     * https://dashboard.stripe.com/account/payments/settings.
     *
     * To obtain the FPX bank's display name, see [com.stripe.android.view.FpxBank].
     */
    @Parcelize
    data class Fpx internal constructor(
        @JvmField val bank: String?,
        @JvmField val accountHolderType: String?
    ) : TypeData() {
        override val type: Type get() = Type.Fpx
    }

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
    ) : TypeData() {
        override val type: Type get() = Type.SepaDebit
    }

    @Parcelize
    data class AuBecsDebit
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        @JvmField val bsbNumber: String?,
        @JvmField val fingerprint: String?,
        @JvmField val last4: String?
    ) : TypeData() {
        override val type: Type get() = Type.AuBecsDebit
    }

    @Parcelize
    data class BacsDebit internal constructor(
        @JvmField val fingerprint: String?,
        @JvmField val last4: String?,
        @JvmField val sortCode: String?
    ) : TypeData() {
        override val type: Type get() = Type.BacsDebit
    }

    @Parcelize
    data class Sofort internal constructor(
        @JvmField val country: String?
    ) : TypeData() {
        override val type: Type get() = Type.Sofort
    }

    @Parcelize
    data class Upi internal constructor(
        @JvmField val vpa: String?
    ) : TypeData() {
        override val type: Type get() = Type.Upi
    }

    @Parcelize
    data class Netbanking internal constructor(
        /**
         * The customer’s bank.
         *
         * [netbanking.bank](https://stripe.com/docs/js#stripe_create_payment_method-paymentMethodData-netbanking[bank])
         */
        @JvmField val bank: String?
    ) : TypeData() {
        override val type: Type get() = Type.Netbanking
    }

    @Parcelize
    data class USBankAccount
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        /**
         * Account holder type
         *
         * [us_bank_account.account_holder_type](https://stripe.com/docs/api/payment_methods/object#payment_method_object-us_bank_account-account_holder_type)
         */
        @JvmField val accountHolderType: USBankAccountHolderType,

        /**
         * Account type
         *
         * [us_bank_account.account_type](https://stripe.com/docs/api/payment_methods/object#payment_method_object-us_bank_account-account_number)
         */
        @JvmField val accountType: USBankAccountType,

        /**
         * The name of the bank
         *
         * [us_bank_account.bank_name](https://stripe.com/docs/api/payment_methods/object#payment_method_object-us_bank_account-bank_name)
         */
        @JvmField val bankName: String?,

        /**
         * Uniquely identifies this particular bank account. You can use this attribute to check
         * whether two bank accounts are the same
         *
         * [us_bank_account.fingerprint](https://stripe.com/docs/api/payment_methods/object#payment_method_object-us_bank_account-fingerprint)
         */
        @JvmField val fingerprint: String?,

        /**
         * Last four digits of the bank account number
         *
         * [us_bank_account.last4](https://stripe.com/docs/api/payment_methods/object#payment_method_object-us_bank_account-last4)
         */
        @JvmField val last4: String?,

        /**
         * The ID of the Financial Connections Account used to create the payment method
         *
         * [us_bank_account.financial_connections_account](https://stripe.com/docs/api/payment_methods/object#payment_method_object-us_bank_account-financial_connections_account)
         */
        @JvmField val financialConnectionsAccount: String?,

        /**
         * Contains information about US bank account networks that can be used
         *
         * [us_bank_account.networks](https://stripe.com/docs/api/payment_methods/object#payment_method_object-us_bank_account-networks)
         */
        @JvmField val networks: USBankNetworks?,

        /**
         * Routing number of the bank account
         *
         * [us_bank_account.routingNumber](https://stripe.com/docs/api/payment_methods/object#payment_method_object-us_bank_account-routing_number)
         */
        @JvmField val routingNumber: String?,
    ) : TypeData() {
        /**
         * The token of the Linked Account used to create the payment method
         *
         * [us_bank_account.linkedAccount](https://stripe.com/docs/api/payment_methods/object#payment_method_object-us_bank_account-linked_account)
         */
        @Deprecated(
            message = "Renamed to 'financialConnectionsAccount', " +
                "'linkedAccount' will be removed in a future major update",
            replaceWith = ReplaceWith(expression = "financialConnectionsAccount")
        )
        @IgnoredOnParcel
        @JvmField
        val linkedAccount: String? = financialConnectionsAccount

        override val type: Type get() = Type.USBankAccount

        @Parcelize
        enum class USBankAccountHolderType(val value: String) : StripeModel {
            UNKNOWN("unknown"),

            // Account belongs to an individual
            INDIVIDUAL("individual"),

            // Account belongs to a company
            COMPANY("company")
        }

        @Parcelize
        enum class USBankAccountType(val value: String) : StripeModel {
            UNKNOWN("unknown"),

            // Bank account type is checking
            CHECKING("checking"),

            // Bank account type is savings
            SAVINGS("savings")
        }

        @Parcelize
        data class USBankNetworks(
            val preferred: String?,
            val supported: List<String>
        ) : StripeModel
    }

    companion object {
        @JvmStatic
        fun fromJson(paymentMethod: JSONObject?): PaymentMethod? {
            return paymentMethod?.let {
                PaymentMethodJsonParser().parse(it)
            }
        }
    }
}
