package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.StringDef
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.model.wallets.Wallet
import kotlinx.android.parcel.Parcelize
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
    @JvmField val billingDetails: BillingDetails? = null,
    @JvmField val customerId: String? = null,
    @JvmField val metadata: Map<String, String>? = null,
    @JvmField val card: Card? = null,
    @JvmField val cardPresent: CardPresent? = null,
    @JvmField val fpx: Fpx? = null,
    @JvmField val ideal: Ideal? = null,
    @JvmField val sepaDebit: SepaDebit? = null
) : StripeModel {

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
        @JvmField val address: Address? = null,
        @JvmField val email: String? = null,
        @JvmField val name: String? = null,
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

        internal companion object {
            internal const val PARAM_ADDRESS = "address"
            internal const val PARAM_EMAIL = "email"
            internal const val PARAM_NAME = "name"
            internal const val PARAM_PHONE = "phone"
        }
    }

    @Parcelize
    data class Card internal constructor(
        @field:Brand @JvmField val brand: String? = null,
        @JvmField val checks: Checks? = null,
        @JvmField val country: String? = null,
        @JvmField val expiryMonth: Int? = null,
        @JvmField val expiryYear: Int? = null,
        @JvmField val funding: String? = null,
        @JvmField val last4: String? = null,
        @JvmField val threeDSecureUsage: ThreeDSecureUsage? = null,
        @JvmField val wallet: Wallet? = null
    ) : StripeModel {

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
        ) : StripeModel

        @Parcelize
        data class ThreeDSecureUsage internal constructor(
            @JvmField val isSupported: Boolean
        ) : StripeModel
    }

    @Parcelize
    data class CardPresent internal constructor(
        private val ignore: Boolean = true
    ) : StripeModel {
        internal companion object {
            @JvmSynthetic
            internal val EMPTY: CardPresent = CardPresent()
        }
    }

    @Parcelize
    data class Ideal internal constructor(
        @JvmField val bank: String?,
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

    @Parcelize
    data class SepaDebit internal constructor(
        @JvmField val bankCode: String?,
        @JvmField val branchCode: String?,
        @JvmField val country: String?,
        @JvmField val fingerprint: String?,
        @JvmField val last4: String?
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
