package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.Objects

/**
 * Model for PaymentMethod update parameters.
 *
 * See [Update a PaymentMethod](https://stripe.com/docs/api/payment_methods/update).
 */
@Parcelize
class PaymentMethodUpdateParams private constructor(
    internal val paymentMethodId: String,
    private val card: Card? = null,
    private val usBankAccount: USBankAccount? = null,
    private val link: Link? = null,
    private val billingDetails: PaymentMethod.BillingDetails? = null,
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
//    private val overrideParamMap: Map<String, @RawValue Any>? = null
) : StripeParamsModel, Parcelable {

    internal val attribution: Set<String>
        get() = productUsage

    private val paymentMethodType: PaymentMethod.Type
        get() = when {
            card != null -> PaymentMethod.Type.Card
            usBankAccount != null -> PaymentMethod.Type.USBankAccount
            link != null -> PaymentMethod.Type.Link
            else -> error("Invalid state in PaymentMethodUpdateParams")
        }

    override fun toParamMap(): Map<String, Any> {
        val type = mapOf(PARAM_TYPE to paymentMethodType.code)

        val billing = billingDetails?.let {
            mapOf(PARAM_BILLING_DETAILS to it.toParamMap())
        }.orEmpty()

        val metadataParams = metadata?.let {
            mapOf(PARAM_METADATA to it)
        }.orEmpty()

        return type + typeParams + billing + metadataParams
    }

    private val typeParams: Map<String, Any>
        get() {
            val type = card ?: usBankAccount ?: link
            val params = type?.toParamMap()?.takeIf { it.isNotEmpty() }
            return params?.let {
                mapOf(paymentMethodType.code to it)
            }.orEmpty()
        }

    @Parcelize
    data class Card
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        internal val expiryMonth: Int? = null,
        internal val expiryYear: Int? = null,
        internal val networks: Networks? = null,
    ) : StripeParamsModel, Parcelable {

        // TODO(tillh-stripe) Add docs and make public
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
                PARAM_EXP_MONTH to expiryMonth,
                PARAM_EXP_YEAR to expiryYear,
                PARAM_NETWORKS to networks?.toParamMap(),
            ).mapNotNull {
                it.second?.let { value ->
                    it.first to value
                }
            }.toMap()
        }

        private companion object {
            const val PARAM_EXP_MONTH: String = "exp_month"
            const val PARAM_EXP_YEAR: String = "exp_year"
            const val PARAM_NETWORKS: String = "networks"
        }
    }

    @Parcelize
    class USBankAccount internal constructor(
        internal val accountHolderType: PaymentMethod.USBankAccount.USBankAccountHolderType? = null
    ) : StripeParamsModel, Parcelable {

        override fun toParamMap(): Map<String, Any> {
            return if (accountHolderType != null) {
                mapOf(
                    PARAM_ACCOUNT_HOLDER_TYPE to accountHolderType.value
                )
            } else {
                emptyMap()
            }
        }

        private companion object {
            private const val PARAM_ACCOUNT_HOLDER_TYPE = "account_holder_type"
        }
    }

    // TODO
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
        private const val PARAM_METADATA = "metadata"

        /**
         * TODO
         */
        @JvmStatic
        @JvmOverloads
        fun createCard(
            paymentMethodId: String,
            expiryMonth: Int? = null,
            expiryYear: Int? = null,
            networks: Card.Networks? = null,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
        ): PaymentMethodUpdateParams {
            return PaymentMethodUpdateParams(
                paymentMethodId = paymentMethodId,
                card = Card(
                    expiryMonth = expiryMonth,
                    expiryYear = expiryYear,
                    networks = networks,
                ),
                billingDetails = billingDetails,
                metadata = metadata,
            )
        }

        /**
         * TODO
         */
        @JvmStatic
        @JvmOverloads
        fun createUSBankAccount(
            paymentMethodId: String,
            accountHolderType: PaymentMethod.USBankAccount.USBankAccountHolderType? = null,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
        ): PaymentMethodUpdateParams {
            return PaymentMethodUpdateParams(
                paymentMethodId = paymentMethodId,
                usBankAccount = USBankAccount(accountHolderType),
                billingDetails = billingDetails,
                metadata = metadata,
            )
        }

        /**
         * TODO
         */
        @JvmStatic
        @JvmOverloads
        fun createLink(
            paymentMethodId: String,
            billingDetails: PaymentMethod.BillingDetails? = null,
            metadata: Map<String, String>? = null,
        ): PaymentMethodUpdateParams {
            return PaymentMethodUpdateParams(
                paymentMethodId = paymentMethodId,
                link = Link("", "", null),
                billingDetails = billingDetails,
                metadata = metadata,
            )
        }
    }
}
