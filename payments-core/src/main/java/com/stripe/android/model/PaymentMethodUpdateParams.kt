package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import java.util.Objects

/**
 * Model for PaymentMethod update parameters.
 *
 * See [Update a PaymentMethod](https://stripe.com/docs/api/payment_methods/update).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
class PaymentMethodUpdateParams private constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val paymentMethodId: String,
    private val card: Card? = null,
    private val billingDetails: PaymentMethod.BillingDetails? = null,
    private val metadata: Map<String, String>? = null,
    internal val attribution: Set<String> = emptySet(),
) : StripeParamsModel, Parcelable {

    private val type: PaymentMethod.Type
        get() = when {
            card != null -> PaymentMethod.Type.Card
            else -> error("Invalid internal state in PaymentMethodUpdateParams")
        }

    private val paymentMethod: StripeParamsModel
        get() = card ?: error("Invalid internal state in PaymentMethodUpdateParams")

    private val typeParams: Map<String, Any>
        get() {
            val params = paymentMethod.toParamMap().takeIf { it.isNotEmpty() }
            return params?.let {
                mapOf(type.code to it)
            }.orEmpty()
        }

    override fun toParamMap(): Map<String, Any> {
        val type = mapOf(PARAM_TYPE to type.code)

        val billing = billingDetails?.let {
            mapOf(PARAM_BILLING_DETAILS to it.toParamMap())
        }.orEmpty()

        val metadataParams = metadata?.let {
            mapOf(PARAM_METADATA to it)
        }.orEmpty()

        return type + typeParams + billing + metadataParams
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Card(
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        private const val PARAM_TYPE = "type"
        private const val PARAM_BILLING_DETAILS = "billing_details"
        private const val PARAM_METADATA = "metadata"

        /**
         * Update a [PaymentMethod.Type.Card] payment method with the provided data.
         *
         * @param paymentMethodId The ID of the [PaymentMethod]
         * @param expiryMonth The new expiry month
         * @param expiryYear The new expiry year
         * @param networks The new [Card.Networks] information
         * @param billingDetails Billing information to attach to the [PaymentMethod]
         * @param metadata Metadata to attach to the [PaymentMethod]
         *
         * @return Params for updating a [PaymentMethod.Type.Card] payment method.
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
    }
}
