package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.StringDef
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.StripeJsonUtils.optBoolean
import com.stripe.android.model.StripeJsonUtils.optHash
import com.stripe.android.model.StripeJsonUtils.optInteger
import com.stripe.android.model.StripeJsonUtils.optLong
import com.stripe.android.model.StripeJsonUtils.optString
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.model.wallets.WalletFactory
import java.util.HashMap
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for a [Payment Methods API](https://stripe.com/docs/payments/payment-methods)
 * object.
 *
 * See [Payment Methods API reference](https://stripe.com/docs/api/payment_methods).
 *
 * See [PaymentMethodCreateParams] for PaymentMethod creation
 */
@Suppress("DataClassPrivateConstructor")
@Parcelize
data class PaymentMethod internal constructor(
    @JvmField val id: String?,
    @JvmField val created: Long?,
    @JvmField val liveMode: Boolean,
    @JvmField val type: String?,
    @JvmField val billingDetails: BillingDetails?,
    @JvmField val customerId: String?,
    @JvmField val metadata: Map<String, String>?,
    @JvmField val card: Card?,
    @JvmField val cardPresent: CardPresent?,
    @JvmField val fpx: Fpx?,
    @JvmField val ideal: Ideal?,
    @JvmField val sepaDebit: SepaDebit?
) : StripeModel(), Parcelable {

    @Parcelize
    enum class Type constructor(
        @JvmField val code: String,
        @JvmField val isReusable: Boolean = true
    ) : Parcelable {
        Card("card"),
        CardPresent("card_present"),
        Fpx("fpx", false),
        Ideal("ideal"),
        SepaDebit("sepa_debit");

        override fun toString(): String {
            return code
        }

        companion object {
            @JvmSynthetic
            internal fun lookup(code: String?): Type? {
                return values().firstOrNull { it.code == code }
            }
        }
    }

    class Builder : ObjectBuilder<PaymentMethod> {
        private var id: String? = null
        private var created: Long? = null
        private var liveMode: Boolean = false
        private var type: String? = null
        private var billingDetails: BillingDetails? = null
        private var metadata: Map<String, String>? = null
        private var customerId: String? = null
        private var card: Card? = null
        private var cardPresent: CardPresent? = null
        private var ideal: Ideal? = null
        private var fpx: Fpx? = null
        private var sepaDebit: SepaDebit? = null

        fun setId(id: String?): Builder {
            this.id = id
            return this
        }

        fun setCreated(created: Long?): Builder {
            this.created = created
            return this
        }

        fun setLiveMode(liveMode: Boolean): Builder {
            this.liveMode = liveMode
            return this
        }

        fun setMetadata(metadata: Map<String, String>?): Builder {
            this.metadata = metadata
            return this
        }

        fun setType(type: String?): Builder {
            this.type = type
            return this
        }

        fun setBillingDetails(billingDetails: BillingDetails?): Builder {
            this.billingDetails = billingDetails
            return this
        }

        fun setCard(card: Card?): Builder {
            this.card = card
            return this
        }

        fun setCardPresent(cardPresent: CardPresent?): Builder {
            this.cardPresent = cardPresent
            return this
        }

        fun setCustomerId(customerId: String?): Builder {
            this.customerId = customerId
            return this
        }

        fun setIdeal(ideal: Ideal?): Builder {
            this.ideal = ideal
            return this
        }

        fun setFpx(fpx: Fpx?): Builder {
            this.fpx = fpx
            return this
        }

        fun setSepaDebit(sepaDebit: SepaDebit?): Builder {
            this.sepaDebit = sepaDebit
            return this
        }

        override fun build(): PaymentMethod {
            return PaymentMethod(
                id,
                created,
                liveMode,
                type,
                billingDetails,
                customerId,
                metadata,
                card,
                cardPresent,
                fpx,
                ideal,
                sepaDebit
            )
        }
    }

    @Parcelize
    data class BillingDetails internal constructor(
        @JvmField val address: Address?,
        @JvmField val email: String?,
        @JvmField val name: String?,
        @JvmField val phone: String?
    ) : StripeModel(), StripeParamsModel, Parcelable {

        fun toBuilder(): Builder {
            return Builder()
                .setAddress(address)
                .setEmail(email)
                .setName(name)
                .setPhone(phone)
        }

        override fun toParamMap(): Map<String, Any> {
            val billingDetails = HashMap<String, Any>()
            if (address != null) {
                billingDetails[FIELD_ADDRESS] = address.toParamMap()
            }
            if (email != null) {
                billingDetails[FIELD_EMAIL] = email
            }
            if (name != null) {
                billingDetails[FIELD_NAME] = name
            }
            if (phone != null) {
                billingDetails[FIELD_PHONE] = phone
            }
            return billingDetails
        }

        class Builder : ObjectBuilder<BillingDetails> {
            private var address: Address? = null
            private var email: String? = null
            private var name: String? = null
            private var phone: String? = null

            fun setAddress(address: Address?): Builder {
                this.address = address
                return this
            }

            fun setEmail(email: String?): Builder {
                this.email = email
                return this
            }

            fun setName(name: String?): Builder {
                this.name = name
                return this
            }

            fun setPhone(phone: String?): Builder {
                this.phone = phone
                return this
            }

            override fun build(): BillingDetails {
                return BillingDetails(address, email, name, phone)
            }
        }

        companion object {
            internal const val FIELD_ADDRESS = "address"
            internal const val FIELD_EMAIL = "email"
            internal const val FIELD_NAME = "name"
            internal const val FIELD_PHONE = "phone"

            fun fromJson(billingDetails: JSONObject?): BillingDetails? {
                return if (billingDetails == null) {
                    null
                } else {
                    Builder()
                        .setAddress(Address.fromJson(billingDetails.optJSONObject(FIELD_ADDRESS)))
                        .setEmail(optString(billingDetails, FIELD_EMAIL))
                        .setName(optString(billingDetails, FIELD_NAME))
                        .setPhone(optString(billingDetails, FIELD_PHONE))
                        .build()
                }
            }
        }
    }

    @Parcelize
    data class Card internal constructor(
        @field:Brand @JvmField val brand: String?,
        @JvmField val checks: Checks?,
        @JvmField val country: String?,
        @JvmField val expiryMonth: Int?,
        @JvmField val expiryYear: Int?,
        @JvmField val funding: String?,
        @JvmField val last4: String?,
        @JvmField val threeDSecureUsage: ThreeDSecureUsage?,
        @JvmField val wallet: Wallet?
    ) : Parcelable {

        @Retention(AnnotationRetention.SOURCE)
        @StringDef(Brand.AMERICAN_EXPRESS, Brand.DISCOVER, Brand.JCB, Brand.DINERS_CLUB,
            Brand.VISA, Brand.MASTERCARD, Brand.UNIONPAY, Brand.UNKNOWN)
        annotation class Brand {
            companion object {
                const val AMERICAN_EXPRESS: String = "amex"
                const val DISCOVER: String = "discover"
                const val JCB: String = "jcb"
                const val DINERS_CLUB: String = "diners"
                const val VISA: String = "visa"
                const val MASTERCARD: String = "mastercard"
                const val UNIONPAY: String = "unionpay"
                const val UNKNOWN: String = "unknown"
            }
        }

        class Builder : ObjectBuilder<Card> {
            private var brand: String? = null
            private var checks: Checks? = null
            private var country: String? = null
            private var expiryMonth: Int? = null
            private var expiryYear: Int? = null
            private var funding: String? = null
            private var last4: String? = null
            private var threeDSecureUsage: ThreeDSecureUsage? = null
            private var wallet: Wallet? = null

            fun setBrand(@Brand brand: String?): Builder {
                this.brand = brand
                return this
            }

            fun setChecks(checks: Checks?): Builder {
                this.checks = checks
                return this
            }

            fun setCountry(country: String?): Builder {
                this.country = country
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

            fun setFunding(funding: String?): Builder {
                this.funding = funding
                return this
            }

            fun setLast4(last4: String?): Builder {
                this.last4 = last4
                return this
            }

            fun setThreeDSecureUsage(threeDSecureUsage: ThreeDSecureUsage?): Builder {
                this.threeDSecureUsage = threeDSecureUsage
                return this
            }

            fun setWallet(wallet: Wallet?): Builder {
                this.wallet = wallet
                return this
            }

            override fun build(): Card {
                return Card(brand, checks, country, expiryMonth, expiryYear, funding,
                    last4, threeDSecureUsage, wallet)
            }
        }

        @Parcelize
        data class Checks internal constructor(
            @JvmField val addressLine1Check: String?,
            @JvmField val addressPostalCodeCheck: String?,
            @JvmField val cvcCheck: String?
        ) : StripeModel(), Parcelable {

            companion object {
                private const val FIELD_ADDRESS_LINE1_CHECK = "address_line1_check"
                private const val FIELD_ADDRESS_POSTAL_CODE_CHECK = "address_postal_code_check"
                private const val FIELD_CVC_CHECK = "cvc_check"

                @JvmSynthetic
                internal fun fromJson(checksJson: JSONObject?): Checks? {
                    return if (checksJson == null) {
                        null
                    } else {
                        Checks(
                            addressLine1Check = optString(checksJson, FIELD_ADDRESS_LINE1_CHECK),
                            addressPostalCodeCheck = optString(
                                checksJson, FIELD_ADDRESS_POSTAL_CODE_CHECK
                            ),
                            cvcCheck = optString(checksJson, FIELD_CVC_CHECK)
                        )
                    }
                }

                @JvmSynthetic
                internal fun create(
                    addressLine1Check: String?,
                    addressPostalCodeCheck: String?,
                    cvcCheck: String?
                ): Checks {
                    return Checks(addressLine1Check, addressPostalCodeCheck, cvcCheck)
                }
            }
        }

        @Parcelize
        data class ThreeDSecureUsage internal constructor(
            @JvmField val isSupported: Boolean
        ) : StripeModel(), Parcelable {

            companion object {
                private const val FIELD_IS_SUPPORTED = "supported"

                @JvmSynthetic
                internal fun fromJson(threeDSecureUsage: JSONObject?): ThreeDSecureUsage? {
                    return if (threeDSecureUsage == null) {
                        null
                    } else {
                        ThreeDSecureUsage(
                            isSupported = optBoolean(threeDSecureUsage, FIELD_IS_SUPPORTED)
                        )
                    }
                }

                @JvmSynthetic
                internal fun create(isSupported: Boolean): ThreeDSecureUsage {
                    return ThreeDSecureUsage(isSupported)
                }
            }
        }

        companion object {
            private const val FIELD_BRAND = "brand"
            private const val FIELD_CHECKS = "checks"
            private const val FIELD_COUNTRY = "country"
            private const val FIELD_EXP_MONTH = "exp_month"
            private const val FIELD_EXP_YEAR = "exp_year"
            private const val FIELD_FUNDING = "funding"
            private const val FIELD_LAST4 = "last4"
            private const val FIELD_THREE_D_SECURE_USAGE = "three_d_secure_usage"
            private const val FIELD_WALLET = "wallet"

            @JvmSynthetic
            internal fun fromJson(cardJson: JSONObject?): Card? {
                return if (cardJson == null) {
                    null
                } else {
                    Builder()
                        .setBrand(optString(cardJson, FIELD_BRAND))
                        .setChecks(Checks.fromJson(cardJson.optJSONObject(FIELD_CHECKS)))
                        .setCountry(optString(cardJson, FIELD_COUNTRY))
                        .setExpiryMonth(optInteger(cardJson, FIELD_EXP_MONTH))
                        .setExpiryYear(optInteger(cardJson, FIELD_EXP_YEAR))
                        .setFunding(optString(cardJson, FIELD_FUNDING))
                        .setLast4(optString(cardJson, FIELD_LAST4))
                        .setThreeDSecureUsage(ThreeDSecureUsage
                            .fromJson(cardJson.optJSONObject(FIELD_THREE_D_SECURE_USAGE)))
                        .setWallet(WalletFactory().create(cardJson.optJSONObject(FIELD_WALLET)))
                        .build()
                }
            }
        }
    }

    @Parcelize
    data class CardPresent internal constructor(
        private val ignore: Boolean = true
    ) : Parcelable {
        companion object {
            @JvmSynthetic
            internal val EMPTY: CardPresent = CardPresent()
        }
    }

    @Parcelize
    data class Ideal internal constructor(
        @JvmField val bank: String?,
        @JvmField val bankIdentifierCode: String?
    ) : Parcelable {

        companion object {
            private const val FIELD_BANK = "bank"
            private const val FIELD_BIC = "bic"

            @JvmSynthetic
            internal fun fromJson(ideal: JSONObject?): Ideal? {
                return if (ideal == null) {
                    null
                } else {
                    create(
                        bank = optString(ideal, FIELD_BANK),
                        bankIdentifierCode = optString(ideal, FIELD_BIC)
                    )
                }
            }

            @JvmSynthetic
            internal fun create(
                bank: String?,
                bankIdentifierCode: String?
            ): Ideal {
                return Ideal(bank, bankIdentifierCode)
            }
        }
    }

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
    ) : Parcelable {
        internal companion object {
            private const val FIELD_ACCOUNT_HOLDER_TYPE = "account_holder_type"
            private const val FIELD_BANK = "bank"

            @JvmSynthetic
            internal fun fromJson(fpx: JSONObject?): Fpx? {
                return if (fpx == null) {
                    null
                } else {
                    create(
                        bank = optString(fpx, FIELD_BANK),
                        accountHolderType = optString(fpx, FIELD_ACCOUNT_HOLDER_TYPE)
                    )
                }
            }

            @JvmSynthetic
            internal fun create(
                bank: String?,
                accountHolderType: String?
            ): Fpx {
                return Fpx(bank, accountHolderType)
            }
        }
    }

    @Parcelize
    data class SepaDebit internal constructor(
        @JvmField val bankCode: String?,
        @JvmField val branchCode: String?,
        @JvmField val country: String?,
        @JvmField val fingerprint: String?,
        @JvmField val last4: String?
    ) : Parcelable {

        internal companion object {
            private const val FIELD_BANK_CODE = "bank_code"
            private const val FIELD_BRANCH_CODE = "branch_code"
            private const val FIELD_COUNTRY = "country"
            private const val FIELD_FINGERPRINT = "fingerprint"
            private const val FIELD_LAST4 = "last4"

            @JvmSynthetic
            internal fun fromJson(sepaDebit: JSONObject?): SepaDebit? {
                return if (sepaDebit == null) {
                    null
                } else {
                    SepaDebit(
                        optString(sepaDebit, FIELD_BANK_CODE),
                        optString(sepaDebit, FIELD_BRANCH_CODE),
                        optString(sepaDebit, FIELD_COUNTRY),
                        optString(sepaDebit, FIELD_FINGERPRINT),
                        optString(sepaDebit, FIELD_LAST4)
                    )
                }
            }

            @JvmSynthetic
            internal fun create(
                bankCode: String?,
                branchCode: String?,
                country: String?,
                fingerprint: String?,
                last4: String?
            ): SepaDebit {
                return SepaDebit(bankCode, branchCode, country, fingerprint, last4)
            }
        }
    }

    companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_BILLING_DETAILS = "billing_details"
        private const val FIELD_CREATED = "created"
        private const val FIELD_CUSTOMER = "customer"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_METADATA = "metadata"

        // types
        private const val FIELD_TYPE = "type"
        private const val FIELD_CARD = "card"
        private const val FIELD_CARD_PRESENT = "card_present"
        private const val FIELD_FPX = "fpx"
        private const val FIELD_IDEAL = "ideal"
        private const val FIELD_SEPA_DEBIT = "sepa_debit"

        @JvmStatic
        fun fromString(jsonString: String?): PaymentMethod? {
            if (jsonString == null) {
                return null
            }

            return try {
                fromJson(JSONObject(jsonString))
            } catch (ignored: JSONException) {
                null
            }
        }

        @JvmStatic
        fun fromJson(paymentMethod: JSONObject?): PaymentMethod? {
            if (paymentMethod == null) {
                return null
            }

            val type = optString(paymentMethod, FIELD_TYPE)
            val builder = Builder()
                .setId(optString(paymentMethod, FIELD_ID))
                .setType(type)
                .setCreated(optLong(paymentMethod, FIELD_CREATED))
                .setBillingDetails(BillingDetails.fromJson(
                    paymentMethod.optJSONObject(FIELD_BILLING_DETAILS)))
                .setCustomerId(optString(paymentMethod, FIELD_CUSTOMER))
                .setLiveMode(java.lang.Boolean.TRUE == paymentMethod.optBoolean(FIELD_LIVEMODE))
                .setMetadata(optHash(paymentMethod, FIELD_METADATA))

            when {
                FIELD_CARD == type ->
                    builder.setCard(Card.fromJson(paymentMethod.optJSONObject(FIELD_CARD)))
                FIELD_CARD_PRESENT == type ->
                    builder.setCardPresent(CardPresent.EMPTY)
                FIELD_IDEAL == type ->
                    builder.setIdeal(Ideal.fromJson(paymentMethod.optJSONObject(FIELD_IDEAL)))
                FIELD_FPX == type ->
                    builder.setFpx(Fpx.fromJson(paymentMethod.optJSONObject(FIELD_FPX)))
                FIELD_SEPA_DEBIT == type ->
                    builder.setSepaDebit(
                        SepaDebit.fromJson(paymentMethod.optJSONObject(FIELD_SEPA_DEBIT))
                    )
            }

            return builder.build()
        }
    }
}
